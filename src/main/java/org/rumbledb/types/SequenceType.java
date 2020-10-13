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

import org.rumbledb.exceptions.OurBadException;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class SequenceType implements Serializable {

    private static final long serialVersionUID = 1L;
    private Arity arity;
    private ItemType itemType;
    private boolean isEmptySequence = false;

    public final static SequenceType MOST_GENERAL_SEQUENCE_TYPE = new SequenceType(
            ItemType.item,
            Arity.ZeroOrMore
    );

    public final static SequenceType EMPTY_SEQUENCE = new SequenceType();


    public SequenceType(ItemType itemType, Arity arity) {
        this.itemType = itemType;
        this.arity = arity;
    }

    public SequenceType(ItemType itemType) {
        this.itemType = itemType;
        this.arity = Arity.One;
    }

    private SequenceType() {
        this.itemType = null;
        this.arity = null;
        this.isEmptySequence = true;
    }

    public boolean isEmptySequence() {
        return this.isEmptySequence;
    }

    public ItemType getItemType() {
        if (this.isEmptySequence) {
            throw new OurBadException("Empty sequence type has no item");
        }
        return this.itemType;
    }

    public Arity getArity() {
        if (this.isEmptySequence) {
            throw new OurBadException("Empty sequence type has no arity");
        }
        return this.arity;
    }

    public boolean isSubtypeOf(SequenceType superType) {
        // TODO: arity check because of possible error
        if (this.isEmptySequence) {
            return superType.arity == Arity.OneOrZero || superType.arity == Arity.ZeroOrMore;
        }
        return this.itemType.isSubtypeOf(superType.getItemType())
            &&
            this.arity == superType.arity;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof SequenceType)) {
            return false;
        }
        SequenceType sequenceType = (SequenceType) other;
        if (this.isEmptySequence) {
            return sequenceType.isEmptySequence();
        }
        if (sequenceType.isEmptySequence()) {
            return false;
        }
        return this.getItemType().equals(sequenceType.getItemType()) && this.getArity().equals(sequenceType.getArity());
    }

    public enum Arity {
        OneOrZero {
            @Override
            public String getSymbol() {
                return "?";
            }
        },
        OneOrMore {
            @Override
            public String getSymbol() {
                return "+";
            }
        },
        ZeroOrMore {
            @Override
            public String getSymbol() {
                return "*";
            }
        },
        One {
            @Override
            public String getSymbol() {
                return "";
            }
        };

        public abstract String getSymbol();
    }

    @Override
    public String toString() {
        if (this.isEmptySequence) {
            return "()";
        }
        StringBuilder result = new StringBuilder();
        result.append(this.getItemType().toString());
        result.append(this.arity.getSymbol());
        return result.toString();
    }

    private static final Map<String, SequenceType> sequenceTypes;

    static {
        sequenceTypes = new HashMap<>();
        sequenceTypes.put("item", new SequenceType(ItemType.item, SequenceType.Arity.One));
        sequenceTypes.put("item?", new SequenceType(ItemType.item, SequenceType.Arity.OneOrZero));
        sequenceTypes.put("item*", new SequenceType(ItemType.item, SequenceType.Arity.ZeroOrMore));
        sequenceTypes.put("item+", new SequenceType(ItemType.item, SequenceType.Arity.OneOrMore));

        sequenceTypes.put("object", new SequenceType(ItemType.objectItem, SequenceType.Arity.One));
        sequenceTypes.put("object+", new SequenceType(ItemType.objectItem, SequenceType.Arity.OneOrMore));
        sequenceTypes.put("object*", new SequenceType(ItemType.objectItem, SequenceType.Arity.ZeroOrMore));

        sequenceTypes.put("array?", new SequenceType(ItemType.arrayItem, SequenceType.Arity.OneOrZero));

        sequenceTypes.put("atomic", new SequenceType(ItemType.atomicItem, SequenceType.Arity.One));
        sequenceTypes.put("atomic?", new SequenceType(ItemType.atomicItem, SequenceType.Arity.OneOrZero));
        sequenceTypes.put("atomic*", new SequenceType(ItemType.atomicItem, SequenceType.Arity.ZeroOrMore));

        sequenceTypes.put("string", new SequenceType(ItemType.stringItem, SequenceType.Arity.One));
        sequenceTypes.put("string?", new SequenceType(ItemType.stringItem, SequenceType.Arity.OneOrZero));
        sequenceTypes.put("string*", new SequenceType(ItemType.stringItem, SequenceType.Arity.ZeroOrMore));

        sequenceTypes.put("integer", new SequenceType(ItemType.integerItem, SequenceType.Arity.One));
        sequenceTypes.put("integer?", new SequenceType(ItemType.integerItem, SequenceType.Arity.OneOrZero));
        sequenceTypes.put("integer*", new SequenceType(ItemType.integerItem, SequenceType.Arity.ZeroOrMore));

        sequenceTypes.put("decimal?", new SequenceType(ItemType.decimalItem, SequenceType.Arity.OneOrZero));

        sequenceTypes.put("double", new SequenceType(ItemType.doubleItem, SequenceType.Arity.One));
        sequenceTypes.put("double?", new SequenceType(ItemType.doubleItem, SequenceType.Arity.OneOrZero));

        sequenceTypes.put("boolean", new SequenceType(ItemType.booleanItem, SequenceType.Arity.One));
        sequenceTypes.put("boolean?", new SequenceType(ItemType.booleanItem, SequenceType.Arity.OneOrZero));

        sequenceTypes.put("duration?", new SequenceType(ItemType.durationItem, SequenceType.Arity.OneOrZero));

        sequenceTypes.put(
            "yearMonthDuration?",
            new SequenceType(ItemType.yearMonthDurationItem, SequenceType.Arity.OneOrZero)
        );

        sequenceTypes.put(
            "dayTimeDuration?",
            new SequenceType(ItemType.dayTimeDurationItem, SequenceType.Arity.OneOrZero)
        );

        sequenceTypes.put("dateTime?", new SequenceType(ItemType.dateTimeItem, SequenceType.Arity.OneOrZero));

        sequenceTypes.put("date?", new SequenceType(ItemType.dateItem, SequenceType.Arity.OneOrZero));

        sequenceTypes.put("time?", new SequenceType(ItemType.timeItem, SequenceType.Arity.OneOrZero));

        sequenceTypes.put("anyURI?", new SequenceType(ItemType.anyURIItem, SequenceType.Arity.OneOrZero));

        sequenceTypes.put("hexBinary?", new SequenceType(ItemType.hexBinaryItem, SequenceType.Arity.OneOrZero));

        sequenceTypes.put(
            "base64Binary?",
            new SequenceType(ItemType.base64BinaryItem, SequenceType.Arity.OneOrZero)
        );

        sequenceTypes.put("null?", new SequenceType(ItemType.nullItem, SequenceType.Arity.OneOrZero));
    }

    public static SequenceType createSequenceType(String userFriendlyName) {
        if (sequenceTypes.containsKey(userFriendlyName)) {
            return sequenceTypes.get(userFriendlyName);
        }
        throw new OurBadException("Unrecognized type: " + userFriendlyName);
    }



}
