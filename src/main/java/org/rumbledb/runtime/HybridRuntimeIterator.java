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

package org.rumbledb.runtime;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.rumbledb.api.Item;
import org.rumbledb.context.DynamicContext;
import org.rumbledb.exceptions.ExceptionMetadata;
import org.rumbledb.exceptions.IteratorFlowException;
import org.rumbledb.expressions.ExecutionMode;
import org.rumbledb.items.parsing.RowToItemMapper;

import sparksoniq.spark.SparkSessionManager;

import java.util.List;

public abstract class HybridRuntimeIterator extends RuntimeIterator {

    private static final long serialVersionUID = 1L;
    protected List<Item> result = null;
    private int currentResultIndex = 0;

    protected HybridRuntimeIterator(
            List<RuntimeIterator> children,
            ExecutionMode executionMode,
            ExceptionMetadata iteratorMetadata
    ) {
        super(children, executionMode, iteratorMetadata);
        fallbackToRDDIfDFNotImplemented(executionMode);
    }

    protected boolean implementsDataFrames() {
        return false;
    }

    protected void fallbackToRDDIfDFNotImplemented(ExecutionMode executionMode) {
        if (executionMode == ExecutionMode.DATAFRAME && !this.implementsDataFrames()) {
            this.highestExecutionMode = ExecutionMode.RDD;
        }
    }

    @Override
    public void open(DynamicContext context) {
        super.open(context);
        if (!isRDDOrDataFrame()) {
            openLocal();
        }
    }

    @Override
    public void reset(DynamicContext context) {
        super.reset(context);
        if (!isRDDOrDataFrame()) {
            resetLocal();
            return;
        }
        this.result = null;
    }

    @Override
    public void close() {
        super.close();
        if (!isRDDOrDataFrame()) {
            closeLocal();
            return;
        }
        this.result = null;
    }

    @Override
    public boolean hasNext() {
        if (!isRDDOrDataFrame()) {
            return hasNextLocal();
        }
        if (this.result == null) {
            this.currentResultIndex = 0;
            JavaRDD<Item> rdd = this.getRDD(this.currentDynamicContextForLocalExecution);
            this.result = SparkSessionManager.collectRDDwithLimit(rdd, this.getMetadata());
            this.hasNext = !this.result.isEmpty();
        }
        return this.hasNext;
    }

    @Override
    public Item next() {
        if (!isRDDOrDataFrame()) {
            return nextLocal();
        }
        if (!this.isOpen) {
            throw new IteratorFlowException("Runtime iterator is not open", getMetadata());
        }

        if (!(this.currentResultIndex <= this.result.size() - 1)) {
            throw new IteratorFlowException(
                    RuntimeIterator.FLOW_EXCEPTION_MESSAGE + this.getClass().getSimpleName(),
                    getMetadata()
            );
        }
        if (this.currentResultIndex == this.result.size() - 1) {
            this.hasNext = false;
        }

        Item item = this.result.get(this.currentResultIndex);
        this.currentResultIndex++;
        return item;
    }


    @Override
    public JavaRDD<Item> getRDD(DynamicContext context) {
        if (isDataFrame()) {
            Dataset<Row> df = this.getDataFrame(context);
            return dataFrameToRDDOfItems(df, getMetadata());
        } else if (isRDDOrDataFrame()) {
            return getRDDAux(context);
        } else {
            List<Item> contents = this.materialize(context);
            return SparkSessionManager.getInstance().getJavaSparkContext().parallelize(contents);
        }
    }

    public static JavaRDD<Item> dataFrameToRDDOfItems(Dataset<Row> df, ExceptionMetadata metadata) {
        JavaRDD<Row> rowRDD = df.javaRDD();
        return rowRDD.map(new RowToItemMapper(metadata));
    }

    protected abstract JavaRDD<Item> getRDDAux(DynamicContext context);

    protected abstract void openLocal();

    protected abstract void closeLocal();

    protected abstract void resetLocal();

    protected abstract boolean hasNextLocal();

    protected abstract Item nextLocal();
}
