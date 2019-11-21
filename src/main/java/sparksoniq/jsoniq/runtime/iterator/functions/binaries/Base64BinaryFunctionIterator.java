package sparksoniq.jsoniq.runtime.iterator.functions.binaries;

import org.rumbledb.api.Item;
import sparksoniq.exceptions.CastException;
import sparksoniq.exceptions.IteratorFlowException;
import sparksoniq.exceptions.UnexpectedTypeException;
import sparksoniq.exceptions.UnknownFunctionCallException;
import sparksoniq.jsoniq.item.ItemFactory;
import sparksoniq.jsoniq.item.StringItem;
import sparksoniq.jsoniq.runtime.iterator.RuntimeIterator;
import sparksoniq.jsoniq.runtime.iterator.functions.base.LocalFunctionCallIterator;
import sparksoniq.jsoniq.runtime.metadata.IteratorMetadata;
import sparksoniq.semantics.DynamicContext;

import java.util.List;

public class Base64BinaryFunctionIterator extends LocalFunctionCallIterator {

    private static final long serialVersionUID = 1L;
    private StringItem _base64BinaryStringItem = null;

    public Base64BinaryFunctionIterator(
            List<RuntimeIterator> parameters,
            IteratorMetadata iteratorMetadata
    ) {
        super(parameters, iteratorMetadata);
    }

    @Override
    public Item next() {
        if (this._hasNext) {
            this._hasNext = false;
            try {
                return ItemFactory.getInstance().createBase64BinaryItem(_base64BinaryStringItem.getStringValue());
            } catch (IllegalArgumentException e) {
                String message = String.format(
                    "\"%s\": value of type %s is not castable to type %s",
                    _base64BinaryStringItem.serialize(),
                    "string",
                    "base64Binary"
                );
                throw new CastException(message, getMetadata());
            }
        } else
            throw new IteratorFlowException(
                    RuntimeIterator.FLOW_EXCEPTION_MESSAGE + " base64Binary function",
                    getMetadata()
            );
    }


    @Override
    public void open(DynamicContext context) {
        super.open(context);
        try {
            _base64BinaryStringItem = this.getSingleItemOfTypeFromIterator(
                this._children.get(0),
                StringItem.class,
                new UnknownFunctionCallException("base64Binary", this._children.size(), getMetadata())
            );
        } catch (UnknownFunctionCallException e) {
            throw new UnexpectedTypeException(
                    " Sequence of more than one item can not be cast to type with quantifier '1' or '?'",
                    getMetadata()
            );
        }
        this._hasNext = _base64BinaryStringItem != null;
    }
}
