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
package org.apache.asterix.runtime.evaluators.functions;

import java.io.DataOutput;
import java.io.IOException;

import org.apache.asterix.dataflow.data.nontagged.Coordinate;
import org.apache.asterix.dataflow.data.nontagged.serde.ADoubleSerializerDeserializer;
import org.apache.asterix.dataflow.data.nontagged.serde.APointSerializerDeserializer;
import org.apache.asterix.formats.nontagged.AqlSerializerDeserializerProvider;
import org.apache.asterix.om.base.ALine;
import org.apache.asterix.om.base.AMutableLine;
import org.apache.asterix.om.base.AMutablePoint;
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
import org.apache.hyracks.data.std.api.IPointable;
import org.apache.hyracks.data.std.primitive.VoidPointable;
import org.apache.hyracks.data.std.util.ArrayBackedValueStorage;
import org.apache.hyracks.dataflow.common.data.accessors.IFrameTupleReference;

public class CreateLineDescriptor extends AbstractScalarFunctionDynamicDescriptor {

    private static final long serialVersionUID = 1L;

    public static final IFunctionDescriptorFactory FACTORY = new IFunctionDescriptorFactory() {
        @Override
        public IFunctionDescriptor createFunctionDescriptor() {
            return new CreateLineDescriptor();
        }
    };

    @Override
    public IScalarEvaluatorFactory createEvaluatorFactory(final IScalarEvaluatorFactory[] args)
            throws AlgebricksException {
        return new IScalarEvaluatorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public IScalarEvaluator createScalarEvaluator(IHyracksTaskContext ctx) throws AlgebricksException {
                return new IScalarEvaluator() {

                    private ArrayBackedValueStorage resultStorage = new ArrayBackedValueStorage();
                    private DataOutput out = resultStorage.getDataOutput();
                    private IPointable inputArg0 = new VoidPointable();
                    private IPointable inputArg1 = new VoidPointable();
                    private IScalarEvaluator eval0 = args[0].createScalarEvaluator(ctx);
                    private IScalarEvaluator eval1 = args[1].createScalarEvaluator(ctx);
                    private AMutableLine aLine = new AMutableLine(null, null);
                    private AMutablePoint[] aPoint = { new AMutablePoint(0, 0), new AMutablePoint(0, 0) };
                    @SuppressWarnings("unchecked")
                    private ISerializerDeserializer<ALine> lineSerde = AqlSerializerDeserializerProvider.INSTANCE
                            .getSerializerDeserializer(BuiltinType.ALINE);

                    @Override
                    public void evaluate(IFrameTupleReference tuple, IPointable result) throws AlgebricksException {
                        resultStorage.reset();
                        eval0.evaluate(tuple, inputArg0);
                        eval1.evaluate(tuple, inputArg1);
                        byte[] bytes0 = inputArg0.getByteArray();
                        int offset0 = inputArg0.getStartOffset();
                        byte[] bytes1 = inputArg1.getByteArray();
                        int offset1 = inputArg1.getStartOffset();

                        // type-check: (point, point)
                        if (bytes0[offset0] != ATypeTag.SERIALIZED_POINT_TYPE_TAG
                                || bytes1[offset1] != ATypeTag.SERIALIZED_POINT_TYPE_TAG) {
                            throw new AlgebricksException(AsterixBuiltinFunctions.CREATE_LINE.getName()
                                    + ": expects input type: (POINT, POINT) but got ("
                                    + EnumDeserializer.ATYPETAGDESERIALIZER.deserialize(bytes0[offset0]) + ", "
                                    + EnumDeserializer.ATYPETAGDESERIALIZER.deserialize(bytes1[offset1]) + ").");
                        }

                        try {
                            aPoint[0]
                                    .setValue(
                                            ADoubleSerializerDeserializer.getDouble(bytes0,
                                                    offset0 + APointSerializerDeserializer
                                                            .getCoordinateOffset(Coordinate.X)),
                                    ADoubleSerializerDeserializer.getDouble(bytes0,
                                            offset0 + APointSerializerDeserializer.getCoordinateOffset(Coordinate.Y)));
                            aPoint[1]
                                    .setValue(
                                            ADoubleSerializerDeserializer.getDouble(bytes1,
                                                    offset1 + APointSerializerDeserializer
                                                            .getCoordinateOffset(Coordinate.X)),
                                    ADoubleSerializerDeserializer.getDouble(bytes1,
                                            offset1 + APointSerializerDeserializer.getCoordinateOffset(Coordinate.Y)));
                            aLine.setValue(aPoint[0], aPoint[1]);
                            lineSerde.serialize(aLine, out);
                        } catch (IOException e1) {
                            throw new AlgebricksException(e1);
                        }
                        result.set(resultStorage);
                    }
                };
            }
        };
    }

    @Override
    public FunctionIdentifier getIdentifier() {
        return AsterixBuiltinFunctions.CREATE_LINE;
    }

}
