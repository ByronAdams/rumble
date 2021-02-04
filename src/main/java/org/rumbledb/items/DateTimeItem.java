package org.rumbledb.items;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.DateTimeParser;
import org.joda.time.format.ISODateTimeFormat;
import org.rumbledb.api.Item;
import org.rumbledb.exceptions.ExceptionMetadata;
import org.rumbledb.exceptions.IteratorFlowException;
import org.rumbledb.exceptions.UnexpectedTypeException;
import org.rumbledb.expressions.comparison.ComparisonExpression;
import org.rumbledb.runtime.flwor.NativeClauseContext;
import org.rumbledb.types.AtomicItemType;
import org.rumbledb.types.ItemType;

import java.util.regex.Pattern;

import static org.joda.time.format.ISODateTimeFormat.dateElementParser;

public class DateTimeItem extends AtomicItem {

    private static final String yearFrag = "((-)?(([1-9]\\d\\d(\\d)+)|(0\\d\\d\\d)))";
    private static final String monthFrag = "((0[1-9])|(1[0-2]))";
    private static final String dayFrag = "((0[1-9])|([1-2]\\d)|(3[0-1]))";
    private static final String hourFrag = "(([0-1]\\d)|(2[0-3]))";
    private static final String minuteFrag = "([0-5]\\d)";
    private static final String secondFrag = "(([0-5]\\d)(\\.(\\d)+)?)";
    private static final String endOfDayFrag = "(24:00:00(\\.(0)+)?)";
    private static final String timezoneFrag = "(Z|([+\\-])(((0\\d|1[0-3]):" + minuteFrag + ")|(14:00)))";
    private static final String dateFrag = "(" + yearFrag + '-' + monthFrag + '-' + dayFrag + ")";
    private static final String timeFrag = "(("
        + hourFrag
        + ":"
        + minuteFrag
        + ":"
        + secondFrag
        + ")|("
        + endOfDayFrag
        + "))";

    private static final String dateTimeLexicalRep = dateFrag + "T" + timeFrag + "(" + timezoneFrag + ")?";
    private static final String dateLexicalRep = "(" + dateFrag + "(" + timezoneFrag + ")?)";
    private static final String timeLexicalRep = "(" + timeFrag + "(" + timezoneFrag + ")?)";

    private static final Pattern dateTimePattern = Pattern.compile(dateTimeLexicalRep);
    private static final Pattern datePattern = Pattern.compile(dateLexicalRep);
    private static final Pattern timePattern = Pattern.compile(timeLexicalRep);


    private static final long serialVersionUID = 1L;
    private DateTime value;
    private boolean hasTimeZone = true;

    public DateTimeItem() {
        super();
    }

    DateTimeItem(DateTime value, boolean hasTimeZone) {
        super();
        this.value = value;
        this.hasTimeZone = hasTimeZone;
    }

    DateTimeItem(String dateTimeString) {
        this.value = parseDateTime(dateTimeString, AtomicItemType.dateTimeItem);
        if (!dateTimeString.endsWith("Z") && this.value.getZone() == DateTimeZone.getDefault()) {
            this.hasTimeZone = false;
            this.value = this.value.withZoneRetainFields(DateTimeZone.UTC);
        }
    }

    public DateTime getValue() {
        return this.value;
    }

    @Override
    public DateTime getDateTimeValue() {
        return this.getValue();
    }

    @Override
    public boolean isAtomic() {
        return true;
    }

    @Override
    public boolean hasTimeZone() {
        return this.hasTimeZone;
    }

    @Override
    public boolean isDateTime() {
        return true;
    }

    @Override
    public boolean hasDateTime() {
        return true;
    }

    @Override
    public Item castAs(ItemType itemType) {
        if (itemType.equals(AtomicItemType.stringItem)) {
            return ItemFactory.getInstance().createStringItem(this.serialize());
        }
        if (itemType.equals(AtomicItemType.dateTimeItem)) {
            return this;
        }
        if (itemType.equals(AtomicItemType.dateItem)) {
            return ItemFactory.getInstance().createDateItem(this.getDateTimeValue(), this.hasTimeZone);
        }
        if (itemType.equals(AtomicItemType.timeItem)) {
            return ItemFactory.getInstance().createTimeItem(this.getDateTimeValue(), this.hasTimeZone);
        }
        throw new ClassCastException();
    }

    @Override
    public boolean isCastableAs(ItemType itemType) {
        return itemType.equals(AtomicItemType.dateTimeItem)
            || itemType.equals(AtomicItemType.dateItem)
            || itemType.equals(AtomicItemType.timeItem)
            || itemType.equals(AtomicItemType.stringItem);
    }

    @Override
    public boolean getEffectiveBooleanValue() {
        return false;
    }

