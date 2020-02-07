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

package org.rumbledb.expressions.quantifiers;

import org.rumbledb.exceptions.ExceptionMetadata;
import org.rumbledb.expressions.Expression;
import org.rumbledb.expressions.Node;
import org.rumbledb.expressions.primary.VariableReference;
import sparksoniq.semantics.types.SequenceType;
import sparksoniq.semantics.visitor.AbstractNodeVisitor;

import java.util.ArrayList;
import java.util.List;

public class QuantifiedExpressionVar extends Node {
    private final VariableReference _variableReference;
    private final Expression expression;
    private final SequenceType sequenceType;

    public QuantifiedExpressionVar(
            VariableReference varRef,
            Expression varExpression,
            SequenceType sequenceType,
            ExceptionMetadata metadata
    ) {
        super(metadata);
        this._variableReference = varRef;
        this.expression = varExpression;
        this.sequenceType = sequenceType;
    }

    public Expression getExpression() {
        return this.expression;
    }

    public VariableReference getVariableReference() {

        return this._variableReference;
    }

    public SequenceType getSequenceType() {
        return this.sequenceType;
    }

    @Override
    public List<Node> getDescendants(boolean depthSearch) {
        List<Node> result = new ArrayList<>();
        result.add(this._variableReference);
        result.add(this.expression);
        return getDescendantsFromChildren(result, depthSearch);
    }

    @Override
    public <T> T accept(AbstractNodeVisitor<T> visitor, T argument) {
        return visitor.visitQuantifiedExpressionVar(this, argument);
    }

    @Override
    public String serializationString(boolean prefix) {
        return null;
    }
}
