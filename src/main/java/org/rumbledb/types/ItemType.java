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

package org.rumbledb.types;


import org.rumbledb.context.Name;
import org.rumbledb.exceptions.OurBadException;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public class ItemType implements Serializable {

    private static final long serialVersionUID = 1L;
    private String name;

    public static final ItemType objectItem = new ItemType("object");
    public static final ItemType atomicItem = new ItemType("atomic");
    public static final ItemType stringItem = new ItemType("string");
    public static final ItemType integerItem = new ItemType("integer");
    public static final ItemType intItem = new ItemType("int");
    public static final ItemType decimalItem = new ItemType("decimal");
    public static final ItemType doubleItem = new ItemType("double");
    public static final ItemType floatItem = new ItemType("float");
    public static final ItemType booleanItem = new ItemType("boolean");
    public static final ItemType arrayItem = new ItemType("array");
    public static final ItemType nullItem = new ItemType("null");
    public static final ItemType JSONItem = new ItemType("json-item");
    public static final ItemType durationItem = new ItemType("duration");
    public static final ItemType yearMonthDurationItem = new ItemType("yearMonthDuration");
    public static final ItemType dayTimeDurationItem = new ItemType("dayTimeDuration");
    public static final ItemType dateTimeItem = new ItemType("dateTime");
    public static final ItemType dateItem = new ItemType("date");
    public static final ItemType timeItem = new ItemType("time");
    public static final ItemType hexBinaryItem = new ItemType("hexBinary");
    public static final ItemType anyURIItem = new ItemType("anyURI");
    public static final ItemType base64BinaryItem = new ItemType("base64Binary");
    public static final ItemType item = new ItemType("item");
    public static final ItemType functionItem = new ItemType("function");

    private static List<ItemType> builtInItemTypes = Arrays.asList(
        objectItem,
        atomicItem,
        stringItem,
        integerItem,
        intItem,
        decimalItem,
        doubleItem,
        floatItem,
        booleanItem,
        arrayItem,
        nullItem,
        JSONItem,
        durationItem,
        yearMonthDurationItem,
        dayTimeDurationItem,
        dateTimeItem,
        dateItem,
        timeItem,
        hexBinaryItem,
        anyURIItem,
        base64BinaryItem,
        item,
        functionItem
    );


    public ItemType() {
    }

    private ItemType(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public static boolean typeExists(Name name) {
        for (int i = 0; i < builtInItemTypes.size(); ++i) {
            if (builtInItemTypes.get(i).getName().equals(name.getLocalName())) {
                return true;
            } ;
        }
        return false;
    }

    public static ItemType getItemTypeByName(String name) {
        for (int i = 0; i < builtInItemTypes.size(); ++i) {
            if (builtInItemTypes.get(i).getName().equals(name)) {
                return builtInItemTypes.get(i);
            } ;
        }
        throw new OurBadException("Type unrecognized: " + name);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ItemType)) {
            return false;
        }
        return this.name.equals(((ItemType) other).getName());
    }


    public boolean isSubtypeOf(ItemType superType) {
        if (superType.equals(item)) {
            return true;
        }
        if (superType.equals(this)) {
            return true;
        }
        if (superType.equals(decimalItem)) {
            return this.equals(decimalItem)
                || this.equals(integerItem);
        }
        if (superType.equals(JSONItem)) {
            return this.equals(objectItem)
                || this.equals(arrayItem)
                || this.equals(JSONItem);
        }

        if (superType.equals(atomicItem)) {
            return this.equals(stringItem)
                || this.equals(integerItem)
                || this.equals(decimalItem)
                || this.equals(doubleItem)
                || this.equals(booleanItem)
                || this.equals(anyURIItem);
        }

        return false;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
