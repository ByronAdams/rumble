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

import org.rumbledb.api.Item;
import org.rumbledb.exceptions.ExceptionMetadata;
import org.rumbledb.exceptions.IteratorFlowException;
import org.rumbledb.exceptions.UnexpectedTypeException;
import org.rumbledb.expressions.operational.base.OperationalExpressionBase;
import sparksoniq.jsoniq.ExecutionMode;
import sparksoniq.jsoniq.item.ItemFactory;
import sparksoniq.jsoniq.runtime.iterator.RuntimeIterator;
import sparksoniq.jsoniq.runtime.iterator.operational.base.BinaryOperationBaseIterator;
import sparksoniq.semantics.DynamicContext;

public class RangeOperationIterator extends BinaryOperationBaseIterator {


    private static final long serialVersionUID = 1L;
    private int _left;
    private int _right;
    private int _index;

    public RangeOperationIterator(
            RuntimeIterator left,
            RuntimeIterator right,
            ExecutionMode executionMode,
            ExceptionMetadata iteratorMetadata
    ) {
        super(left, right, OperationalExpressionBase.Operator.TO, executionMode, iteratorMetadata);
    }

    public boolean hasNext() {
        return this._hasNext;
    }

    @Override
    public Item next() {
        if (this._hasNext) {
            if (this._index == this._right)
                this._hasNext = false;
            return ItemFactory.getInstance().createIntegerItem(this._index++);
        }
        throw new IteratorFlowException("Invalid next call in Range Operation", getMetadata());
    }

    public void open(DynamicContext context) {
        super.open(context);

        this._index = 0;
        this._leftIterator.open(this._currentDynamicContextForLocalExecution);
        this._rightIterator.open(this._currentDynamicContextForLocalExecution);
        if (this._leftIterator.hasNext() && this._rightIterator.hasNext()) {
            Item left = this._leftIterator.next();
            Item right = this._rightIterator.next();

            if (
                this._leftIterator.hasNext()
                    || this._rightIterator.hasNext()
                    || !(left.isInteger())
                    || !(right.isInteger())
            )
                throw new UnexpectedTypeException(
                        "Range expression has non numeric args "
                            +
                            left.serialize()
                            + ", "
                            + right.serialize(),
                        getMetadata()
                );
            try {
                this._left = left.castToIntegerValue();
                this._right = right.castToIntegerValue();
            } catch (IteratorFlowException e) {
                throw new IteratorFlowException(e.getJSONiqErrorMessage(), getMetadata());
            }
            if (this._right < this._left) {
                this._hasNext = false;
            } else {
                this._index = this._left;
                this._hasNext = true;
            }
        } else {
            this._hasNext = false;
        }

        this._leftIterator.close();
        this._rightIterator.close();

    }
}
