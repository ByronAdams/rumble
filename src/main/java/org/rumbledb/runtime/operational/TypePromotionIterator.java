package org.rumbledb.runtime.operational;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.rumbledb.api.Item;
import org.rumbledb.context.DynamicContext;
import org.rumbledb.exceptions.ExceptionMetadata;
import org.rumbledb.exceptions.IteratorFlowException;
import org.rumbledb.exceptions.OurBadException;
import org.rumbledb.exceptions.UnexpectedTypeException;
import org.rumbledb.expressions.ExecutionMode;
import org.rumbledb.items.AtomicItem;
import org.rumbledb.runtime.HybridRuntimeIterator;
import org.rumbledb.runtime.RuntimeIterator;
import org.rumbledb.runtime.functions.sequences.general.TypePromotionClosure;
import org.rumbledb.runtime.typing.InstanceOfIterator;
import org.rumbledb.runtime.typing.TreatIterator;
import org.rumbledb.types.AtomicItemType;
import org.rumbledb.types.ItemType;
import org.rumbledb.types.SequenceType;
import org.rumbledb.types.SequenceType.Arity;

import sparksoniq.spark.SparkSessionManager;

import java.util.Collections;

public class TypePromotionIterator extends HybridRuntimeIterator {

    private static final long serialVersionUID = 1L;
    private final String exceptionMessage;
    private RuntimeIterator iterator;
    private SequenceType sequenceType;

    private ItemType itemType;

    private Item nextResult;
    private int childIndex;

    public TypePromotionIterator(
            RuntimeIterator iterator,
            SequenceType sequenceType,
            String exceptionMessage,
            ExecutionMode executionMode,
            ExceptionMetadata iteratorMetadata
    ) {
        super(Collections.singletonList(iterator), executionMode, iteratorMetadata);
        this.exceptionMessage = exceptionMessage;
        this.iterator = iterator;
        this.sequenceType = sequenceType;
        this.itemType = this.sequenceType.getItemType();
        if (
            !executionMode.equals(ExecutionMode.LOCAL)
                && (sequenceType.isEmptySequence()
                    || sequenceType.getArity().equals(Arity.One)
                    || sequenceType.getArity().equals(Arity.OneOrZero))
        ) {
            throw new OurBadException(
                    "A promotion iterator should never be executed in parallel if the sequence type arity is 0, 1 or ?."
            );
        }
    }

    @Override
    public boolean hasNextLocal() {
        return this.hasNext;
    }

    @Override
    public void resetLocal() {
        this.iterator.reset(this.currentDynamicContextForLocalExecution);
        this.childIndex = 0;
        setNextResult();
    }

    @Override
    public void closeLocal() {
        this.iterator.close();
    }

    @Override
    public void openLocal() {
        this.childIndex = 0;
        this.iterator.open(this.currentDynamicContextForLocalExecution);
        this.setNextResult();
    }


    @Override
    public Item nextLocal() {
        if (this.hasNext) {
            Item currentResult = this.nextResult;
            setNextResult();
            return currentResult;
        } else {
            throw new IteratorFlowException(RuntimeIterator.FLOW_EXCEPTION_MESSAGE, getMetadata());
        }
    }

    private void setNextResult() {
        this.nextResult = null;
        if (this.iterator.hasNext()) {
            this.nextResult = this.iterator.next();
            this.childIndex++;
        } else {
            this.iterator.close();
            checkEmptySequence(this.childIndex);
        }

        this.hasNext = this.nextResult != null;
        if (!hasNext()) {
            return;
        }

        checkItemsSize(this.childIndex);
        if (!InstanceOfIterator.doesItemTypeMatchItem(this.itemType, this.nextResult)) {
            checkTypePromotion();
        }
    }

    private void checkEmptySequence(long size) {
        if (
            size == 0
                && (this.sequenceType.getArity() == SequenceType.Arity.One
                    ||
                    this.sequenceType.getArity() == SequenceType.Arity.OneOrMore)
        ) {
            throw new UnexpectedTypeException(
                    this.exceptionMessage
                        + "Expecting"
                        + ((this.sequenceType.getArity() == SequenceType.Arity.OneOrMore) ? " at least" : "")
                        + " one item, but the value provided is the empty sequence.",
                    getMetadata()
            );
        }
    }

    private void checkItemsSize(long size) {
        if (size > 0 && this.sequenceType.isEmptySequence()) {
            throw new UnexpectedTypeException(
                    this.exceptionMessage
                        + "Expecting empty sequence, but the value provided has at least one item.",
                    getMetadata()
            );
        }


        if (
            size > 1
                && (this.sequenceType.getArity() == SequenceType.Arity.One
                    ||
                    this.sequenceType.getArity() == SequenceType.Arity.OneOrZero)
        ) {
            throw new UnexpectedTypeException(
                    this.exceptionMessage
                        + "Expecting"
                        + ((this.sequenceType.getArity() == SequenceType.Arity.OneOrZero) ? " at most" : "")
                        + " one item, but the value provided has at least two items.",
                    getMetadata()
            );
        }
    }

    @Override
    public JavaRDD<Item> getRDDAux(DynamicContext context) {
        JavaRDD<Item> childRDD = this.iterator.getRDD(context);

        int count = childRDD.take(2).size();
        checkEmptySequence(count);
        checkItemsSize(count);
        Function<Item, Item> transformation = new TypePromotionClosure(
                this.exceptionMessage,
                this.sequenceType,
                getMetadata()
        );
        return childRDD.map(transformation);
    }

    @Override
    protected boolean implementsDataFrames() {
        return true;
    }

    @Override
    public Dataset<Row> getDataFrame(DynamicContext dynamicContext) {
        Dataset<Row> df = this.iterator.getDataFrame(dynamicContext);
        int count = df.takeAsList(1).size();
        checkEmptySequence(count);
        if (count == 0) {
            return df;
        }
        ItemType dataItemType = TreatIterator.getItemType(df);
        if (dataItemType.isSubtypeOf(AtomicItemType.decimalItem) && this.itemType.equals(AtomicItemType.doubleItem)) {
            df.createOrReplaceTempView("input");
            df = df.sparkSession()
                .sql(
                    "SELECT CAST (`"
                        + SparkSessionManager.atomicJSONiqItemColumnName
                        + "` AS double) AS `"
                        + SparkSessionManager.atomicJSONiqItemColumnName
                        + "` FROM input"
                );
        }
        dataItemType = TreatIterator.getItemType(df);
        if (dataItemType.isSubtypeOf(this.itemType)) {
            return df;
        }
        throw new UnexpectedTypeException(
                this.exceptionMessage
                    + dataItemType
                    + " cannot be promoted to type "
                    + this.sequenceType
                    + ".",
                getMetadata()
        );
    }

    private void checkTypePromotion() {
        if (this.nextResult.isFunction()) {
            return;
        }
        if (!this.nextResult.canBePromotedTo(this.sequenceType.getItemType())) {
            throw new UnexpectedTypeException(
                    this.exceptionMessage
                        + this.nextResult.getDynamicType().toString()
                        + " cannot be promoted to type "
                        + this.sequenceType
                        + ".",
                    getMetadata()
            );
        }
        this.nextResult = this.nextResult.promoteTo(this.sequenceType.getItemType());
    }
}
