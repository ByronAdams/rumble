package org.rumbledb.types;

import java.io.Serializable;

public class AtomicItemType extends ItemType implements Serializable {

    private static final long serialVersionUID = 1L;

    // TODO: extract array and object into its own types
    public static final AtomicItemType atomicItem = new AtomicItemType("atomic");
    public static final AtomicItemType stringItem = new AtomicItemType("string");
    public static final AtomicItemType integerItem = new AtomicItemType("integer");
    public static final AtomicItemType decimalItem = new AtomicItemType("decimal");
    public static final AtomicItemType doubleItem = new AtomicItemType("double");
    public static final AtomicItemType booleanItem = new AtomicItemType("boolean");
    public static final AtomicItemType nullItem = new AtomicItemType("null");
    public static final AtomicItemType durationItem = new AtomicItemType("duration");
    public static final AtomicItemType yearMonthDurationItem = new AtomicItemType("yearMonthDuration");
    public static final AtomicItemType dayTimeDurationItem = new AtomicItemType("dayTimeDuration");
    public static final AtomicItemType dateTimeItem = new AtomicItemType("dateTime");
    public static final AtomicItemType dateItem = new AtomicItemType("date");
    public static final AtomicItemType timeItem = new AtomicItemType("time");
    public static final AtomicItemType hexBinaryItem = new AtomicItemType("hexBinary");
    public static final AtomicItemType anyURIItem = new AtomicItemType("anyURI");
    public static final AtomicItemType base64BinaryItem = new AtomicItemType("base64Binary");
    public static final AtomicItemType JSONItem = new AtomicItemType("json-item");
    public static final AtomicItemType objectItem = new AtomicItemType("object");
    public static final AtomicItemType arrayItem = new AtomicItemType("array");

    public AtomicItemType() {
    }

    private AtomicItemType(String name) {
        super(name);
    }

    // Returns true if [this] is a subtype of [superType], any type is considered a subtype of itself
    @Override
    public boolean isSubtypeOf(ItemType superType) {
        if (superType.equals(ItemType.item)) {
            return true;
        } else if (superType.equals(JSONItem)) {
            return this.equals(objectItem)
                || this.equals(arrayItem)
                || this.equals(JSONItem);
        } else if (superType.equals(atomicItem)) {
            return this.equals(stringItem)
                || this.equals(integerItem)
                || this.equals(decimalItem)
                || this.equals(doubleItem)
                || this.equals(booleanItem)
                || this.equals(nullItem)
                || this.equals(anyURIItem)
                || this.equals(hexBinaryItem)
                || this.equals(base64BinaryItem)
                || this.equals(dateTimeItem)
                || this.equals(dateItem)
                || this.equals(timeItem)
                || this.equals(durationItem)
                || this.equals(yearMonthDurationItem)
                || this.equals(dayTimeDurationItem)
                || this.equals(atomicItem);
        } else if (superType.equals(durationItem)) {
            return this.equals(yearMonthDurationItem)
                || this.equals(dayTimeDurationItem)
                || this.equals(durationItem);
        } else if (superType.equals(decimalItem)) {
            return this.equals(integerItem) || this.equals(decimalItem);
        }
        return this.equals(superType);
    }

    @Override
    public ItemType findCommonSuperType(ItemType other) {
        if (other.isSubtypeOf(this)) {
            return this;
        } else if (this.isSubtypeOf(other)) {
            return other;
        } else if (this.isSubtypeOf(durationItem) && other.isSubtypeOf(durationItem)) {
            return durationItem;
        } else if (this.isSubtypeOf(atomicItem) && other.isSubtypeOf(atomicItem)) {
            return atomicItem;
        } else if (this.isSubtypeOf(JSONItem) && other.isSubtypeOf(JSONItem)) {
            return JSONItem;
        } else {
            return ItemType.item;
        }
    }

    @Override
    public boolean staticallyCastableAs(ItemType other) {
        // anything can be casted to itself
        if (this.equals(other))
            return true;
        // anything can be casted from and to a string (or from one of its supertype)
        if (this.equals(stringItem) || other.equals(stringItem))
            return true;
        // boolean and numeric can be cast between themselves
        if (
            this.equals(booleanItem) || this.equals(integerItem) || this.equals(doubleItem) || this.equals(decimalItem)
        ) {
            if (
                other.equals(integerItem)
                    ||
                    other.equals(doubleItem)
                    ||
                    other.equals(decimalItem)
                    ||
                    other.equals(booleanItem)
            )
                return true;
            else
                return false;
        }
        // base64 and hex can be cast between themselves
        if (this.equals(base64BinaryItem) || this.equals(hexBinaryItem)) {
            if (
                other.equals(base64BinaryItem)
                    ||
                    other.equals(hexBinaryItem)
            )
                return true;
            else
                return false;
        }
        // durations can be cast between themselves
        if (this.isSubtypeOf(durationItem)) {
            if (other.isSubtypeOf(durationItem))
                return true;
            else
                return false;
        }
        // DateTime can be cast also to Date or Time
        if (this.equals(dateTimeItem)) {
            if (other.equals(dateItem) || other.equals(timeItem))
                return true;
            else
                return false;
        }
        // Date can be cast also to DateTime
        if (this.equals(dateItem)) {
            if (other.equals(dateTimeItem))
                return true;
            else
                return false;
        }
        // Otherwise this cannot be casted to other
        return false;
    }

    @Override
    public boolean isNumeric() {
        return this.equals(integerItem) || this.equals(decimalItem) || this.equals(doubleItem);
    }

    @Override
    public boolean canBePromotedToString() {
        return this.equals(stringItem) || this.equals(anyURIItem);
    }
}
