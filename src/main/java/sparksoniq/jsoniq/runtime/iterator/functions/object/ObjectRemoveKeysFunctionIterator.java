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

package sparksoniq.jsoniq.runtime.iterator.functions.object;

import org.rumbledb.api.Item;
import org.rumbledb.exceptions.ExceptionMetadata;
import org.rumbledb.exceptions.InvalidSelectorException;
import org.rumbledb.exceptions.IteratorFlowException;
import org.rumbledb.exceptions.UnexpectedTypeException;
import sparksoniq.jsoniq.ExecutionMode;
import sparksoniq.jsoniq.item.ItemFactory;
import sparksoniq.jsoniq.runtime.iterator.RuntimeIterator;
import sparksoniq.jsoniq.runtime.iterator.functions.base.LocalFunctionCallIterator;
import sparksoniq.semantics.DynamicContext;

import java.util.ArrayList;
import java.util.List;

public class ObjectRemoveKeysFunctionIterator extends LocalFunctionCallIterator {


    private static final long serialVersionUID = 1L;
    private RuntimeIterator _iterator;
    private Item _nextResult;
    private List<String> _removalKeys;

    public ObjectRemoveKeysFunctionIterator(
            List<RuntimeIterator> arguments,
            ExecutionMode executionMode,
            ExceptionMetadata iteratorMetadata
    ) {
        super(arguments, executionMode, iteratorMetadata);
    }

    @Override
    public void open(DynamicContext context) {
        super.open(context);

        this._iterator = this._children.get(0);
        this._iterator.open(context);

        List<Item> removalKeys = this._children.get(1).materialize(this._currentDynamicContextForLocalExecution);
        if (removalKeys.isEmpty()) {
            throw new InvalidSelectorException(
                    "Invalid Key Removal Parameter; Object key removal can't be performed with zero keys: ",
                    getMetadata()
            );
        }
        this._removalKeys = new ArrayList<>();
        for (Item removalKeyItem : removalKeys) {
            if (!removalKeyItem.isString()) {
                throw new UnexpectedTypeException("Remove-keys function has non-string key args.", getMetadata());
            }
            String removalKey = removalKeyItem.getStringValue();
            this._removalKeys.add(removalKey);
        }

        setNextResult();
    }

    @Override
    public Item next() {
        if (this._hasNext) {
            Item result = this._nextResult; // save the result to be returned
            setNextResult(); // calculate and store the next result
            return result;
        }
        throw new IteratorFlowException(
                RuntimeIterator.FLOW_EXCEPTION_MESSAGE + " REMOVE-KEYS function",
                getMetadata()
        );
    }

    public void setNextResult() {
        this._nextResult = null;

        if (this._iterator.hasNext()) {
            Item item = this._iterator.next();
            if (item.isObject()) {
                this._nextResult = removeKeys(item, this._removalKeys);
            } else {
                this._nextResult = item;
            }
        }

        if (this._nextResult == null) {
            this._hasNext = false;
            this._iterator.close();
        } else {
            this._hasNext = true;
        }
    }

    private Item removeKeys(Item objItem, List<String> removalKeys) {
        ArrayList<String> finalKeylist = new ArrayList<>();
        ArrayList<Item> finalValueList = new ArrayList<>();

        for (String objectKey : objItem.getKeys()) {
            if (!removalKeys.contains(objectKey)) {
                finalKeylist.add(objectKey);
                finalValueList.add(objItem.getItemByKey(objectKey));
            }
        }
        return ItemFactory.getInstance()
            .createObjectItem(finalKeylist, finalValueList, getMetadata());
    }
}
