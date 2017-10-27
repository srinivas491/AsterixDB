/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.asterix.runtime.evaluators.functions.temporal;

import java.io.DataOutput;

import org.apache.asterix.dataflow.data.nontagged.serde.ADateSerializerDeserializer;
import org.apache.asterix.dataflow.data.nontagged.serde.ADateTimeSerializerDeserializer;
import org.apache.asterix.formats.nontagged.AqlSerializerDeserializerProvider;
import org.apache.asterix.om.base.AInt64;
import org.apache.asterix.om.base.AMutableInt64;
import org.apache.asterix.om.base.temporal.GregorianCalendarSystem;
import org.apache.asterix.om.functions.AsterixBuiltinFunctions;
import org.apache.asterix.om.functions.IFunctionDescriptor;
import org.apache.asterix.om.functions.IFunctionDescriptorFactory;
import org.apache.asterix.om.types.ATypeTag;
import org.apache.asterix.om.types.BuiltinType;
import org.apache.asterix.om.types.EnumDeserializer;
import org.apache.asterix.runtime.evaluators.base.AbstractScalarFunctionDynamicDescriptor;
import org.apache.hyracks.algebricks.common.exceptions.AlgebricksException;
import org.apache.hyracks.algebricks.core.algebra.functions.FunctionIdentifier;
import org.apache.hyracks.algebricks.runtime.base.IScalarEvaluator;
import org.apache.hyracks.algebricks.runtime.base.IScalarEvaluatorFactory;
import org.apache.hyracks.api.context.IHyracksTaskContext;
import org.apache.hyracks.api.dataflow.value.ISerializerDeserializer;
import org.apache.hyracks.api.exceptions.HyracksDataException;
import org.apache.hyracks.data.std.api.IPointable;
import org.apache.hyracks.data.std.primitive.VoidPointable;
import org.apache.hyracks.data.std.util.ArrayBackedValueStorage;
import org.apache.hyracks.dataflow.common.data.accessors.IFrameTupleReference;

public class DayOfWeekDescriptor extends AbstractScalarFunctionDynamicDescriptor {
    private static final long serialVersionUID = 1L;
    public final static FunctionIdentifier FID = AsterixBuiltinFunctions.DAY_OF_WEEK;
    // Fixed week day anchor: Thursday, 1 January 1970
    private final static int ANCHOR_WEEKDAY = 4;

    public final static IFunctionDescriptorFactory FACTORY = new IFunctionDescriptorFactory() {

        @Override
        public IFunctionDescriptor createFunctionDescriptor() {
            return new DayOfWeekDescriptor();
        }

    };

    @Override
    public IScalarEvaluatorFactory createEvaluatorFactory(final IScalarEvaluatorFactory[] args)
            throws AlgebricksException {
        return new IScalarEvaluatorFactory() {

            private static final long serialVersionUID = 1L;

            @Override
            public IScalarEvaluator createScalarEvaluator(final IHyracksTaskContext ctx) throws AlgebricksException {
                return new IScalarEvaluator() {

                    private ArrayBackedValueStorage resultStorage = new ArrayBackedValueStorage();
                    private DataOutput out = resultStorage.getDataOutput();
                    private IPointable argPtr = new VoidPointable();
                    private IScalarEvaluator eval = args[0].createScalarEvaluator(ctx);

                    // possible returning types
                    @SuppressWarnings("unchecked")
                    private ISerializerDeserializer<AInt64> int64Serde = AqlSerializerDeserializerProvider.INSTANCE
                            .getSerializerDeserializer(BuiltinType.AINT64);
                    private AMutableInt64 aInt64 = new AMutableInt64(0);

                    @Override
                    public void evaluate(IFrameTupleReference tuple, IPointable result) throws AlgebricksException {
                        resultStorage.reset();
                        eval.evaluate(tuple, argPtr);

                        byte[] bytes = argPtr.getByteArray();
                        int offset = argPtr.getStartOffset();

                        try {
                            int daysSinceAnchor;
                            int reminder = 0;
                            if (bytes[offset] == ATypeTag.SERIALIZED_DATETIME_TYPE_TAG) {
                                daysSinceAnchor = (int) (ADateTimeSerializerDeserializer.getChronon(bytes, offset + 1)
                                        / GregorianCalendarSystem.CHRONON_OF_DAY);
                                reminder = (int) (ADateTimeSerializerDeserializer.getChronon(bytes, offset + 1)
                                        % GregorianCalendarSystem.CHRONON_OF_DAY);
                            } else if (bytes[offset] == ATypeTag.SERIALIZED_DATE_TYPE_TAG) {
                                daysSinceAnchor = ADateSerializerDeserializer.getChronon(bytes, offset + 1);
                            } else {
                                throw new AlgebricksException(
                                        FID.getName() + ": expects input type DATETIME/DATE/NULL but got "
                                                + EnumDeserializer.ATYPETAGDESERIALIZER.deserialize(bytes[offset]));
                            }

                            // adjust the day before 1970-01-01
                            if (daysSinceAnchor < 0 && reminder != 0) {
                                daysSinceAnchor -= 1;
                            }

                            // compute the weekday (0-based, and 0 = Sunday). Adjustment is needed as
                            // the anchor day is Thursday.
                            int weekday = (daysSinceAnchor + ANCHOR_WEEKDAY) % 7;

                            // handle the negative weekday
                            if (weekday < 0) {
                                weekday += 7;
                            }

                            // convert from 0-based to 1-based (so 7 = Sunday)
                            if (weekday == 0) {
                                weekday = 7;
                            }

                            aInt64.setValue(weekday);
                            int64Serde.serialize(aInt64, out);
                        } catch (HyracksDataException hex) {
                            throw new AlgebricksException(hex);
                        }
                        result.set(resultStorage);
                    }
                };
            }
        };
    }

    @Override
    public FunctionIdentifier getIdentifier() {
        return FID;
    }

}