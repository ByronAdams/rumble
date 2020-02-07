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

package sparksoniq.spark.iterator.flowr;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;
import org.rumbledb.api.Item;
import org.rumbledb.exceptions.ExceptionMetadata;
import org.rumbledb.exceptions.IteratorFlowException;
import org.rumbledb.exceptions.OurBadException;
import sparksoniq.jsoniq.ExecutionMode;
import sparksoniq.jsoniq.item.ItemFactory;
import sparksoniq.jsoniq.runtime.iterator.RuntimeIterator;
import sparksoniq.jsoniq.runtime.iterator.primary.VariableReferenceIterator;
import sparksoniq.jsoniq.runtime.tupleiterator.RuntimeTupleIterator;
import sparksoniq.jsoniq.tuple.FlworTuple;
import sparksoniq.semantics.DynamicContext;
import sparksoniq.spark.DataFrameUtils;
import sparksoniq.spark.udf.CountClauseSerializeUDF;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class CountClauseSparkIterator extends RuntimeTupleIterator {

    private static final long serialVersionUID = 1L;
    private String variableName;
    private FlworTuple nextLocalTupleResult;
    private int _currentCountIndex;

    public CountClauseSparkIterator(
            RuntimeTupleIterator child,
            RuntimeIterator variableReference,
            ExecutionMode executionMode,
            ExceptionMetadata iteratorMetadata
    ) {
        super(child, executionMode, iteratorMetadata);
        this.variableName = ((VariableReferenceIterator) variableReference).getVariableName();
        this._currentCountIndex = 1; // indices start at 1 in JSONiq
    }

    @Override
    public void open(DynamicContext context) {
        super.open(context);
        if (this.child != null) {
            this.child.open(this.currentDynamicContext);

            setNextLocalTupleResult();
        } else {
            throw new OurBadException("Invalid count clause.");
        }
    }

    @Override
    public void close() {
        super.close();
        this._currentCountIndex = 1;
    }

    @Override
    public FlworTuple next() {
        if (this.hasNext) {
            FlworTuple result = this.nextLocalTupleResult; // save the result to be returned
            setNextLocalTupleResult(); // calculate and store the next result
            return result;
        }
        throw new IteratorFlowException("Invalid next() call in count flwor clause", getMetadata());
    }

    private void setNextLocalTupleResult() {
        if (this.child.hasNext()) {
            FlworTuple inputTuple = this.child.next();

            List<Item> results = new ArrayList<>();
            results.add(ItemFactory.getInstance().createIntegerItem(this._currentCountIndex++));

            this.nextLocalTupleResult = new FlworTuple(inputTuple).putValue(this.variableName, results);
            this.hasNext = true;
        } else {
            this.child.close();
            this.hasNext = false;
        }
    }

    @Override
    public Dataset<Row> getDataFrame(
            DynamicContext context,
            Map<String, DynamicContext.VariableDependency> parentProjection
    ) {
        if (this.child == null) {
            throw new OurBadException("Invalid count clause.");
        }
        Dataset<Row> df = this.child.getDataFrame(context, getProjection(parentProjection));
        StructType inputSchema = df.schema();
        int duplicateVariableIndex = Arrays.asList(inputSchema.fieldNames()).indexOf(this.variableName);

        List<String> allColumns = DataFrameUtils.getColumnNames(inputSchema, duplicateVariableIndex, null);

        String selectSQL = DataFrameUtils.getSQL(allColumns, true);

        Dataset<Row> dfWithIndex = DataFrameUtils.zipWithIndex(df, 1L, this.variableName);

        df.sparkSession()
            .udf()
            .register(
                "serializeCountIndex",
                new CountClauseSerializeUDF(),
                DataTypes.BinaryType
            );

        dfWithIndex.createOrReplaceTempView("input");
        dfWithIndex = dfWithIndex.sparkSession()
            .sql(
                String.format(
                    "select %s serializeCountIndex(`%s`) as `%s` from input",
                    selectSQL,
                    this.variableName,
                    this.variableName
                )
            );
        return dfWithIndex;
    }

    public Map<String, DynamicContext.VariableDependency> getVariableDependencies() {
        Map<String, DynamicContext.VariableDependency> result =
            new TreeMap<String, DynamicContext.VariableDependency>();
        result.putAll(this.child.getVariableDependencies());
        return result;
    }

    public Set<String> getVariablesBoundInCurrentFLWORExpression() {
        Set<String> result = new HashSet<String>();
        result.addAll(this.child.getVariablesBoundInCurrentFLWORExpression());
        result.add(this.variableName);
        return result;
    }

    public void print(StringBuffer buffer, int indent) {
        super.print(buffer, indent);
        for (int i = 0; i < indent + 1; ++i) {
            buffer.append("  ");
        }
        buffer.append("Variable ").append(this.variableName);
        buffer.append("\n");
    }

    public Map<String, DynamicContext.VariableDependency> getProjection(
            Map<String, DynamicContext.VariableDependency> parentProjection
    ) {
        // start with an empty projection.
        Map<String, DynamicContext.VariableDependency> projection =
            new TreeMap<String, DynamicContext.VariableDependency>();

        // copy over the projection needed by the parent clause.
        projection.putAll(parentProjection);

        // remove the variable that this clause binds.
        projection.remove(this.variableName);
        return projection;
    }
}
