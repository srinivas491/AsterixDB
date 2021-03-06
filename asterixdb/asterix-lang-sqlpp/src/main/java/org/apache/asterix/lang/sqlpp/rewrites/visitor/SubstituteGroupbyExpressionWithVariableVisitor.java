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

package org.apache.asterix.lang.sqlpp.rewrites.visitor;

import java.util.HashMap;
import java.util.Map;

import org.apache.asterix.common.exceptions.AsterixException;
import org.apache.asterix.common.functions.FunctionSignature;
import org.apache.asterix.lang.common.base.Expression;
import org.apache.asterix.lang.common.base.Expression.Kind;
import org.apache.asterix.lang.common.base.ILangExpression;
import org.apache.asterix.lang.common.expression.CallExpr;
import org.apache.asterix.lang.common.expression.GbyVariableExpressionPair;
import org.apache.asterix.lang.common.rewrites.ExpressionSubstitutionEnvironment;
import org.apache.asterix.lang.sqlpp.clause.SelectBlock;
import org.apache.asterix.lang.sqlpp.expression.SelectExpression;
import org.apache.asterix.lang.sqlpp.util.FunctionMapUtil;
import org.apache.asterix.lang.sqlpp.util.SqlppVariableUtil;
import org.apache.asterix.lang.sqlpp.visitor.SqlppSubstituteExpressionsVisitor;
import org.apache.asterix.lang.sqlpp.visitor.base.AbstractSqlppSimpleExpressionVisitor;

// Replaces expressions that appear in having/select/order-by/limit clause and are identical to some
// group by key expression with the group by key expression.
public class SubstituteGroupbyExpressionWithVariableVisitor extends AbstractSqlppSimpleExpressionVisitor {

    @Override
    public Expression visit(SelectBlock selectBlock, ILangExpression arg) throws AsterixException {
        if (selectBlock.hasGroupbyClause()) {
            Map<Expression, Expression> map = new HashMap<>();
            for (GbyVariableExpressionPair gbyKeyPair : selectBlock.getGroupbyClause().getGbyPairList()) {
                Expression gbyKeyExpr = gbyKeyPair.getExpr();
                if (gbyKeyExpr.getKind() != Kind.VARIABLE_EXPRESSION) {
                    map.put(gbyKeyExpr, gbyKeyPair.getVar());
                }
            }

            // Creates a substitution visitor.
            ExpressionSubstitutionEnvironment env =
                    new ExpressionSubstitutionEnvironment(map, SqlppVariableUtil::getFreeVariables);
            SubstituteGroupbyExpressionVisitor visitor = new SubstituteGroupbyExpressionVisitor();

            // Rewrites having/select/order-by/limit clauses.
            if (selectBlock.hasHavingClause()) {
                selectBlock.getHavingClause().accept(visitor, env);
            }
            selectBlock.getSelectClause().accept(visitor, env);
            SelectExpression selectExpression = (SelectExpression) arg;
            if (selectExpression.hasOrderby()) {
                selectExpression.getOrderbyClause().accept(visitor, env);
            }
            if (selectExpression.hasLimit()) {
                selectExpression.getLimitClause().accept(visitor, env);
            }
        }
        return super.visit(selectBlock, arg);
    }

}

class SubstituteGroupbyExpressionVisitor extends SqlppSubstituteExpressionsVisitor {

    @Override
    public Expression visit(CallExpr callExpr, ExpressionSubstitutionEnvironment env) throws AsterixException {
        FunctionSignature signature = callExpr.getFunctionSignature();
        if (FunctionMapUtil.isSql92AggregateFunction(signature)) {
            return callExpr;
        } else {
            return super.visit(callExpr, env);
        }
    }
}
