package org.rumbledb.expressions.flowr;

import org.rumbledb.exceptions.ExceptionMetadata;
import org.rumbledb.expressions.Expression;
import org.rumbledb.expressions.Node;
import sparksoniq.semantics.types.AtomicTypes;
import sparksoniq.semantics.types.SingleType;

import java.util.ArrayList;
import java.util.List;

public class FlworVarSingleType extends Expression {

    private SingleType _singleType;
    private boolean isEmpty;

    public FlworVarSingleType(ExceptionMetadata metadata) {
        super(metadata);
        this._singleType = new SingleType(AtomicTypes.AtomicItem);
        this.isEmpty = true;
    }

    public FlworVarSingleType(AtomicTypes atomicType, ExceptionMetadata metadata) {
        super(metadata);
        this._singleType = new SingleType(atomicType);
    }

    public FlworVarSingleType(AtomicTypes atomicType, boolean zeroOrOne, ExceptionMetadata metadata) {
        super(metadata);
        this._singleType = new SingleType(atomicType, zeroOrOne);
        this.isEmpty = false;

    }

    public static AtomicTypes getAtomicType(String text) {
        text = text.toLowerCase();
        switch (text) {
            case "string":
                return AtomicTypes.StringItem;
            case "integer":
                return AtomicTypes.IntegerItem;
            case "decimal":
                return AtomicTypes.DecimalItem;
            case "double":
                return AtomicTypes.DoubleItem;
            case "boolean":
                return AtomicTypes.BooleanItem;
            case "null":
                return AtomicTypes.NullItem;
            case "duration":
                return AtomicTypes.DurationItem;
            case "yearmonthduration":
                return AtomicTypes.YearMonthDurationItem;
            case "daytimeduration":
                return AtomicTypes.DayTimeDurationItem;
            case "datetime":
                return AtomicTypes.DateTimeItem;
            case "date":
                return AtomicTypes.DateItem;
            case "time":
                return AtomicTypes.TimeItem;
            case "hexbinary":
                return AtomicTypes.HexBinaryItem;
            case "base64binary":
                return AtomicTypes.Base64BinaryItem;

            default:
                return AtomicTypes.AtomicItem;
        }
    }

    public boolean isEmpty() {
        return this.isEmpty;
    }

    public SingleType getSingleType() {
        return this._singleType;
    }

    @Override
    public List<Node> getDescendants(boolean depthSearch) {
        return new ArrayList<>();
    }

    @Override
    public String serializationString(boolean prefix) {
        String result = "(atomicType ";
        result += "(itemType ";
        result += getSerializationOfAtomicType(this._singleType.getType());
        result += "))";
        return result;
    }

    private String getSerializationOfAtomicType(AtomicTypes item) {
        switch (item) {
            case IntegerItem:
                return "(atomicType integer)";
            case DecimalItem:
                return "(atomicType decimal)";
            case DoubleItem:
                return "(atomicType double)";
            case StringItem:
                return "(atomicType string)";
            case BooleanItem:
                return "(atomicType boolean)";
            case NullItem:
                return "(atomicType null)";
            case DurationItem:
                return "(atomicType duration)";
            default:
                return "item";
        }
    }
}
