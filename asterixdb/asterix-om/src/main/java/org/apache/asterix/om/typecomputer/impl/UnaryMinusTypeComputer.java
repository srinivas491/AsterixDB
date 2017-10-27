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
package org.apache.asterix.om.typecomputer.impl;

import org.apache.asterix.om.typecomputer.base.AbstractResultTypeComputer;
import org.apache.asterix.om.types.ATypeTag;
import org.apache.asterix.om.types.IAType;
import org.apache.hyracks.algebricks.common.exceptions.AlgebricksException;
import org.apache.hyracks.algebricks.common.exceptions.NotImplementedException;
import org.apache.hyracks.algebricks.core.algebra.base.ILogicalExpression;

public class UnaryMinusTypeComputer extends AbstractResultTypeComputer {

    public static final UnaryMinusTypeComputer INSTANCE = new UnaryMinusTypeComputer();

    private UnaryMinusTypeComputer() {
    }

    @Override
    public void checkArgType(int argIndex, IAType type) throws AlgebricksException {
        ATypeTag tag = type.getTypeTag();
        switch (tag) {
            case INT8:
            case INT16:
            case INT32:
            case INT64:
            case FLOAT:
            case DOUBLE:
                break;
            default:
                throw new NotImplementedException(
                        "Negative operations are not implemented for " + type.getDisplayName());
        }
    }

    @Override
    public IAType getResultType(ILogicalExpression expr, IAType... knownTypes) throws AlgebricksException {
        ATypeTag tag = knownTypes[0].getTypeTag();
        switch (tag) {
            case INT8:
            case INT16:
            case INT32:
            case INT64:
            case FLOAT:
            case DOUBLE:
                return knownTypes[0];
            default:
                return null;
        }
    }
}
