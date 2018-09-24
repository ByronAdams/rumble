package sparksoniq.jsoniq.runtime.iterator.functions.object;

import sparksoniq.exceptions.IteratorFlowException;
import sparksoniq.jsoniq.item.Item;
import sparksoniq.jsoniq.item.ObjectItem;
import sparksoniq.jsoniq.item.StringItem;
import sparksoniq.jsoniq.item.metadata.ItemMetadata;
import sparksoniq.jsoniq.runtime.iterator.RuntimeIterator;
import sparksoniq.jsoniq.runtime.metadata.IteratorMetadata;
import static sparksoniq.jsoniq.runtime.iterator.functions.object.ObjectFunctionUtilities.listHasDuplicateString;

import javax.naming.OperationNotSupportedException;
import java.util.ArrayList;
import java.util.List;

public class ObjectKeysFunctionIterator extends ObjectFunctionIterator {
    public ObjectKeysFunctionIterator(List<RuntimeIterator> arguments, IteratorMetadata iteratorMetadata) {
        super(arguments, ObjectFunctionOperators.KEYS, iteratorMetadata);
    }

    @Override
    public Item next() {
        if (this._hasNext) {
            if (results == null) {
                _currentIndex = 0;
                results = new ArrayList<>();
                RuntimeIterator sequenceIterator = this._children.get(0);
                List<Item> items = getItemsFromIteratorWithCurrentContext(sequenceIterator);
                for (Item item:items) {
                    if (item.isObject()) {
                        try {
                            StringItem result = null;
                            for (String key : item.getKeys()) {
                                result = new StringItem(key, ItemMetadata.fromIteratorMetadata(getMetadata()));
                                if (!listHasDuplicateString(results, result))
                                {
                                    results.add(result);
                                }
                            }
                        } catch (OperationNotSupportedException e) {
                            e.printStackTrace();
                        }
                    }
                }

                // single item algorithm doesn't work for sequences
                /*
                ObjectItem object = getSingleItemOfTypeFromIterator(objectIterator, ObjectItem.class);
                for (String key : object.getKeys())
                    results.add(new StringItem(key, ItemMetadata.fromIteratorMetadata(getMetadata())));
                */
            }
            return getResult();
        }
        throw new IteratorFlowException(RuntimeIterator.FLOW_EXCEPTION_MESSAGE + " KEYS function",
                getMetadata());
    }
}