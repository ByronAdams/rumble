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

package sparksoniq.jsoniq.runtime.iterator.postfix;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.rumbledb.api.Item;
import org.rumbledb.exceptions.ExceptionMetadata;
import org.rumbledb.exceptions.IteratorFlowException;
import sparksoniq.jsoniq.ExecutionMode;
import sparksoniq.jsoniq.item.ArrayItem;
import sparksoniq.jsoniq.runtime.iterator.HybridRuntimeIterator;
import sparksoniq.jsoniq.runtime.iterator.RuntimeIterator;
import sparksoniq.semantics.DynamicContext;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

public class ArrayUnboxingIterator extends HybridRuntimeIterator {

    private static final long serialVersionUID = 1L;
    private RuntimeIterator _iterator;
    private Queue<Item> _nextResults; // queue that holds the results created by the current item in inspection

    public ArrayUnboxingIterator(
            RuntimeIterator arrayIterator,
            ExecutionMode executionMode,
            ExceptionMetadata iteratorMetadata
    ) {
        super(Arrays.asList(arrayIterator), executionMode, iteratorMetadata);
        this._iterator = arrayIterator;
    }

    @Override
    public void openLocal() {
        this._iterator.open(this._currentDynamicContextForLocalExecution);
        this._nextResults = new LinkedList<>();
        setNextResult();
    }

    @Override
    protected boolean hasNextLocal() {
        return this._hasNext;
    }

    @Override
    public Item nextLocal() {
        if (this._hasNext) {
            Item result = this._nextResults.remove(); // save the result to be returned
            if (this._nextResults.isEmpty()) {
                // if there are no more results left in the queue, trigger calculation for the next result
                setNextResult();
            }
            return result;
        }
        throw new IteratorFlowException("Invalid next call in Array Unboxing", getMetadata());
    }

    @Override
    protected void resetLocal(DynamicContext context) {
        this._iterator.reset(this._currentDynamicContextForLocalExecution);
        setNextResult();
    }

    @Override
    protected void closeLocal() {
        this._iterator.close();
    }

    private void setNextResult() {
        while (this._iterator.hasNext()) {
            Item item = this._iterator.next();
            if (item instanceof ArrayItem) {
                ArrayItem arrItem = (ArrayItem) item;
                // if array is not empty, set the first item as the result
                if (0 < arrItem.getSize()) {
                    this._nextResults.addAll(arrItem.getItems());
                    break;
                }
            }
        }

        if (this._nextResults.isEmpty()) {
            this._hasNext = false;
            this._iterator.close();
        } else {
            this._hasNext = true;
        }
    }

    @Override
    public JavaRDD<Item> getRDDAux(DynamicContext dynamicContext) {
        JavaRDD<Item> childRDD = this._children.get(0).getRDD(dynamicContext);
        FlatMapFunction<Item, Item> transformation = new ArrayUnboxingClosure();
        JavaRDD<Item> resultRDD = childRDD.flatMap(transformation);
        return resultRDD;
    }
}