    @Override
    public boolean equals(Object otherObject) {
        if (!(otherObject instanceof Item)) {
            return false;
        }
        Item otherItem = (Item) otherObject;
        if (otherItem.isDateTime()) {
            return this.getValue().isEqual(otherItem.getDateTimeValue());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.getValue().hashCode();
    }

    @Override
    public boolean isTypeOf(ItemType type) {
        return type.equals(AtomicItemType.dateTimeItem) || super.isTypeOf(type);
    }

    @Override
    public String serialize() {
        String value = this.getValue().toString();
        String zoneString = this.getValue().getZone() == DateTimeZone.UTC
            ? "Z"
            : this.getValue().getZone().toString().equals(DateTimeZone.getDefault().toString())
                ? ""
                : value.substring(value.length() - 6);
        value = value.substring(0, value.length() - zoneString.length());
        value = this.getValue().getMillisOfSecond() == 0 ? value.substring(0, value.length() - 4) : value;
        return value + (this.hasTimeZone ? zoneString : "");
    }

    @Override
    public void write(Kryo kryo, Output output) {
        output.writeLong(this.getDateTimeValue().getMillis(), true);
        output.writeBoolean(this.hasTimeZone);
        output.writeString(this.getDateTimeValue().getZone().getID());
    }

    @Override
    public void read(Kryo kryo, Input input) {
        Long millis = input.readLong(true);
        this.hasTimeZone = input.readBoolean();
        DateTimeZone zone = DateTimeZone.forID(input.readString());
        this.value = new DateTime(millis, zone);
    }

    private static DateTimeFormatter getDateTimeFormatter(ItemType dateTimeType) {
        if (dateTimeType.equals(AtomicItemType.dateTimeItem)) {
            return ISODateTimeFormat.dateTimeParser().withOffsetParsed();
        }
        if (dateTimeType.equals(AtomicItemType.dateItem)) {
            DateTimeParser dtParser = new DateTimeFormatterBuilder().appendOptional(
                ((new DateTimeFormatterBuilder()).appendTimeZoneOffset("Z", true, 2, 4).toFormatter()).getParser()
            ).toParser();
            return (new DateTimeFormatterBuilder()).append(dateElementParser())
                .appendOptional(dtParser)
                .toFormatter()
                .withOffsetParsed();
        }
        if (dateTimeType.equals(AtomicItemType.timeItem)) {
            return ISODateTimeFormat.timeParser().withOffsetParsed();
        }
        throw new IllegalArgumentException();
    }

    private static boolean checkInvalidDateTimeFormat(String dateTime, ItemType dateTimeType) {
        if (dateTimeType.equals(AtomicItemType.dateTimeItem)) {
            return dateTimePattern.matcher(dateTime).matches();
        }
        if (dateTimeType.equals(AtomicItemType.dateItem)) {
            return datePattern.matcher(dateTime).matches();
        }
        if (dateTimeType.equals(AtomicItemType.timeItem)) {
            return timePattern.matcher(dateTime).matches();
        }
        return false;
    }

    private static String fixEndOfDay(String dateTime) {
        String endOfDay = "24:00:00";
        String startOfDay = "00:00:00";
        if (dateTime.contains(endOfDay)) {
            if (dateTime.indexOf(endOfDay) == 0) {
                return startOfDay;
            }
            int indexOfT = dateTime.indexOf('T');
            if (
                indexOfT < 1
                    || indexOfT != dateTime.indexOf(endOfDay) - 1
                    || !Character.isDigit(dateTime.charAt(indexOfT - 1))
            ) {
                throw new IllegalArgumentException();
            }
            int dayValue;
            try {
                dayValue = Character.getNumericValue(dateTime.charAt(indexOfT - 1));
            } catch (Exception e) {
                throw new IllegalArgumentException();
            }
            return dateTime.substring(0, indexOfT - 1)
                +
                (dayValue + 1)
                + "T"
                + startOfDay
                +
                dateTime.substring(indexOfT + endOfDay.length() + 1);
        }
        return dateTime;
    }

    static DateTime parseDateTime(String dateTime, ItemType dateTimeType) throws IllegalArgumentException {
        if (!checkInvalidDateTimeFormat(dateTime, dateTimeType)) {
            throw new IllegalArgumentException();
        }
        dateTime = fixEndOfDay(dateTime);
        return DateTime.parse(dateTime, getDateTimeFormatter(dateTimeType));
    }

    @Override
    public Item add(Item other) {
        if (other.isYearMonthDuration() || other.isDayTimeDuration()) {
            return ItemFactory.getInstance()
                .createDateTimeItem(this.getValue().plus(other.getDurationValue()), this.hasTimeZone);
        } else {
            throw new ClassCastException();
        }
    }

    @Override
    public Item subtract(Item other) {
        if (other.isDateTime()) {
            return ItemFactory.getInstance()
                .createDayTimeDurationItem(new Period(other.getDateTimeValue(), this.getValue(), PeriodType.dayTime()));
        }
        if (other.isYearMonthDuration() || other.isDayTimeDuration()) {
            return ItemFactory.getInstance()
                .createDateTimeItem(this.getValue().minus(other.getDurationValue()), this.hasTimeZone);
        } else {
            throw new ClassCastException();
        }
    }

    @Override
    public int compareTo(Item other) {
        if (other.isNull()) {
            return 1;
        }
        if (other.isDateTime()) {
            return this.getValue().compareTo(other.getDateTimeValue());
        }
        throw new IteratorFlowException(
                "Cannot compare item of type "
                    + this.getDynamicType().toString()
                    +
                    " with item of type "
                    + other.getDynamicType().toString()
        );
    }

    @Override
    public Item compareItem(
            Item other,
            ComparisonExpression.ComparisonOperator comparisonOperator,
            ExceptionMetadata metadata
    ) {
        if (!other.isDateTime() && !other.isNull()) {
            throw new UnexpectedTypeException(
                    "\""
                        + this.getDynamicType().toString()
                        + "\": invalid type: can not compare for equality to type \""
                        + other.getDynamicType().toString()
                        + "\"",
                    metadata
            );
        }
        return super.compareItem(other, comparisonOperator, metadata);
    }

    @Override
    public ItemType getDynamicType() {
        return AtomicItemType.dateTimeItem;
    }

    @Override
    public NativeClauseContext generateNativeQuery(NativeClauseContext context) {
        return NativeClauseContext.NoNativeQuery;
    }
}
