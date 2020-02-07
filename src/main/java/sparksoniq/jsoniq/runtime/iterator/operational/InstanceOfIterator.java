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

package sparksoniq.jsoniq.runtime.iterator.operational;

import org.apache.spark.api.java.JavaRDD;
import org.rumbledb.api.Item;
import org.rumbledb.exceptions.ExceptionMetadata;
import org.rumbledb.exceptions.IteratorFlowException;
import org.rumbledb.expressions.operational.base.OperationalExpressionBase;
import sparksoniq.jsoniq.ExecutionMode;
import sparksoniq.jsoniq.item.ItemFactory;
import sparksoniq.jsoniq.runtime.iterator.RuntimeIterator;
import sparksoniq.jsoniq.runtime.iterator.functions.sequences.general.InstanceOfClosure;
import sparksoniq.jsoniq.runtime.iterator.operational.base.UnaryOperationBaseIterator;
import sparksoniq.semantics.types.ItemType;
import sparksoniq.semantics.types.SequenceType;

import java.util.ArrayList;
import java.util.List;


public class InstanceOfIterator extends UnaryOperationBaseIterator {

    private static final long serialVersionUID = 1L;
    private final SequenceType _sequenceType;

    public InstanceOfIterator(
            RuntimeIterator iterator,
            SequenceType sequenceType,
            ExecutionMode executionMode,
            ExceptionMetadata iteratorMetadata
    ) {
        super(iterator, OperationalExpressionBase.Operator.INSTANCE_OF, executionMode, iteratorMetadata);
        this._sequenceType = sequenceType;
    }

    @Override
    public Item next() {
        if (this._hasNext) {
            if (!this._child.isRDD()) {
                List<Item> items = new ArrayList<>();
                this._child.open(this._currentDynamicContextForLocalExecution);

                while (this._child.hasNext())
                    items.add(this._child.next());
                this._child.close();
                this._hasNext = false;

                if (isInvalidArity(items.size()))
                    return ItemFactory.getInstance().createBooleanItem(false);

                ItemType itemType = this._sequenceType.getItemType();
                for (Item item : items) {
                    if (!item.isTypeOf(itemType)) {
                        return ItemFactory.getInstance().createBooleanItem(false);
                    }
                }
                return ItemFactory.getInstance().createBooleanItem(true);
            } else {
                JavaRDD<Item> childRDD = this._child.getRDD(this._currentDynamicContextForLocalExecution);
                this._hasNext = false;

                if (isInvalidArity(childRDD.take(2).size()))
                    return ItemFactory.getInstance().createBooleanItem(false);

                JavaRDD<Item> result = childRDD.filter(new InstanceOfClosure(this._sequenceType.getItemType()));
                return ItemFactory.getInstance().createBooleanItem(result.isEmpty());
            }
        } else
            throw new IteratorFlowException(RuntimeIterator.FLOW_EXCEPTION_MESSAGE, getMetadata());
    }

    private boolean isInvalidArity(long numOfItems) {
        return (numOfItems != 0 && this._sequenceType.isEmptySequence())
            ||
            (numOfItems == 0
                && (this._sequenceType.getArity() == SequenceType.Arity.One
                    ||
                    this._sequenceType.getArity() == SequenceType.Arity.OneOrMore))
            ||
            (numOfItems > 1
                && (this._sequenceType.getArity() == SequenceType.Arity.One
                    ||
                    this._sequenceType.getArity() == SequenceType.Arity.OneOrZero));
    }
}
