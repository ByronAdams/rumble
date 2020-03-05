/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Authors: Stefan Irimescu, Can Berker Cikis
 *
 */

package org.rumbledb.expressions.operational;

import org.rumbledb.exceptions.ExceptionMetadata;
import org.rumbledb.expressions.AbstractNodeVisitor;
import org.rumbledb.expressions.Expression;
import org.rumbledb.expressions.operational.base.BinaryExpressionBase;

import java.util.Arrays;

public class ComparisonExpression extends BinaryExpressionBase {

    public static final Operator[] operators = new Operator[] {
        Operator.VC_GE,
        Operator.VC_GT,
        Operator.VC_EQ,
        Operator.VC_NE,
        Operator.VC_LE,
        Operator.VC_LT,
        Operator.GC_GE,
        Operator.GC_GT,
        Operator.GC_EQ,
        Operator.GC_NE,
        Operator.GC_LE,
        Operator.GC_LT };


    public ComparisonExpression(
            Expression mainExpression,
            Expression rhs,
            Operator op,
            ExceptionMetadata metadata
    ) {
        super(mainExpression, rhs, op, metadata);
        validateOperator(Arrays.asList(operators), op);
    }

    @Override
    public <T> T accept(AbstractNodeVisitor<T> visitor, T argument) {
        return visitor.visitComparisonExpr(this, argument);
    }

    @Override
    public String serializationString(boolean prefix) {
        String result = "(comparisonExpr ";
        result += this.mainExpression.serializationString(true);
        if (this.getRightExpression() != null) {
            result += " "
                + this.getOperator().toString().toLowerCase()
                + " "
                + this.getRightExpression().serializationString(true);
        }
        result += ")";
        return result;
    }
}
