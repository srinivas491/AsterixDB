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
package org.apache.asterix.dataflow.data.common;

import org.apache.asterix.om.typecomputer.impl.TypeComputeUtils;
import org.apache.asterix.om.types.AUnionType;
import org.apache.asterix.om.types.IAType;
import org.apache.asterix.om.types.TypeHelper;
import org.apache.hyracks.algebricks.common.exceptions.AlgebricksException;
import org.apache.hyracks.algebricks.core.algebra.expressions.IMissableTypeComputer;

public class AqlMissableTypeComputer implements IMissableTypeComputer {

    public static final AqlMissableTypeComputer INSTANCE = new AqlMissableTypeComputer();

    private AqlMissableTypeComputer() {
    }

    @Override
    public IAType makeMissableType(Object type) throws AlgebricksException {
        IAType t = (IAType) type;
        return AUnionType.createMissableType(t);
    }

    @Override
    public boolean canBeMissing(Object type) {
        IAType t = (IAType) type;
        return TypeHelper.canBeMissing(t);
    }

    @Override
    public Object getNonOptionalType(Object type) {
        IAType t = (IAType) type;
        return TypeComputeUtils.getActualType(t);
    }
}
