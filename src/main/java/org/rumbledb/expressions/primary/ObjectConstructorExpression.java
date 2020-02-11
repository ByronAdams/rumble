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

package org.rumbledb.expressions.primary;


import org.rumbledb.exceptions.ExceptionMetadata;
import org.rumbledb.expressions.CommaExpression;
import org.rumbledb.expressions.Expression;
import org.rumbledb.expressions.Node;
import sparksoniq.semantics.visitor.AbstractNodeVisitor;

import java.util.ArrayList;
import java.util.List;

public class ObjectConstructorExpression extends PrimaryExpression {

    private boolean isMergedConstructor = false;
    private List<Expression> _values;
    private List<Expression> _keys;
    private CommaExpression childExpression;

    public ObjectConstructorExpression(
            List<Expression> keys,
            List<Expression> values,
            ExceptionMetadata metadata
    ) {
        super(metadata);
        this._keys = keys;
        this._values = values;
    }

    public ObjectConstructorExpression(CommaExpression expression, ExceptionMetadata metadata) {
        super(metadata);
        this.childExpression = expression;
        this.isMergedConstructor = true;
    }

    public List<Expression> getKeys() {
        return this._keys;
    }

    public List<Expression> getValues() {
        return this._values;
    }

    public boolean isMergedConstructor() {
        return this.isMergedConstructor;
    }

    public CommaExpression getChildExpression() {
        return this.childExpression;
    }

    @Override
    public List<Node> getDescendants(boolean depthSearch) {
        List<Node> result = new ArrayList<>();
        if (!this.isMergedConstructor) {
            result.addAll(this._keys);
            result.addAll(this._values);
        } else
            result.add(this.childExpression);
        return getDescendantsFromChildren(result, depthSearch);
    }

    @Override
    public <T> T accept(AbstractNodeVisitor<T> visitor, T argument) {
        return visitor.visitObjectConstructor(this, argument);
    }

    @Override
    public String serializationString(boolean prefix) {
        StringBuilder builder = new StringBuilder();
        builder.append("(primaryExpr (objectConstructor {");
        if (!this.isMergedConstructor) {
            builder.append(" ");
            for (Expression key : this._keys) {
                builder.append("(pairConstructor (exprSingle ");
                builder.append(key.serializationString(false));
                builder.append(") : (exprSingle ");
                builder.append(this._values.get(this._keys.indexOf(key)).serializationString(false));
                builder.append("))");
                builder.append((this._keys.indexOf(key) < this._keys.size() - 1 ? " , " : " "));
            }
        } else {
            builder.append("| " + this.childExpression.serializationString(prefix) + " |");
        }
        builder.append("}))");
        return builder.toString();
    }

}
