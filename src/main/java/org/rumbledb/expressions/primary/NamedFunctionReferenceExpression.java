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
import org.rumbledb.expressions.Node;
import sparksoniq.jsoniq.runtime.iterator.functions.base.FunctionIdentifier;
import sparksoniq.semantics.visitor.AbstractNodeVisitor;

import java.util.ArrayList;
import java.util.List;


<<<<<<< HEAD:src/main/java/org/rumbledb/expressions/primary/NamedFunctionReferenceExpression.java
=======
import org.rumbledb.expressions.Node;

import org.rumbledb.exceptions.ExceptionMetadata;

>>>>>>> c94fc8ddae10d0d8652a240536e13bdcdb7fce0d:src/main/java/org/rumbledb/expressions/primary/NamedFunctionRef.java

public class NamedFunctionReferenceExpression extends PrimaryExpression {

    private final FunctionIdentifier identifier;

    public NamedFunctionReferenceExpression(FunctionIdentifier identifier, ExceptionMetadata metadata) {
        super(metadata);
        this.identifier = identifier;
    }

    public FunctionIdentifier getIdentifier() {
        return this.identifier;
    }

    @Override
    public List<Node> getChildren() {
        List<Node> result = new ArrayList<>();
        return result;
    }

    @Override
    public <T> T accept(AbstractNodeVisitor<T> visitor, T argument) {
        return visitor.visitNamedFunctionRef(this, argument);
    }

    @Override
    public String serializationString(boolean prefix) {
        return "(namedFunctionRef(NCName "
            + this.identifier.getName()
            + ") (IntegerLiteral "
            + this.identifier.getArity()
            + "))";
    }
}
