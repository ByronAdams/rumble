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

<<<<<<< HEAD
import org.rumbledb.exceptions.ExceptionMetadata;
import org.rumbledb.expressions.Node;
import sparksoniq.semantics.visitor.AbstractNodeVisitor;

import java.util.ArrayList;
import java.util.List;
=======
import org.rumbledb.expressions.Node;
import org.rumbledb.exceptions.ExceptionMetadata;
>>>>>>> c94fc8ddae10d0d8652a240536e13bdcdb7fce0d

/**
 * GRAMMAR:flowrExpression
 */

public abstract class FlworClause extends Node {

    protected FlworClause previousClause;
    protected FLWOR_CLAUSES clauseType;


    public FlworClause(FLWOR_CLAUSES clauseType, ExceptionMetadata metadata) {
        super(metadata);
        this.clauseType = clauseType;
    }

    public FLWOR_CLAUSES getClauseType() {
        return this.clauseType;
    }

    public FlworClause getPreviousClause() {
        return this.previousClause;
    }

    public void setPreviousClause(FlworClause previousClause) {
        this.previousClause = previousClause;
    }

    /**
     * This method is overridden in clauses such as for and let for special behavior
     */
    @Override
    public void initHighestExecutionMode() {
        this.highestExecutionMode = this.previousClause.getHighestExecutionMode();
    }

    @Override
    public String serializationString(boolean prefix) {
        return "";
    }
}
