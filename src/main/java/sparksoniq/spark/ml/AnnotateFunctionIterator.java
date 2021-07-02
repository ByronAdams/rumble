package sparksoniq.spark.ml;

import org.apache.spark.api.java.JavaRDD;
import org.rumbledb.api.Item;
import org.rumbledb.context.DynamicContext;
import org.rumbledb.exceptions.ExceptionMetadata;
import org.rumbledb.exceptions.InvalidInstanceException;
import org.rumbledb.expressions.ExecutionMode;
import org.rumbledb.items.structured.JSoundDataFrame;
import org.rumbledb.runtime.DataFrameRuntimeIterator;
import org.rumbledb.runtime.RuntimeIterator;
import org.rumbledb.types.BuiltinTypesCatalogue;
import org.rumbledb.types.ItemType;
import org.rumbledb.types.ItemTypeFactory;

import sparksoniq.spark.DataFrameUtils;

import java.util.List;

public class AnnotateFunctionIterator extends DataFrameRuntimeIterator {

    private static final long serialVersionUID = 1L;

    public AnnotateFunctionIterator(
            List<RuntimeIterator> arguments,
            ExecutionMode executionMode,
            ExceptionMetadata iteratorMetadata
    ) {
        super(arguments, executionMode, iteratorMetadata);
    }

    @Override
    public JSoundDataFrame getDataFrame(DynamicContext context) {
        RuntimeIterator inputDataIterator = this.children.get(0);
        RuntimeIterator schemaIterator = this.children.get(1);
        Item schemaItem = schemaIterator.materializeFirstItemOrNull(context);
        ItemType schemaType = ItemTypeFactory.createItemTypeFromJSoundCompactItem(null, schemaItem, null);
        schemaType.resolve(context, getMetadata());
        try {

            if (inputDataIterator.isDataFrame()) {
                JSoundDataFrame inputDataAsDataFrame = inputDataIterator.getDataFrame(context);
                inputDataAsDataFrame.getDataFrame().printSchema();
                inputDataAsDataFrame.getDataFrame().show();
                ItemType actualSchemaType = ItemTypeFactory.createItemType(
                    inputDataAsDataFrame.getDataFrame().schema()
                );
                DataFrameUtils.validateSchemaItemAgainstDataFrame(
                    schemaType,
                    actualSchemaType
                );
                return inputDataAsDataFrame;
            }

            if (inputDataIterator.isRDDOrDataFrame()) {
                JavaRDD<Item> rdd = inputDataIterator.getRDD(context);
                return new JSoundDataFrame(
                        DataFrameUtils.convertItemRDDToDataFrame(rdd, schemaItem),
                        BuiltinTypesCatalogue.objectItem
                );
            }

            List<Item> items = inputDataIterator.materialize(context);
            return new JSoundDataFrame(
                    DataFrameUtils.convertLocalItemsToDataFrame(items, schemaItem),
                    BuiltinTypesCatalogue.objectItem
            );
        } catch (InvalidInstanceException ex) {
            InvalidInstanceException e = new InvalidInstanceException(
                    "Schema error in annotate(); " + ex.getJSONiqErrorMessage(),
                    getMetadata()
            );
            e.initCause(ex);
            throw e;
        }
    }

}
