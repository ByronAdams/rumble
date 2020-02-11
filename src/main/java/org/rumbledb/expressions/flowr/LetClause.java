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

package org.rumbledb.expressions.flowr;

import sparksoniq.jsoniq.ExecutionMode;


import sparksoniq.semantics.visitor.AbstractNodeVisitor;



import java.util.ArrayList;
import java.util.List;

import org.rumbledb.exceptions.ExceptionMetadata;
import org.rumbledb.exceptions.SemanticException;
import org.rumbledb.expressions.Node;

public class LetClause extends FlworClause {

    private final List<LetClauseVar> letVars;

    public LetClause(List<LetClauseVar> vars, ExceptionMetadata metadataFromContext) {
        super(FLWOR_CLAUSES.LET, metadataFromContext);
        if (vars == null || vars.isEmpty())
            throw new SemanticException("Let clause must have at least one variable", metadataFromContext);
        this.letVars = vars;

        // chain letVariables with previousClause relationship
        for (int varIndex = letVars.size() - 1; varIndex > 0; varIndex--) {
            letVars.get(varIndex).setPreviousClause(letVars.get(varIndex - 1));
        }
    }

    public List<LetClauseVar> getLetVariables() {
        return letVars;
    }

    @Override
    public void setPreviousClause(FlworClause previousClause) {
        super.setPreviousClause(previousClause);
        // assign the previous clause of the LetClause as the first variable definition's previous
        letVars.get(0).previousClause = this.previousClause;
    }

    @Override
    public void initHighestExecutionMode() {
        // call isDataFrame on the last letVariable
        this._highestExecutionMode = letVars.get(letVars.size() - 1).getHighestExecutionMode();
    }

    @Override
    public List<Node> getDescendants(boolean depthSearch) {
        List<Node> result = new ArrayList<>();
        letVars.forEach(e -> {
            if (e != null)
                result.add(e);
        });
        return getDescendantsFromChildren(result, depthSearch);
    }

    @Override
    public <T> T accept(AbstractNodeVisitor<T> visitor, T argument) {
        return visitor.visitLetClause(this, argument);
    }

    @Override
    public String serializationString(boolean prefix) {
        String result = "(letClause let ";
        for (LetClauseVar var : letVars)
            result += var.serializationString(true)
                + (letVars.indexOf(var) < letVars.size() - 1 ? " , " : "");
        result += ")";
        return result;
    }
}