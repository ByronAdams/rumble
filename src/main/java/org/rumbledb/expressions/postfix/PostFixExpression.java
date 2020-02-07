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

package org.rumbledb.expressions.postfix;

import org.rumbledb.exceptions.ExceptionMetadata;
import org.rumbledb.expressions.Expression;
import org.rumbledb.expressions.Node;
import org.rumbledb.expressions.postfix.extensions.PostfixExtension;
import org.rumbledb.expressions.primary.PrimaryExpression;
import sparksoniq.jsoniq.ExecutionMode;
import sparksoniq.semantics.visitor.AbstractNodeVisitor;

import java.util.ArrayList;
import java.util.List;


public class PostFixExpression extends Expression {

    private PrimaryExpression _primaryExpressionNode;
    private List<PostfixExtension> _extensions = null;

    public PostFixExpression(PrimaryExpression primaryExpressionNode, ExceptionMetadata metadata) {
        super(metadata);
        this._primaryExpressionNode = primaryExpressionNode;
    }

    public PostFixExpression(
            PrimaryExpression primaryExpressionNode,
            List<PostfixExtension> extensions,
            ExceptionMetadata metadata
    ) {
        super(metadata);
        this._primaryExpressionNode = primaryExpressionNode;
        this._extensions = new ArrayList<>();
        this._extensions.addAll(extensions);
    }

    public boolean isPrimary() {
        return this._extensions == null || this._extensions.isEmpty();
    }

    public PrimaryExpression get_primaryExpressionNode() {
        return this._primaryExpressionNode;
    }

    public List<PostfixExtension> getExtensions() {
        return this._extensions;
    }

    private boolean bypassCurrentExpressionForExecutionModeOperations() {
        return isPrimary();
    }

    @Override
    public void initHighestExecutionMode() {
        if (bypassCurrentExpressionForExecutionModeOperations()) {
            return;
        }
        this.highestExecutionMode = this._primaryExpressionNode.getHighestExecutionMode();
    }

    @Override
    public ExecutionMode getHighestExecutionMode(boolean ignoreUnsetError) {
        if (bypassCurrentExpressionForExecutionModeOperations()) {
            return this._primaryExpressionNode.getHighestExecutionMode(ignoreUnsetError);
        }
        return super.getHighestExecutionMode(ignoreUnsetError);
    }

    @Override
    public <T> T accept(AbstractNodeVisitor<T> visitor, T argument) {
        return visitor.visitPostfixExpression(this, argument);
    }

    @Override
    public List<Node> getDescendants(boolean depthSearch) {
        List<Node> result = new ArrayList<>();
        result.add(this._primaryExpressionNode);
        if (this._extensions != null)
            this._extensions.forEach(e -> {
                if (e != null)
                    result.add(e);
            });
        return getDescendantsFromChildren(result, depthSearch);
    }

    @Override
    public String serializationString(boolean prefix) {
        String result = "(postFixExpr ";
        result += get_primaryExpressionNode().serializationString(true);
        if (this._extensions != null && this._extensions.size() > 0) {
            for (PostfixExtension ext : this._extensions)
                result += " "
                    + ext.serializationString(true)
                    + (this._extensions.indexOf(ext) < this._extensions.size() - 1 ? " " : "");
        }
        result += ")";
        return result;
    }

}
