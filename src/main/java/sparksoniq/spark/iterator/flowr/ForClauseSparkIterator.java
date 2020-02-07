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

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.rumbledb.api.Item;
import org.rumbledb.exceptions.ExceptionMetadata;
import org.rumbledb.exceptions.IteratorFlowException;
import org.rumbledb.exceptions.JobWithinAJobException;
import sparksoniq.jsoniq.ExecutionMode;
import sparksoniq.jsoniq.runtime.iterator.RuntimeIterator;
import sparksoniq.jsoniq.runtime.tupleiterator.RuntimeTupleIterator;
import sparksoniq.jsoniq.tuple.FlworTuple;
import sparksoniq.semantics.DynamicContext;
import sparksoniq.spark.DataFrameUtils;
import sparksoniq.spark.SparkSessionManager;
import sparksoniq.spark.closures.ForClauseLocalToRowClosure;
import sparksoniq.spark.closures.ForClauseSerializeClosure;
import sparksoniq.spark.udf.ForClauseUDF;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class ForClauseSparkIterator extends RuntimeTupleIterator {


    private static final long serialVersionUID = 1L;
    private String _variableName; // for efficient use in local iteration
    private RuntimeIterator _assignmentIterator;
    private Map<String, DynamicContext.VariableDependency> _dependencies;
    private DynamicContext _tupleContext; // re-use same DynamicContext object for efficiency
    private FlworTuple _nextLocalTupleResult;
    private FlworTuple _inputTuple; // tuple received from child, used for tuple creation

    public ForClauseSparkIterator(
            RuntimeTupleIterator child,
            String variableName,
            RuntimeIterator assignmentIterator,
            ExecutionMode executionMode,
            ExceptionMetadata iteratorMetadata
    ) {
        super(child, executionMode, iteratorMetadata);
        this._variableName = variableName;
        this._assignmentIterator = assignmentIterator;
        this._dependencies = this._assignmentIterator.getVariableDependencies();
    }

    @Override
    public void open(DynamicContext context) {
        super.open(context);

        if (this._child != null) { // if it's not a start clause
            this._child.open(this._currentDynamicContext);
            this._tupleContext = new DynamicContext(this._currentDynamicContext); // assign current context as parent

            setNextLocalTupleResult();
        } else { // if it's a start clause, get results using only the _assignmentIterator
            this._assignmentIterator.open(this._currentDynamicContext);
            setResultFromExpression();
        }
    }

    @Override
    public FlworTuple next() {
        if (this._hasNext) {
            FlworTuple result = this._nextLocalTupleResult; // save the result to be returned
            // calculate and store the next result
            if (this._child == null) { // if it's the initial for clause, call the correct function
                setResultFromExpression();
            } else {
                setNextLocalTupleResult();
            }
            return result;
        }
        throw new IteratorFlowException("Invalid next() call in let flwor clause", getMetadata());
    }

    private void setNextLocalTupleResult() {
        if (this._assignmentIterator.isOpen()) {
            if (setResultFromExpression()) {
                return;
            }
        }

        while (this._child.hasNext()) {
            this._inputTuple = this._child.next();
            this._tupleContext.removeAllVariables(); // clear the previous variables
            this._tupleContext.setBindingsFromTuple(this._inputTuple, getMetadata()); // assign new variables from new
                                                                                      // tuple

            this._assignmentIterator.open(this._tupleContext);
            if (setResultFromExpression()) {
                return;
            }
        }

        // execution reaches here when there are no more results
        this._child.close();
    }

    /**
     * _assignmentIterator has to be open prior to call.
     *
     * @return true if _nextLocalTupleResult is set and _hasNext is true, false otherwise
     */
    private boolean setResultFromExpression() {
        if (this._assignmentIterator.hasNext()) { // if expression returns a value, set it as next
            List<Item> results = new ArrayList<>();
            results.add(this._assignmentIterator.next());
            FlworTuple newTuple;
            if (this._child == null) { // if initial for clause
                newTuple = new FlworTuple().putValue(this._variableName, results);
            } else {
                newTuple = new FlworTuple(this._inputTuple).putValue(this._variableName, results);
            }
            this._nextLocalTupleResult = newTuple;
            this._hasNext = true;
            return true;
        } else {
            this._assignmentIterator.close();
            this._hasNext = false;
            return false;
        }
    }

    @Override
    public void close() {
        this._isOpen = false;
        if (this._child != null) {
            this._child.close();
        }
        this._assignmentIterator.close();
    }

    @Override
    public Dataset<Row> getDataFrame(
            DynamicContext context,
            Map<String, DynamicContext.VariableDependency> parentProjection
    ) {
        // if it's a starting clause
        if (this._child == null) {
            return getDataFrameFromRDDExpression(context);
        }

        if (this._child.isDataFrame()) {
            if (this._assignmentIterator.isRDD()) {
                Set<String> intersection = new HashSet<>(this._assignmentIterator.getVariableDependencies().keySet());
                intersection.retainAll(getVariablesBoundInCurrentFLWORExpression());
                boolean expressionUsesVariablesOfCurrentFlwor = !intersection.isEmpty();

                if (expressionUsesVariablesOfCurrentFlwor) {
                    throw new JobWithinAJobException(
                            "A for clause expression cannot produce a big sequence of items for a big number of tuples, as this would lead to a data flow explosion.",
                            getMetadata()
                    );
                }

                // since no variable dependency to the current FLWOR expression exists for the expression
                // evaluate the DataFrame with the parent context and calculate the cartesian product
                Dataset<Row> expressionDF;
                expressionDF = getDataFrameFromRDDExpression(context);

                String inputDFTableName = "input";
                String expressionDFTableName = "expression";

                Dataset<Row> inputDF = this._child.getDataFrame(context, getProjection(parentProjection));
                StructType inputSchema = inputDF.schema();
                int duplicateVariableIndex = Arrays.asList(inputSchema.fieldNames()).indexOf(this._variableName);
                List<String> columnsToSelect = DataFrameUtils.getColumnNames(
                    inputSchema,
                    duplicateVariableIndex,
                    null
                );

                if (duplicateVariableIndex == -1) {
                    columnsToSelect.add(this._variableName);
                } else {
                    columnsToSelect.add(expressionDFTableName + "`.`" + this._variableName);
                }
                String selectSQL = DataFrameUtils.getSQL(columnsToSelect, false);

                inputDF.createOrReplaceTempView(inputDFTableName);
                expressionDF.createOrReplaceTempView(expressionDFTableName);

                return inputDF.sparkSession()
                    .sql(
                        String.format(
                            "select %s from %s, %s",
                            selectSQL,
                            inputDFTableName,
                            expressionDFTableName
                        )
                    );
            }

            // the expression is locally evaluated
            Dataset<Row> df = this._child.getDataFrame(context, getProjection(parentProjection));
            StructType inputSchema = df.schema();
            int duplicateVariableIndex = Arrays.asList(inputSchema.fieldNames()).indexOf(this._variableName);
            List<String> allColumns = DataFrameUtils.getColumnNames(inputSchema, duplicateVariableIndex, null);
            Map<String, List<String>> UDFcolumnsByType = DataFrameUtils.getColumnNamesByType(
                inputSchema,
                -1,
                this._dependencies
            );

            df.sparkSession()
                .udf()
                .register(
                    "forClauseUDF",
                    new ForClauseUDF(this._assignmentIterator, context, UDFcolumnsByType),
                    DataTypes.createArrayType(DataTypes.BinaryType)
                );

            String selectSQL = DataFrameUtils.getSQL(allColumns, true);
            String UDFParameters = DataFrameUtils.getUDFParameters(UDFcolumnsByType);

            df.createOrReplaceTempView("input");
            df = df.sparkSession()
                .sql(
                    String.format(
                        "select %s explode(forClauseUDF(%s)) as `%s` from input",
                        selectSQL,
                        UDFParameters,
                        this._variableName
                    )
                );
            return df;
        }

        // if child is locally evaluated
        // _assignmentIterator is definitely an RDD if execution flows here
        Dataset<Row> df = null;
        this._child.open(context);
        this._tupleContext = new DynamicContext(context); // assign current context as parent
        while (this._child.hasNext()) {
            this._inputTuple = this._child.next();
            this._tupleContext.removeAllVariables(); // clear the previous variables
            this._tupleContext.setBindingsFromTuple(this._inputTuple, getMetadata()); // assign new variables from new
                                                                                      // tuple

            JavaRDD<Item> expressionRDD = this._assignmentIterator.getRDD(this._tupleContext);

            // TODO - Optimization: Iterate schema creation only once
            Set<String> oldColumnNames = this._inputTuple.getLocalKeys();
            List<String> newColumnNames = new ArrayList<>(oldColumnNames);

            newColumnNames.add(this._variableName);
            List<StructField> fields = new ArrayList<>();
            for (String columnName : newColumnNames) {
                StructField field = DataTypes.createStructField(columnName, DataTypes.BinaryType, true);
                fields.add(field);
            }
            StructType schema = DataTypes.createStructType(fields);

            JavaRDD<Row> rowRDD = expressionRDD.map(new ForClauseLocalToRowClosure(this._inputTuple, getMetadata()));

            if (df == null) {
                df = SparkSessionManager.getInstance().getOrCreateSession().createDataFrame(rowRDD, schema);
            } else {
                df = df.union(SparkSessionManager.getInstance().getOrCreateSession().createDataFrame(rowRDD, schema));
            }
        }
        this._child.close();
        return df;
    }

    private Dataset<Row> getDataFrameFromRDDExpression(DynamicContext context) {
        // create initial RDD from expression
        JavaRDD<Item> expressionRDD = this._assignmentIterator.getRDD(context);
        return getDataFrameFromItemRDD(expressionRDD);
    }

    private Dataset<Row> getDataFrameFromItemRDD(JavaRDD<Item> expressionRDD) {
        // define a schema
        List<StructField> fields = new ArrayList<>();
        StructField field = DataTypes.createStructField(this._variableName, DataTypes.BinaryType, true);
        fields.add(field);
        StructType schema = DataTypes.createStructType(fields);

        JavaRDD<Row> rowRDD = expressionRDD.map(new ForClauseSerializeClosure());

        // apply the schema to row RDD
        return SparkSessionManager.getInstance().getOrCreateSession().createDataFrame(rowRDD, schema);
    }

    public Map<String, DynamicContext.VariableDependency> getVariableDependencies() {
        Map<String, DynamicContext.VariableDependency> result =
            new TreeMap<>(this._assignmentIterator.getVariableDependencies());
        if (this._child != null) {
            for (String var : this._child.getVariablesBoundInCurrentFLWORExpression()) {
                result.remove(var);
            }
            result.putAll(this._child.getVariableDependencies());
        }
        return result;
    }

    public Set<String> getVariablesBoundInCurrentFLWORExpression() {
        Set<String> result = new HashSet<>();
        if (this._child != null) {
            result.addAll(this._child.getVariablesBoundInCurrentFLWORExpression());
        }
        result.add(this._variableName);
        return result;
    }

    public void print(StringBuffer buffer, int indent) {
        super.print(buffer, indent);
        for (int i = 0; i < indent + 1; ++i) {
            buffer.append("  ");
        }
        buffer.append("Variable ").append(this._variableName).append("\n");
        this._assignmentIterator.print(buffer, indent + 1);
    }

    public Map<String, DynamicContext.VariableDependency> getProjection(
            Map<String, DynamicContext.VariableDependency> parentProjection
    ) {
        if (this._child == null) {
            return null;
        }

        // start with an empty projection.

        // copy over the projection needed by the parent clause.
        Map<String, DynamicContext.VariableDependency> projection =
            new TreeMap<>(parentProjection);

        // remove the variable that this for clause binds.
        projection.remove(this._variableName);

        // add the variable dependencies needed by this for clause's expression.
        Map<String, DynamicContext.VariableDependency> exprDependency = this._assignmentIterator
            .getVariableDependencies();
        for (String variable : exprDependency.keySet()) {
            if (projection.containsKey(variable)) {
                if (projection.get(variable) != exprDependency.get(variable)) {
                    // If the projection already needed a different kind of dependency, we fall back to the full
                    // sequence of items.
                    projection.put(variable, DynamicContext.VariableDependency.FULL);
                }
            } else {
                projection.put(variable, exprDependency.get(variable));
            }
        }
        return projection;
    }
}
