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

public class StringConcatIterator extends BinaryOperationBaseIterator {

    private static final long serialVersionUID = 1L;

    public StringConcatIterator(
            RuntimeIterator left,
            RuntimeIterator right,
            ExecutionMode executionMode,
            ExceptionMetadata iteratorMetadata
    ) {
        super(left, right, OperationalExpressionBase.Operator.CONCAT, executionMode, iteratorMetadata);
    }

    @Override
    public Item next() {
        if (this.hasNext()) {
            this._leftIterator.open(this._currentDynamicContextForLocalExecution);
            this._rightIterator.open(this._currentDynamicContextForLocalExecution);
            Item left;
            Item right;
            // empty sequences are treated as empty strings in concatenation
            if (this._leftIterator.hasNext()) {
                left = this._leftIterator.next();
            } else {
                left = ItemFactory.getInstance().createStringItem("");
            }
            if (this._rightIterator.hasNext()) {
                right = this._rightIterator.next();
            } else {
                right = ItemFactory.getInstance().createStringItem("");
            }
            if (!(left.isAtomic()) || !(right.isAtomic()))
                throw new UnexpectedTypeException(
                        "String concat expression has arguments that can't be converted to a string "
                            +
                            left.serialize()
                            + ", "
                            + right.serialize(),
                        getMetadata()
                );

            String leftStringValue = left.serialize();
            String rightStringValue = right.serialize();

            this._leftIterator.close();
            this._rightIterator.close();
            this._hasNext = false;
            return ItemFactory.getInstance().createStringItem(leftStringValue.concat(rightStringValue));
        }
        throw new IteratorFlowException(RuntimeIterator.FLOW_EXCEPTION_MESSAGE, getMetadata());

    }
}
