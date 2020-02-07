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

package org.rumbledb.expressions.postfix.extensions;


import org.rumbledb.exceptions.ExceptionMetadata;
import org.rumbledb.expressions.Node;
import org.rumbledb.expressions.postfix.PostFixExpression;
import sparksoniq.semantics.visitor.AbstractNodeVisitor;

import java.util.ArrayList;
import java.util.List;


public abstract class PostfixExtension extends Node {

    private PostFixExpression parent;
    private PostfixExtension previous;

    protected PostfixExtension(ExceptionMetadata metadata) {
        super(metadata);
    }

    public PostFixExpression getParent() {
        return this.parent;
    }

    public void setParent(PostFixExpression _parent) {
        this.parent = _parent;
    }

    public PostfixExtension getPrevious() {
        return this.previous;
    }

    public void setPrevious(PostfixExtension previous) {
        this.previous = previous;
    }

    @Override
    public List<Node> getDescendants(boolean depthSearch) {
        return new ArrayList<>();
    }

    @Override
    public void initHighestExecutionMode() {
        if (this.previous != null) {
            this.highestExecutionMode = this.previous.getHighestExecutionMode();
        } else {
            this.highestExecutionMode = this.parent.getHighestExecutionMode();
        }
    }

    @Override
    public <T> T accept(AbstractNodeVisitor<T> visitor, T argument) {
        return visitor.visitDescendants(this, argument);
    }

    @Override
    public String serializationString(boolean prefix) {
        return "";
    }
}
