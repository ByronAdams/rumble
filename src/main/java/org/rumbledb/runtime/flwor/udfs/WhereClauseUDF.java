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
 * Authors: Stefan Irimescu, Can Berker Cikis, Ghislain Fourny
 *
 */

package org.rumbledb.runtime.flwor.udfs;

import org.apache.spark.sql.api.java.UDF2;
import org.rumbledb.context.DynamicContext;
import org.rumbledb.runtime.RuntimeIterator;
import scala.collection.mutable.WrappedArray;

import java.util.List;
import java.util.Map;

public class WhereClauseUDF implements UDF2<WrappedArray<byte[]>, WrappedArray<Long>, Boolean> {
    private static final long serialVersionUID = 1L;

    private DataFrameContext dataFrameContext;
    private RuntimeIterator expression;

    public WhereClauseUDF(
            RuntimeIterator expression,
            DynamicContext context,
            Map<String, List<String>> columnNamesByType
    ) {
        this.dataFrameContext = new DataFrameContext(context, columnNamesByType);
        this.expression = expression;
    }

    @Override
    public Boolean call(WrappedArray<byte[]> wrappedParameters, WrappedArray<Long> wrappedParametersLong) {
        this.dataFrameContext.setFromWrappedParameters(wrappedParameters, wrappedParametersLong);

        this.expression.open(this.dataFrameContext.getContext());
        boolean result = RuntimeIterator.getEffectiveBooleanValue(this.expression);
        this.expression.close();
        return result;
    }
}
