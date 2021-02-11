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

package org.rumbledb.items.parsing;

import org.apache.commons.codec.binary.Hex;
import org.apache.spark.ml.linalg.DenseVector;
import org.apache.spark.ml.linalg.SparseVector;
import org.apache.spark.ml.linalg.Vector;
import org.apache.spark.ml.linalg.VectorUDT;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.types.ArrayType;
import org.apache.spark.sql.types.DataType;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.DecimalType;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.joda.time.DateTime;
import org.rumbledb.api.Item;
import org.rumbledb.context.Name;
import org.rumbledb.exceptions.ExceptionMetadata;
import org.rumbledb.exceptions.MLInvalidDataFrameSchemaException;
import org.rumbledb.exceptions.OurBadException;
import org.rumbledb.exceptions.ParsingException;
import org.rumbledb.exceptions.RumbleException;
import org.rumbledb.items.ItemFactory;
import org.rumbledb.runtime.typing.CastIterator;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import org.rumbledb.types.BuiltinTypesCatalogue;
import org.rumbledb.types.ItemType;
import scala.collection.mutable.WrappedArray;
import sparksoniq.spark.SparkSessionManager;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ItemParser implements Serializable {


    private static final long serialVersionUID = 1L;
    private static final DataType vectorType = new VectorUDT();
    public static final DataType decimalType = new DecimalType(30, 15); // 30 and 15 are arbitrary

    public static Item getItemFromObject(JsonReader object, ExceptionMetadata metadata) {
        try {
            if (object.peek() == JsonToken.STRING) {
                return ItemFactory.getInstance().createStringItem(object.nextString());
            }
            if (object.peek() == JsonToken.NUMBER) {
                String number = object.nextString();
                if (number.contains("E") || number.contains("e")) {
                    return ItemFactory.getInstance().createDoubleItem(Double.parseDouble(number));
                }
                if (number.contains(".")) {
                    return ItemFactory.getInstance().createDecimalItem(new BigDecimal(number));
                }
                return ItemFactory.getInstance().createIntegerItem(number);
            }
            if (object.peek() == JsonToken.BOOLEAN) {
                return ItemFactory.getInstance().createBooleanItem(object.nextBoolean());
            }
            if (object.peek() == JsonToken.BEGIN_ARRAY) {
                List<Item> values = new ArrayList<>();
                object.beginArray();
                while (object.hasNext()) {
                    values.add(getItemFromObject(object, metadata));
                }
                object.endArray();
                return ItemFactory.getInstance().createArrayItem(values);
            }
            if (object.peek() == JsonToken.BEGIN_OBJECT) {
                List<String> keys = new ArrayList<>();
                List<Item> values = new ArrayList<>();
                object.beginObject();
                while (object.hasNext()) {
                    keys.add(object.nextName());
                    values.add(getItemFromObject(object, metadata));
                }
                object.endObject();
                return ItemFactory.getInstance()
                    .createObjectItem(keys, values, metadata);
            }
            if (object.peek() == JsonToken.NULL) {
                object.nextNull();
                return ItemFactory.getInstance().createNullItem();
            }
            throw new ParsingException("Invalid value found while parsing. JSON is not well-formed!", metadata);
        } catch (Exception e) {
            RumbleException r = new ParsingException(
                    "An error happened while parsing JSON. JSON is not well-formed! Hint: if you use json-file(), it must be in the JSON Lines format, with one value per line. If this is not the case, consider using json-doc().",
                    metadata
            );
            r.initCause(e);
            throw r;
        }
    }

    public static Item getItemFromRow(Row row, ExceptionMetadata metadata) {
        List<String> keys = new ArrayList<>();
        List<Item> values = new ArrayList<>();
        StructType schema = row.schema();
        StructField[] fields = schema.fields();
        String[] fieldnames = schema.fieldNames();

        for (int i = 0; i < fields.length; ++i) {
            StructField field = fields[i];
            DataType fieldType = field.dataType();
            keys.add(field.name());
            Item newItem = convertValueToItem(row, i, null, fieldType, metadata);
            values.add(newItem);
        }

        if (fields.length == 1 && fieldnames[0].equals(SparkSessionManager.atomicJSONiqItemColumnName)) {
            return values.get(0);
        }
        return ItemFactory.getInstance().createObjectItem(keys, values, metadata);
    }

    public static Item convertValueToItem(
            Object o,
            DataType fieldType,
            ExceptionMetadata metadata
    ) {
        return convertValueToItem(null, 0, o, fieldType, metadata);
    }

    public static ItemType convertDataTypeToItemType(DataType dt) {
        if (dt instanceof StructType) {
            return BuiltinTypesCatalogue.objectItem;
        }
        if (dt instanceof ArrayType) {
            return BuiltinTypesCatalogue.arrayItem;
        }
        if (dt.equals(DataTypes.StringType)) {
            return BuiltinTypesCatalogue.stringItem;
        } else if (dt.equals(DataTypes.BooleanType)) {
            return BuiltinTypesCatalogue.booleanItem;
        } else if (dt.equals(DataTypes.DoubleType)) {
            return BuiltinTypesCatalogue.doubleItem;
        } else if (dt.equals(DataTypes.IntegerType)) {
            return BuiltinTypesCatalogue.integerItem;
        } else if (dt.equals(DataTypes.FloatType)) {
            return BuiltinTypesCatalogue.floatItem;
        } else if (dt.equals(decimalType)) {
            return BuiltinTypesCatalogue.decimalItem;
        } else if (dt.equals(DataTypes.LongType)) {
            return BuiltinTypesCatalogue.integerItem;
        } else if (dt.equals(DataTypes.NullType)) {
            return BuiltinTypesCatalogue.nullItem;
        } else if (dt.equals(DataTypes.ShortType)) {
            return BuiltinTypesCatalogue.integerItem;
        } else if (dt.equals(DataTypes.TimestampType)) {
            return BuiltinTypesCatalogue.dateTimeItem;
        } else if (dt.equals(DataTypes.DateType)) {
            return BuiltinTypesCatalogue.dateItem;
        } else if (dt.equals(DataTypes.BinaryType)) {
            return BuiltinTypesCatalogue.hexBinaryItem;
        } else if (dt instanceof VectorUDT) {
            return BuiltinTypesCatalogue.arrayItem;
        }
        throw new OurBadException("DataFrame type unsupported: " + dt);
    }

    private static Item convertValueToItem(
            Row row,
            int i,
            Object o,
            DataType fieldType,
            ExceptionMetadata metadata
    ) {
        if (row != null && row.isNullAt(i)) {
            return ItemFactory.getInstance().createNullItem();
        } else if (fieldType.equals(DataTypes.StringType)) {
            String s;
            if (row != null) {
                s = row.getString(i);
            } else {
                s = (String) o;
            }
            return ItemFactory.getInstance().createStringItem(s);
        } else if (fieldType.equals(DataTypes.BooleanType)) {
            boolean b;
            if (row != null) {
                b = row.getBoolean(i);
            } else {
                b = (Boolean) o;
            }
            return ItemFactory.getInstance().createBooleanItem(b);
        } else if (fieldType.equals(DataTypes.DoubleType)) {
            double value;
            if (row != null) {
                value = row.getDouble(i);
            } else {
                value = (Double) o;
            }
            return ItemFactory.getInstance().createDoubleItem(value);
        } else if (fieldType.equals(DataTypes.IntegerType)) {
            int value;
            if (row != null) {
                value = row.getInt(i);
            } else {
                value = (Integer) o;
            }
            return ItemFactory.getInstance().createIntItem(value);
        } else if (fieldType.equals(DataTypes.FloatType)) {
            float value;
            if (row != null) {
                value = row.getFloat(i);
            } else {
                value = (Float) o;
            }
            return ItemFactory.getInstance().createFloatItem(value);
        } else if (fieldType.equals(decimalType)) {
            BigDecimal value;
            if (row != null) {
                value = row.getDecimal(i);
            } else {
                value = (BigDecimal) o;
            }
            return ItemFactory.getInstance().createDecimalItem(value);
        } else if (fieldType.equals(DataTypes.LongType)) {
            BigDecimal value;
            if (row != null) {
                value = new BigDecimal(row.getLong(i));
            } else {
                value = new BigDecimal((Long) o);
            }
            return ItemFactory.getInstance().createDecimalItem(value);
        } else if (fieldType.equals(DataTypes.NullType)) {
            return ItemFactory.getInstance().createNullItem();
        } else if (fieldType.equals(DataTypes.ShortType)) {
            short value;
            if (row != null) {
                value = row.getShort(i);
            } else {
                value = (Short) o;
            }
            return ItemFactory.getInstance().createIntItem(value);
        } else if (fieldType.equals(DataTypes.TimestampType)) {
            Timestamp value;
            if (row != null) {
                value = row.getTimestamp(i);
            } else {
                value = (Timestamp) o;
            }
            Instant instant = value.toInstant();
            DateTime dt = new DateTime(instant);
            return ItemFactory.getInstance().createDateTimeItem(dt, false);
        } else if (fieldType.equals(DataTypes.DateType)) {
            Date value;
            if (row != null) {
                value = row.getDate(i);
            } else {
                value = (Date) o;
            }
            Instant instant = value.toInstant();
            DateTime dt = new DateTime(instant);
            return ItemFactory.getInstance().createDateItem(dt, false);
        } else if (fieldType.equals(DataTypes.BinaryType)) {
            byte[] value;
            if (row != null) {
                value = (byte[]) row.get(i);
            } else {
                value = (byte[]) o;
            }
            return ItemFactory.getInstance().createHexBinaryItem(Hex.encodeHexString(value));
        } else if (fieldType instanceof StructType) {
            Row value;
            if (row != null) {
                value = row.getStruct(i);
            } else {
                value = (Row) o;
            }
            return getItemFromRow(value, metadata);
        } else if (fieldType instanceof ArrayType) {
            ArrayType arrayType = (ArrayType) fieldType;
            DataType dataType = arrayType.elementType();
            List<Item> members = new ArrayList<>();
            if (row != null) {
                List<Object> objects = row.getList(i);
                for (Object object : objects) {
                    members.add(convertValueToItem(object, dataType, metadata));
                }
            } else {
                @SuppressWarnings("unchecked")
                Object arrayObject = ((WrappedArray<Object>) o).array();
                for (int index = 0; index < Array.getLength(arrayObject); index++) {
                    Object value = Array.get(arrayObject, index);
                    members.add(convertValueToItem(value, dataType, metadata));
                }
            }
            return ItemFactory.getInstance().createArrayItem(members);
        } else if (fieldType instanceof VectorUDT) {
            Vector vector;
            if (row != null) {
                vector = (Vector) row.get(i);
            } else {
                vector = (Vector) o;
            }
            if (vector instanceof DenseVector) {
                // a dense vector is mapped to a rumble array
                DenseVector denseVector = (DenseVector) vector;
                List<Item> members = new ArrayList<>(vector.size());
                for (double value : denseVector.values()) {
                    members.add(ItemFactory.getInstance().createDoubleItem(value));
                }
                return ItemFactory.getInstance().createArrayItem(members);
            } else if (vector instanceof SparseVector) {
                // a sparse vector is mapped to a Rumble object where keys are indices of the non-0 values in the vector
                SparseVector sparseVector = (SparseVector) vector;
                List<String> objectKeyList = new ArrayList<>();
                List<Item> objectValueList = new ArrayList<>();
                int[] vectorIndices = sparseVector.indices();
                double[] vectorValues = sparseVector.values();
                for (int j = 0; j < vectorIndices.length; j++) {
                    objectKeyList.add(String.valueOf(vectorIndices[j]));
                    objectValueList.add(ItemFactory.getInstance().createDoubleItem(vectorValues[j]));
                }
                return ItemFactory.getInstance().createObjectItem(objectKeyList, objectValueList, metadata);
            } else {
                throw new OurBadException("Unexpected program state reached while converting vectorUDT to rumble item");
            }
        } else {
            throw new RuntimeException("DataFrame type unsupported: " + fieldType.json());
        }
    }

    public static DataType getDataFrameDataTypeFromItemType(ItemType itemType) {
        if (itemType.equals(BuiltinTypesCatalogue.booleanItem)) {
            return DataTypes.BooleanType;
        }
        if (itemType.equals(BuiltinTypesCatalogue.integerItem)) {
            return DataTypes.IntegerType;
        }
        if (itemType.equals(BuiltinTypesCatalogue.doubleItem)) {
            return DataTypes.DoubleType;
        }
        if (itemType.equals(BuiltinTypesCatalogue.floatItem)) {
            return DataTypes.FloatType;
        }
        if (itemType.equals(BuiltinTypesCatalogue.decimalItem)) {
            return decimalType;
        }
        if (itemType.equals(BuiltinTypesCatalogue.stringItem)) {
            return DataTypes.StringType;
        }
        if (itemType.equals(BuiltinTypesCatalogue.nullItem)) {
            return DataTypes.NullType;
        }
        if (itemType.equals(BuiltinTypesCatalogue.dateItem)) {
            return DataTypes.DateType;
        }
        if (itemType.equals(BuiltinTypesCatalogue.dateTimeItem)) {
            return DataTypes.TimestampType;
        }
        if (itemType.equals(BuiltinTypesCatalogue.hexBinaryItem)) {
            return DataTypes.BinaryType;
        }
        if (itemType.equals(BuiltinTypesCatalogue.objectItem)) {
            return vectorType;
        }
        throw new IllegalArgumentException(
                "Unexpected item type found: '" + itemType + "' in namespace " + itemType.getName().getNamespace() + "."
        );
    }

    public static Name getItemTypeNameFromDataFrameDataType(DataType dataType) {
        if (DataTypes.BooleanType.equals(dataType)) {
            return BuiltinTypesCatalogue.booleanItem.getName();
        }
        if (DataTypes.IntegerType.equals(dataType) || DataTypes.ShortType.equals(dataType)) {
            return BuiltinTypesCatalogue.integerItem.getName();
        }
        if (DataTypes.DoubleType.equals(dataType)) {
            return BuiltinTypesCatalogue.doubleItem.getName();
        }
        if (DataTypes.FloatType.equals(dataType)) {
            return BuiltinTypesCatalogue.floatItem.getName();
        }
        if (dataType.equals(decimalType) || DataTypes.LongType.equals(dataType)) {
            return BuiltinTypesCatalogue.decimalItem.getName();
        }
        if (DataTypes.StringType.equals(dataType)) {
            return BuiltinTypesCatalogue.stringItem.getName();
        }
        if (DataTypes.NullType.equals(dataType)) {
            return BuiltinTypesCatalogue.nullItem.getName();
        }
        if (DataTypes.DateType.equals(dataType)) {
            return BuiltinTypesCatalogue.dateItem.getName();
        }
        if (DataTypes.TimestampType.equals(dataType)) {
            return BuiltinTypesCatalogue.dateTimeItem.getName();
        }
        if (DataTypes.BinaryType.equals(dataType)) {
            return BuiltinTypesCatalogue.hexBinaryItem.getName();
        }
        if (vectorType.equals(dataType)) {
            return BuiltinTypesCatalogue.objectItem.getName();
        }
        throw new OurBadException("Unexpected DataFrame data type found: '" + dataType.toString() + "'.");
    }

    public static List<Row> getRowsFromItemsUsingSchema(List<Item> items, StructType schema) {
        List<Row> rows = new ArrayList<>();
        for (Item item : items) {
            Row row = getRowFromItemUsingSchema(item, schema);
            rows.add(row);
        }
        return rows;
    }

    public static Row getRowFromItemUsingSchema(Item item, StructType schema) {
        Object[] rowColumns = new Object[schema.fields().length];
        for (int fieldIndex = 0; fieldIndex < schema.fields().length; fieldIndex++) {
            StructField field = schema.fields()[fieldIndex];
            Object rowColumn = getRowColumnFromItemUsingSchemaField(item, field);
            rowColumns[fieldIndex] = rowColumn;
        }
        return RowFactory.create(rowColumns);
    }

    private static Object getRowColumnFromItemUsingSchemaField(Item item, StructField field) {
        String fieldName = field.name();
        DataType fieldDataType = field.dataType();
        Item columnValueItem = item.getItemByKey(fieldName);
        if (columnValueItem == null) {
            throw new MLInvalidDataFrameSchemaException(
                    "Missing field '" + fieldName + "' in object '" + item.serialize() + "'."
            );
        }
        try {
            return getRowColumnFromItemUsingDataType(columnValueItem, fieldDataType);
        } catch (MLInvalidDataFrameSchemaException ex) {
            throw new MLInvalidDataFrameSchemaException(
                    "Data does not fit to the given schema in field '"
                        + fieldName
                        + "'; "
                        + ex.getJSONiqErrorMessage()
            );
        }
    }

    private static Object getRowColumnFromItemUsingDataType(Item item, DataType dataType) {
        try {
            if (dataType instanceof ArrayType) {
                if (!item.isArray()) {
                    throw new MLInvalidDataFrameSchemaException("Type mismatch " + dataType);
                }
                List<Item> arrayItems = item.getItems();
                Object[] arrayItemsForRow = new Object[arrayItems.size()];
                DataType elementType = ((ArrayType) dataType).elementType();
                for (int i = 0; i < arrayItems.size(); i++) {
                    Item arrayItem = item.getItemAt(i);
                    arrayItemsForRow[i] = getRowColumnFromItemUsingDataType(arrayItem, elementType);
                }
                return arrayItemsForRow;
            }

            if (dataType instanceof StructType) {
                if (!item.isObject()) {
                    throw new MLInvalidDataFrameSchemaException("Type mismatch " + dataType);
                }
                return getRowFromItemUsingSchema(item, (StructType) dataType);
            }

            if (dataType.equals(DataTypes.BooleanType)) {
                if (!item.isBoolean()) {
                    Item i = CastIterator.castItemToType(
                        item,
                        BuiltinTypesCatalogue.booleanItem,
                        ExceptionMetadata.EMPTY_METADATA
                    );
                    if (i == null) {
                        throw new MLInvalidDataFrameSchemaException(
                                "Type mismatch and cast unsuccessful to " + dataType
                        );
                    }
                    return i.getBooleanValue();
                }
                return item.getBooleanValue();
            }
            if (dataType.equals(DataTypes.IntegerType)) {
                if (!item.isInt()) {
                    Item i = CastIterator.castItemToType(
                        item,
                        BuiltinTypesCatalogue.intItem,
                        ExceptionMetadata.EMPTY_METADATA
                    );
                    if (i == null) {
                        throw new MLInvalidDataFrameSchemaException(
                                "Type mismatch and cast unsuccessful to " + dataType
                        );
                    }
                    return i.getIntValue();
                }
                return item.getIntValue();
            }
            if (dataType.equals(DataTypes.DoubleType)) {
                if (!item.isDouble()) {
                    Item i = CastIterator.castItemToType(
                        item,
                        BuiltinTypesCatalogue.doubleItem,
                        ExceptionMetadata.EMPTY_METADATA
                    );
                    if (i == null) {
                        throw new MLInvalidDataFrameSchemaException(
                                "Type mismatch and cast unsuccessful to " + dataType
                        );
                    }
                    return i.getDoubleValue();
                }
                return item.getDoubleValue();
            }
            if (dataType.equals(DataTypes.FloatType)) {
                if (!item.isFloat()) {
                    Item i = CastIterator.castItemToType(
                        item,
                        BuiltinTypesCatalogue.floatItem,
                        ExceptionMetadata.EMPTY_METADATA
                    );
                    if (i == null) {
                        throw new MLInvalidDataFrameSchemaException(
                                "Type mismatch and cast unsuccessful to " + dataType
                        );
                    }
                    return i.getFloatValue();
                }
                return item.getFloatValue();
            }
            if (dataType.equals(decimalType)) {
                if (!item.isDecimal()) {
                    Item i = CastIterator.castItemToType(
                        item,
                        BuiltinTypesCatalogue.decimalItem,
                        ExceptionMetadata.EMPTY_METADATA
                    );
                    if (i == null) {
                        throw new MLInvalidDataFrameSchemaException(
                                "Type mismatch and cast unsuccessful to " + dataType
                        );
                    }
                    return i.getDecimalValue();
                }
                return item.getDecimalValue();
            }
            if (dataType.equals(DataTypes.StringType)) {
                if (!item.isString()) {
                    Item i = CastIterator.castItemToType(
                        item,
                        BuiltinTypesCatalogue.stringItem,
                        ExceptionMetadata.EMPTY_METADATA
                    );
                    if (i == null) {
                        throw new MLInvalidDataFrameSchemaException(
                                "Type mismatch and cast unsuccessful to " + dataType
                        );
                    }
                    return i.getStringValue();
                }
                return item.getStringValue();
            }
            if (dataType.equals(DataTypes.NullType)) {
                if (!item.isNull()) {
                    Item i = CastIterator.castItemToType(
                        item,
                        BuiltinTypesCatalogue.nullItem,
                        ExceptionMetadata.EMPTY_METADATA
                    );
                    if (i == null) {
                        throw new MLInvalidDataFrameSchemaException(
                                "Type mismatch and cast unsuccessful to " + dataType
                        );
                    }
                    return null;
                }
                return null;
            }
            if (dataType.equals(DataTypes.DateType)) {
                if (!item.isDate()) {
                    Item i = CastIterator.castItemToType(
                        item,
                        BuiltinTypesCatalogue.dateItem,
                        ExceptionMetadata.EMPTY_METADATA
                    );
                    if (i == null) {
                        throw new MLInvalidDataFrameSchemaException(
                                "Type mismatch and cast unsuccessful to " + dataType
                        );
                    }
                    return new Date(item.getDateTimeValue().getMillis());
                }
                return new Date(item.getDateTimeValue().getMillis());
            }
            if (dataType.equals(DataTypes.TimestampType)) {
                if (!item.isDateTime()) {
                    Item i = CastIterator.castItemToType(
                        item,
                        BuiltinTypesCatalogue.dateTimeItem,
                        ExceptionMetadata.EMPTY_METADATA
                    );
                    if (i == null) {
                        throw new MLInvalidDataFrameSchemaException(
                                "Type mismatch and cast unsuccessful to " + dataType
                        );
                    }
                    return new Timestamp(item.getDateTimeValue().getMillis());
                }
                return new Timestamp(item.getDateTimeValue().getMillis());
            }
        } catch (OurBadException ex) {
            // OurBadExceptions triggered by invalid use of value getters here are caused by user's schema
            throw new MLInvalidDataFrameSchemaException(ex.getJSONiqErrorMessage());
        }

        throw new OurBadException(
                "Unhandled item type found while generating rows: '" + dataType + "' ."
        );
    }
}
