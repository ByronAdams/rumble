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

package sparksoniq.jsoniq.runtime.iterator.functions;

import org.apache.spark.api.java.JavaRDD;
import org.rumbledb.api.Item;
import org.rumbledb.exceptions.ExceptionMetadata;
import org.rumbledb.exceptions.IteratorFlowException;
import sparksoniq.jsoniq.ExecutionMode;
import sparksoniq.jsoniq.runtime.iterator.HybridRuntimeIterator;
import sparksoniq.jsoniq.runtime.iterator.RuntimeIterator;
import sparksoniq.jsoniq.runtime.iterator.functions.base.FunctionIdentifier;
import sparksoniq.jsoniq.runtime.iterator.functions.base.Functions;
import sparksoniq.semantics.DynamicContext;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class StaticUserDefinedFunctionCallIterator extends HybridRuntimeIterator {
    // static: functionIdentifier known at compile time

    private static final long serialVersionUID = 1L;
    // parametrized fields
    private FunctionIdentifier _functionIdentifier;
    private List<RuntimeIterator> _functionArguments;

    // calculated fields
    private RuntimeIterator _userDefinedFunctionCallIterator;
    private Item _nextResult;


    public StaticUserDefinedFunctionCallIterator(
            FunctionIdentifier functionIdentifier,
            List<RuntimeIterator> functionArguments,
            ExecutionMode executionMode,
            ExceptionMetadata iteratorMetadata
    ) {
        super(null, executionMode, iteratorMetadata);
        this._functionIdentifier = functionIdentifier;
        this._functionArguments = functionArguments;

    }

    @Override
    public void openLocal() {
        this._userDefinedFunctionCallIterator = Functions.getUserDefinedFunctionCallIterator(
            this._functionIdentifier,
            this.getHighestExecutionMode(),
            getMetadata(),
            this._functionArguments
        );
        this._userDefinedFunctionCallIterator.open(this._currentDynamicContextForLocalExecution);
        setNextResult();
    }

    @Override
    public Item nextLocal() {
        if (this._hasNext) {
            Item result = this._nextResult;
            setNextResult();
            return result;
        }
        throw new IteratorFlowException(
                RuntimeIterator.FLOW_EXCEPTION_MESSAGE + " in " + this._functionIdentifier.getName() + "  function",
                getMetadata()
        );
    }

    @Override
    protected boolean hasNextLocal() {
        return this._hasNext;
    }

    @Override
    protected void resetLocal(DynamicContext context) {
        this._userDefinedFunctionCallIterator.reset(this._currentDynamicContextForLocalExecution);
        setNextResult();
    }

    @Override
    protected void closeLocal() {
        // ensure that recursive function calls terminate gracefully
        // the function call in the body of the deepest recursion call is never visited, never opened and never closed
        if (this.isOpen()) {
            this._userDefinedFunctionCallIterator.close();
        }
    }

    public void setNextResult() {
        this._nextResult = null;
        if (this._userDefinedFunctionCallIterator.hasNext()) {
            this._nextResult = this._userDefinedFunctionCallIterator.next();
        }

        if (this._nextResult == null) {
            this._hasNext = false;
            this._userDefinedFunctionCallIterator.close();
        } else {
            this._hasNext = true;
        }
    }

    @Override
    public JavaRDD<Item> getRDDAux(DynamicContext dynamicContext) {
        this._userDefinedFunctionCallIterator = Functions.getUserDefinedFunctionCallIterator(
            this._functionIdentifier,
            this.getHighestExecutionMode(),
            getMetadata(),
            this._functionArguments
        );
        return this._userDefinedFunctionCallIterator.getRDD(dynamicContext);
    }

    public Map<String, DynamicContext.VariableDependency> getVariableDependencies() {
        Map<String, DynamicContext.VariableDependency> result =
            new TreeMap<>(super.getVariableDependencies());
        for (RuntimeIterator iterator : this._functionArguments) {
            if (iterator == null) {
                continue;
            }
            result.putAll(iterator.getVariableDependencies());
        }
        return result;
    }
}
