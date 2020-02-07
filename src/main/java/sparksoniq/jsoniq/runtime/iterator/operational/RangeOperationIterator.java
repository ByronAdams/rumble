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
    private int left;
    private int right;
    private int index;

    public RangeOperationIterator(
            RuntimeIterator left,
            RuntimeIterator right,
            ExecutionMode executionMode,
            ExceptionMetadata iteratorMetadata
    ) {
        super(left, right, OperationalExpressionBase.Operator.TO, executionMode, iteratorMetadata);
    }

    public boolean hasNext() {
        return this.hasNext;
    }

    @Override
    public Item next() {
        if (this.hasNext) {
            if (this.index == this.right)
                this.hasNext = false;
            return ItemFactory.getInstance().createIntegerItem(this.index++);
        }
        throw new IteratorFlowException("Invalid next call in Range Operation", getMetadata());
    }

    public void open(DynamicContext context) {
        super.open(context);

        this.index = 0;
        this.leftIterator.open(this.currentDynamicContextForLocalExecution);
        this.rightIterator.open(this.currentDynamicContextForLocalExecution);
        if (this.leftIterator.hasNext() && this.rightIterator.hasNext()) {
            Item left = this.leftIterator.next();
            Item right = this.rightIterator.next();

            if (
                this.leftIterator.hasNext()
                    || this.rightIterator.hasNext()
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
                this.left = left.castToIntegerValue();
                this.right = right.castToIntegerValue();
            } catch (IteratorFlowException e) {
                throw new IteratorFlowException(e.getJSONiqErrorMessage(), getMetadata());
            }
            if (this.right < this.left) {
                this.hasNext = false;
            } else {
                this.index = this.left;
                this.hasNext = true;
            }
        } else {
            this.hasNext = false;
        }

        this.leftIterator.close();
        this.rightIterator.close();

    }
}
