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

import org.apache.asterix.formats.nontagged.AqlSerializerDeserializerProvider;
import org.apache.asterix.om.base.ADateTime;
import org.apache.asterix.om.base.AMutableDateTime;
import org.apache.asterix.om.base.temporal.AsterixTemporalTypeParseException;
import org.apache.asterix.om.base.temporal.DateTimeFormatUtils;
import org.apache.asterix.om.base.temporal.DateTimeFormatUtils.DateTimeParseMode;
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
import org.apache.hyracks.data.std.primitive.UTF8StringPointable;
import org.apache.hyracks.data.std.primitive.VoidPointable;
import org.apache.hyracks.data.std.util.ArrayBackedValueStorage;
import org.apache.hyracks.dataflow.common.data.accessors.IFrameTupleReference;

public class ParseDateTimeDescriptor extends AbstractScalarFunctionDynamicDescriptor {
    private static final long serialVersionUID = 1L;
    public final static FunctionIdentifier FID = AsterixBuiltinFunctions.PARSE_DATETIME;
    private final static DateTimeFormatUtils DT_UTILS = DateTimeFormatUtils.getInstance();
    public final static IFunctionDescriptorFactory FACTORY = new IFunctionDescriptorFactory() {

        @Override
        public IFunctionDescriptor createFunctionDescriptor() {
            return new ParseDateTimeDescriptor();
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
                    private IPointable argPtr0 = new VoidPointable();
                    private IPointable argPtr1 = new VoidPointable();
                    private IScalarEvaluator eval0 = args[0].createScalarEvaluator(ctx);
                    private IScalarEvaluator eval1 = args[1].createScalarEvaluator(ctx);

                    @SuppressWarnings("unchecked")
                    private ISerializerDeserializer<ADateTime> datetimeSerde = AqlSerializerDeserializerProvider.INSTANCE
                            .getSerializerDeserializer(BuiltinType.ADATETIME);

                    private AMutableDateTime aDateTime = new AMutableDateTime(0);
                    private final UTF8StringPointable utf8Ptr = new UTF8StringPointable();

                    @Override
                    public void evaluate(IFrameTupleReference tuple, IPointable result) throws AlgebricksException {
                        resultStorage.reset();
                        eval0.evaluate(tuple, argPtr0);
                        eval1.evaluate(tuple, argPtr1);

                        byte[] bytes0 = argPtr0.getByteArray();
                        int offset0 = argPtr0.getStartOffset();
                        int len0 = argPtr0.getLength();
                        byte[] bytes1 = argPtr1.getByteArray();
                        int offset1 = argPtr1.getStartOffset();
                        int len1 = argPtr1.getLength();

                        try {
                            if (bytes0[offset0] != ATypeTag.SERIALIZED_STRING_TYPE_TAG
                                    || bytes1[offset1] != ATypeTag.SERIALIZED_STRING_TYPE_TAG) {
                                throw new AlgebricksException(getIdentifier().getName()
                                        + ": expects two strings but got  ("
                                        + EnumDeserializer.ATYPETAGDESERIALIZER.deserialize(bytes0[offset0]) + ", "
                                        + EnumDeserializer.ATYPETAGDESERIALIZER.deserialize(bytes1[offset1]) + ")");
                            }
                            utf8Ptr.set(bytes0, offset0 + 1, len0 - 1);
                            int start0 = utf8Ptr.getCharStartOffset();
                            int length0 = utf8Ptr.getUTF8Length();

                            utf8Ptr.set(bytes1, offset1 + 1, len1 - 1);
                            int start1 = utf8Ptr.getCharStartOffset();
                            int length1 = utf8Ptr.getUTF8Length();
                            long chronon = 0;

                            int formatStart = start1;
                            int formatLength = 0;
                            boolean processSuccessfully = false;
                            while (!processSuccessfully && formatStart < start1 + length1) {
                                // search for "|"
                                formatLength = 0;
                                for (; formatStart + formatLength < start1 + length1; formatLength++) {
                                    if (bytes1[formatStart + formatLength] == '|') {
                                        break;
                                    }
                                }
                                try {
                                    chronon = DT_UTILS.parseDateTime(bytes0, start0, length0, bytes1, formatStart,
                                            formatLength, DateTimeParseMode.DATETIME);
                                } catch (AsterixTemporalTypeParseException ex) {
                                    formatStart += formatLength + 1;
                                    continue;
                                }
                                processSuccessfully = true;
                            }

                            if (!processSuccessfully) {
                                throw new HyracksDataException(
                                        "parse-datetime: Failed to match with any given format string!");
                            }
                            aDateTime.setValue(chronon);
                            datetimeSerde.serialize(aDateTime, out);
                        } catch (HyracksDataException ex) {
                            throw new AlgebricksException(ex);
                        }
                        result.set(resultStorage);
                    }
                };
            }

        };
    }

    /* (non-Javadoc)
     * @see org.apache.asterix.om.functions.AbstractFunctionDescriptor#getIdentifier()
     */
    @Override
    public FunctionIdentifier getIdentifier() {
        return FID;
    }

}