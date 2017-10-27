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

package org.apache.asterix.runtime.evaluators.functions.binary;

import org.apache.asterix.formats.nontagged.AqlSerializerDeserializerProvider;
import org.apache.asterix.om.base.AInt64;
import org.apache.asterix.om.base.AMutableInt64;
import org.apache.asterix.om.types.ATypeTag;
import org.apache.asterix.om.types.BuiltinType;
import org.apache.hyracks.algebricks.common.exceptions.AlgebricksException;
import org.apache.hyracks.algebricks.runtime.base.IScalarEvaluatorFactory;
import org.apache.hyracks.api.context.IHyracksTaskContext;
import org.apache.hyracks.api.dataflow.value.ISerializerDeserializer;
import org.apache.hyracks.api.exceptions.HyracksDataException;
import org.apache.hyracks.data.std.api.IPointable;
import org.apache.hyracks.data.std.primitive.ByteArrayPointable;
import org.apache.hyracks.dataflow.common.data.accessors.IFrameTupleReference;

public abstract class AbstractFindBinaryEvaluator extends AbstractBinaryScalarEvaluator {

    private static final ATypeTag[] EXPECTED_INPUT_TAG = { ATypeTag.BINARY, ATypeTag.BINARY };
    protected String functionName;
    protected AMutableInt64 result = new AMutableInt64(-1);
    protected final ByteArrayPointable textPtr = new ByteArrayPointable();
    protected final ByteArrayPointable wordPtr = new ByteArrayPointable();

    @SuppressWarnings("unchecked")
    protected ISerializerDeserializer<AInt64> intSerde = AqlSerializerDeserializerProvider.INSTANCE
            .getSerializerDeserializer(BuiltinType.AINT64);

    public AbstractFindBinaryEvaluator(IHyracksTaskContext context, IScalarEvaluatorFactory[] copyEvaluatorFactories,
            String functionName) throws AlgebricksException {
        super(context, copyEvaluatorFactories);
        this.functionName = functionName;
    }

    @Override
    public void evaluate(IFrameTupleReference tuple, IPointable resultPointable) throws AlgebricksException {
        resultStorage.reset();
        for (int i = 0; i < pointables.length; ++i) {
            evaluators[i].evaluate(tuple, pointables[i]);
        }

        int fromOffset = getFromOffset(tuple);
        try {
            ATypeTag textTag = ATypeTag.VALUE_TYPE_MAPPING[pointables[0].getByteArray()[pointables[0]
                    .getStartOffset()]];
            ATypeTag wordTag = ATypeTag.VALUE_TYPE_MAPPING[pointables[1].getByteArray()[pointables[1]
                    .getStartOffset()]];

            checkTypeMachingThrowsIfNot(functionName, EXPECTED_INPUT_TAG, textTag, wordTag);
            textPtr.set(pointables[0].getByteArray(), pointables[0].getStartOffset() + 1,
                    pointables[0].getLength() - 1);
            wordPtr.set(pointables[1].getByteArray(), pointables[0].getStartOffset() + 1,
                    pointables[1].getLength() - 1);
            result.setValue(1L + indexOf(textPtr.getByteArray(), textPtr.getContentStartOffset(),
                    textPtr.getContentLength(), wordPtr.getByteArray(), wordPtr.getContentStartOffset(),
                    wordPtr.getContentLength(), fromOffset));
            intSerde.serialize(result, dataOutput);
        } catch (HyracksDataException e) {
            throw new AlgebricksException(e);
        }
        resultPointable.set(resultStorage);
    }

    protected abstract int getFromOffset(IFrameTupleReference tuple) throws AlgebricksException;

    // copy from String.indexOf(String)
    private int indexOf(byte[] source, int sourceOffset, int sourceCount, byte[] target, int targetOffset,
            int targetCount, int fromIndex) {
        if (fromIndex >= sourceCount) {
            return targetCount == 0 ? sourceCount : -1;
        }
        int from = fromIndex;
        if (from < 0) {
            from = 0;
        }
        if (targetCount == 0) {
            return from;
        }

        byte first = target[targetOffset];
        int max = sourceOffset + (sourceCount - targetCount);

        for (int i = sourceOffset + fromIndex; i <= max; i++) {
            /* Look for first character. */
            if (source[i] != first) {
                continue;
            }

            /* Found first character, now look at the rest of v2 */
            int j = i + 1;
            int end = j + targetCount - 1;
            for (int k = targetOffset + 1; j < end && source[j] == target[k]; j++, k++) {
                ;
            }
            if (j == end) {
                /* Found whole string. */
                return i - sourceOffset;
            }
        }
        return -1;
    }

}
