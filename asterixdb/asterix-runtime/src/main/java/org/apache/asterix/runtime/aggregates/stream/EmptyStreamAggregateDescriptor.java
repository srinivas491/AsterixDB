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
package org.apache.asterix.runtime.aggregates.stream;

import org.apache.asterix.formats.nontagged.AqlSerializerDeserializerProvider;
import org.apache.asterix.om.base.ABoolean;
import org.apache.asterix.om.functions.AsterixBuiltinFunctions;
import org.apache.asterix.om.functions.IFunctionDescriptor;
import org.apache.asterix.om.functions.IFunctionDescriptorFactory;
import org.apache.asterix.om.types.BuiltinType;
import org.apache.asterix.runtime.aggregates.base.AbstractAggregateFunctionDynamicDescriptor;
import org.apache.hyracks.algebricks.common.exceptions.AlgebricksException;
import org.apache.hyracks.algebricks.core.algebra.functions.FunctionIdentifier;
import org.apache.hyracks.algebricks.runtime.base.IAggregateEvaluator;
import org.apache.hyracks.algebricks.runtime.base.IAggregateEvaluatorFactory;
import org.apache.hyracks.algebricks.runtime.base.IScalarEvaluatorFactory;
import org.apache.hyracks.api.context.IHyracksTaskContext;
import org.apache.hyracks.api.dataflow.value.ISerializerDeserializer;
import org.apache.hyracks.api.exceptions.HyracksDataException;
import org.apache.hyracks.data.std.api.IPointable;
import org.apache.hyracks.data.std.util.ArrayBackedValueStorage;
import org.apache.hyracks.dataflow.common.data.accessors.IFrameTupleReference;

public class EmptyStreamAggregateDescriptor extends AbstractAggregateFunctionDynamicDescriptor {

    private static final long serialVersionUID = 1L;

    public final static FunctionIdentifier FID = AsterixBuiltinFunctions.EMPTY_STREAM;
    public static final IFunctionDescriptorFactory FACTORY = new IFunctionDescriptorFactory() {
        @Override
        public IFunctionDescriptor createFunctionDescriptor() {
            return new EmptyStreamAggregateDescriptor();
        }
    };

    @Override
    public IAggregateEvaluatorFactory createAggregateEvaluatorFactory(IScalarEvaluatorFactory[] args)
            throws AlgebricksException {
        return new IAggregateEvaluatorFactory() {

            private static final long serialVersionUID = 1L;

            @Override
            public IAggregateEvaluator createAggregateEvaluator(final IHyracksTaskContext ctx)
                    throws AlgebricksException {

                return new IAggregateEvaluator() {

                    private ArrayBackedValueStorage resultStorage = new ArrayBackedValueStorage();
                    @SuppressWarnings("rawtypes")
                    private ISerializerDeserializer serde = AqlSerializerDeserializerProvider.INSTANCE
                            .getSerializerDeserializer(BuiltinType.ABOOLEAN);

                    boolean res = true;

                    @Override
                    public void init() throws AlgebricksException {
                        res = true;
                    }

                    @Override
                    public void step(IFrameTupleReference tuple) throws AlgebricksException {
                        res = false;
                    }

                    @SuppressWarnings("unchecked")
                    @Override
                    public void finish(IPointable result) throws AlgebricksException {
                        resultStorage.reset();
                        ABoolean b = res ? ABoolean.TRUE : ABoolean.FALSE;
                        try {
                            serde.serialize(b, resultStorage.getDataOutput());
                        } catch (HyracksDataException e) {
                            throw new AlgebricksException(e);
                        }
                        result.set(resultStorage);
                    }

                    @Override
                    public void finishPartial(IPointable result) throws AlgebricksException {
                        finish(result);
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
