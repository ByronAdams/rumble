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

package org.rumbledb.expressions.flowr;


import org.rumbledb.exceptions.ExceptionMetadata;
import org.rumbledb.expressions.Expression;
import org.rumbledb.expressions.Node;
import sparksoniq.semantics.types.ItemType;
import sparksoniq.semantics.types.ItemTypes;
import sparksoniq.semantics.types.SequenceType;

import java.util.ArrayList;
import java.util.List;

public class FlworVarSequenceType extends Expression {

    private SequenceType _sequence;
    private boolean isEmpty = false;

    public FlworVarSequenceType(ExceptionMetadata metadata) {
        super(metadata);
        this._sequence = new SequenceType();
        this.isEmpty = true;
    }

    public FlworVarSequenceType(ItemTypes item, SequenceType.Arity arity, ExceptionMetadata metadata) {
        super(metadata);
        this._sequence = new SequenceType(new ItemType(item), arity);
    }

    public FlworVarSequenceType(ItemTypes item, ExceptionMetadata metadata) {
        super(metadata);
        this._sequence = new SequenceType(new ItemType(item), SequenceType.Arity.One);
    }

    public FlworVarSequenceType(SequenceType sequenceType, ExceptionMetadata metadata) {
        super(metadata);
        this._sequence = sequenceType;
    }

    public static ItemTypes getItemType(String text) {
        text = text.toLowerCase();
        switch (text) {
            case "object":
                return ItemTypes.ObjectItem;
            case "atomic":
                return ItemTypes.AtomicItem;
            case "string":
                return ItemTypes.StringItem;
            case "integer":
                return ItemTypes.IntegerItem;
            case "decimal":
                return ItemTypes.DecimalItem;
            case "double":
                return ItemTypes.DoubleItem;
            case "boolean":
                return ItemTypes.BooleanItem;
            case "null":
                return ItemTypes.NullItem;
            case "array":
                return ItemTypes.ArrayItem;
            case "json-item":
                return ItemTypes.JSONItem;
            case "duration":
                return ItemTypes.DurationItem;
            case "yearmonthduration":
                return ItemTypes.YearMonthDurationItem;
            case "daytimeduration":
                return ItemTypes.DayTimeDurationItem;
            case "datetime":
                return ItemTypes.DateTimeItem;
            case "date":
                return ItemTypes.DateItem;
            case "time":
                return ItemTypes.TimeItem;
            case "hexbinary":
                return ItemTypes.HexBinaryItem;
            case "base64binary":
                return ItemTypes.Base64BinaryItem;

            default:
                return ItemTypes.Item;
        }
    }

    public boolean isEmpty() {
        return this.isEmpty;
    }

    public SequenceType getSequence() {
        return this._sequence;
    }

    @Override
    public List<Node> getDescendants(boolean depthSearch) {
        return new ArrayList<>();
    }

    @Override
    public String serializationString(boolean prefix) {
        String result = "(sequenceType ";
        if (this.isEmpty)
            return result + "( )";
        result += "(itemType ";
        result += getSerializationOfItemType(this._sequence.getItemType().getType());
        result += "))";
        return result;
    }

    private String getSerializationOfItemType(ItemTypes item) {
        switch (item) {
            case Item:
                return "item";
            case IntegerItem:
                return "(atomicType integer)";
            case DecimalItem:
                return "(atomicType decimal)";
            case DoubleItem:
                return "(atomicType double)";
            case AtomicItem:
                return "(atomicType atomic)";
            case StringItem:
                return "(atomicType string)";
            case BooleanItem:
                return "(atomicType boolean)";
            case NullItem:
                return "(atomicType null)";

            case JSONItem:
                return "(jSONItemTest json-item)";
            case ArrayItem:
                return "(jSONItemTest array)";
            case ObjectItem:
                return "(jSONItemTest object)";
            default:
                return "item";
        }
    }
}
