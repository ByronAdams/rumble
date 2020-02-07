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

package org.rumbledb.expressions.module;


import org.rumbledb.exceptions.ExceptionMetadata;
import org.rumbledb.expressions.CommaExpression;
import org.rumbledb.expressions.Expression;
import org.rumbledb.expressions.Node;
import sparksoniq.semantics.visitor.AbstractNodeVisitor;

import java.util.ArrayList;
import java.util.List;

public class MainModule extends Expression {

    private final Prolog _prolog;
    private final CommaExpression _commaExpression;

    public MainModule(Prolog _prolog, CommaExpression _commaExpression, ExceptionMetadata metadata) {
        super(metadata);
        this._prolog = _prolog;
        this._commaExpression = _commaExpression;
    }

    public Prolog get_prolog() {
        return this._prolog;
    }

    public CommaExpression get_commaExpression() {
        return this._commaExpression;
    }

    @Override
    public List<Node> getDescendants(boolean depthSearch) {
        List<Node> result = new ArrayList<>();
        if (this._prolog != null) {
            result.add(this._prolog);
        }
        if (this._commaExpression != null) {
            result.add(this._commaExpression);
        }
        return getDescendantsFromChildren(result, depthSearch);
    }

    @Override
    public <T> T accept(AbstractNodeVisitor<T> visitor, T argument) {
        return visitor.visitMainModule(this, argument);
    }

    @Override
    public String serializationString(boolean prefix) {
        String result = "(mainModule ";
        result += " (prolog " + this._prolog.serializationString(false) + "), ";
        result += " (expr " + this._commaExpression.serializationString(false) + ") ";
        result += ")";
        return result;
    }
}

