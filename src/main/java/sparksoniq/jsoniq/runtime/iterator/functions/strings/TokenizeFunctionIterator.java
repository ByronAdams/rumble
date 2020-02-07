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

package sparksoniq.jsoniq.runtime.iterator.functions.strings;

import org.rumbledb.api.Item;
import org.rumbledb.exceptions.ExceptionMetadata;
import org.rumbledb.exceptions.IteratorFlowException;
import org.rumbledb.exceptions.UnexpectedTypeException;
import sparksoniq.jsoniq.ExecutionMode;
import sparksoniq.jsoniq.item.ItemFactory;
import sparksoniq.jsoniq.runtime.iterator.RuntimeIterator;
import sparksoniq.jsoniq.runtime.iterator.functions.base.LocalFunctionCallIterator;
import sparksoniq.semantics.DynamicContext;

import java.util.List;

public class TokenizeFunctionIterator extends LocalFunctionCallIterator {

    private static final long serialVersionUID = 1L;
    private String[] _results;
    private Item _nextResult;
    private int _currentPosition;
    private boolean _lastEmptyString;

    public TokenizeFunctionIterator(
            List<RuntimeIterator> arguments,
            ExecutionMode executionMode,
            ExceptionMetadata iteratorMetadata
    ) {
        super(arguments, executionMode, iteratorMetadata);
    }

    @Override
    public Item next() {
        if (this._nextResult != null) {
            Item result = this._nextResult;
            setNextResult();
            return result;
        }
        throw new IteratorFlowException(FLOW_EXCEPTION_MESSAGE + "tokenize function", getMetadata());
    }

    @Override
    public void open(DynamicContext context) {
        super.open(context);
        this._results = null;
        this._currentPosition = -1;
        setNextResult();
    }

    public void setNextResult() {
        if (this._results == null) {
            // Getting first parameter
            RuntimeIterator stringIterator = this._children.get(0);
            stringIterator.open(this._currentDynamicContextForLocalExecution);
            if (!stringIterator.hasNext()) {
                this._hasNext = false;
                stringIterator.close();
                return;
            }
            String input = null;
            String separator = null;
            Item stringItem = stringIterator.next();
            if (stringIterator.hasNext())
                throw new UnexpectedTypeException(
                        "First parameter of tokenize must be a string or the empty sequence.",
                        getMetadata()
                );
            stringIterator.close();
            if (!stringItem.isString())
                throw new UnexpectedTypeException(
                        "First parameter of tokenize must be a string or the empty sequence.",
                        getMetadata()
                );
            try {
                input = stringItem.getStringValue();
                stringIterator.close();
            } catch (Exception e) {
                throw new UnexpectedTypeException(
                        "First parameter of tokenize must be a string or the empty sequence.",
                        getMetadata()
                );
            }

            // Getting second parameter
            if (this._children.size() == 1) {
                separator = "\\s+";
            } else {
                RuntimeIterator separatorIterator = this._children.get(1);
                separatorIterator.open(this._currentDynamicContextForLocalExecution);
                if (!separatorIterator.hasNext()) {
                    throw new UnexpectedTypeException("Second parameter of tokenize must be a string.", getMetadata());
                }
                stringItem = separatorIterator.next();
                if (separatorIterator.hasNext())
                    throw new UnexpectedTypeException("Second parameter of tokenize must be a string.", getMetadata());
                separatorIterator.close();
                if (!stringItem.isString())
                    throw new UnexpectedTypeException("Second parameter of tokenize must be a string.", getMetadata());
                try {
                    separator = stringItem.getStringValue();
                } catch (Exception e) {
                    throw new UnexpectedTypeException("Second parameter of tokenize must be a string.", getMetadata());
                }
            }
            this._results = input.split(separator);
            this._currentPosition = 0;
            if (this._children.size() == 1 && this._results.length != 0 && this._results[0].equals("")) {
                this._currentPosition++;
            }
            this._lastEmptyString = this._children.size() == 2 && input.matches(".*" + separator + "$");
        }
        if (this._currentPosition < this._results.length) {
            this._nextResult = ItemFactory.getInstance().createStringItem(this._results[this._currentPosition]);
            this._currentPosition++;
            this._hasNext = true;
        } else if (this._lastEmptyString) {
            this._nextResult = ItemFactory.getInstance().createStringItem(new String(""));
            this._hasNext = true;
            this._lastEmptyString = false;
        } else {
            this._hasNext = false;
        }
    }
}
