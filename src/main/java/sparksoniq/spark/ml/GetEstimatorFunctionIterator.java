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

package sparksoniq.spark.ml;

import org.apache.spark.ml.Estimator;
import org.rumbledb.api.Item;
import org.rumbledb.exceptions.ExceptionMetadata;
import org.rumbledb.exceptions.IteratorFlowException;
import org.rumbledb.exceptions.OurBadException;
import org.rumbledb.exceptions.UnexpectedTypeException;
import sparksoniq.jsoniq.ExecutionMode;
import sparksoniq.jsoniq.item.FunctionItem;
import sparksoniq.jsoniq.runtime.iterator.RuntimeIterator;
import sparksoniq.jsoniq.runtime.iterator.functions.base.FunctionIdentifier;
import sparksoniq.jsoniq.runtime.iterator.functions.base.FunctionSignature;
import sparksoniq.jsoniq.runtime.iterator.functions.base.LocalFunctionCallIterator;
import sparksoniq.semantics.DynamicContext;
import sparksoniq.semantics.types.ItemType;
import sparksoniq.semantics.types.ItemTypes;
import sparksoniq.semantics.types.SequenceType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GetEstimatorFunctionIterator extends LocalFunctionCallIterator {

    private static final long serialVersionUID = 1L;
    public static final List<String> estimatorParameterNames = new ArrayList<>(Arrays.asList("input", "params"));
    private String _estimatorShortName;
    private Class<?> _estimatorSparkMLClass;

    public GetEstimatorFunctionIterator(
            List<RuntimeIterator> arguments,
            ExecutionMode executionMode,
            ExceptionMetadata metadata
    ) {
        super(arguments, executionMode, metadata);
    }

    @Override
    public void open(DynamicContext context) {
        super.open(context);

        RuntimeIterator nameIterator = this._children.get(0);
        nameIterator.open(context);
        if (!nameIterator.hasNext()) {
            throw new UnexpectedTypeException(
                    "Invalid args. Estimator lookup can't be performed with empty sequence as the transformer name",
                    getMetadata()
            );
        }
        this._estimatorShortName = nameIterator.next().getStringValue();
        if (nameIterator.hasNext()) {
            throw new UnexpectedTypeException(
                    "Estimator lookup can't be performed on a sequence.",
                    getMetadata()
            );
        }
        nameIterator.close();

        String estimatorFullClassName = RumbleMLCatalog.getEstimatorFullClassName(
            this._estimatorShortName,
            getMetadata()
        );
        try {
            this._estimatorSparkMLClass = Class.forName(estimatorFullClassName);
            this._hasNext = true;
        } catch (ClassNotFoundException e) {
            throw new OurBadException(
                    "No SparkML estimator implementation found with the given full class name."
            );
        }
    }

    @Override
    public Item next() {
        if (this._hasNext) {
            this._hasNext = false;
            try {
                Estimator<?> estimator = (Estimator<?>) this._estimatorSparkMLClass.newInstance();
                RuntimeIterator bodyIterator = new ApplyEstimatorRuntimeIterator(
                        this._estimatorShortName,
                        estimator,
                        ExecutionMode.LOCAL,
                        getMetadata()
                );
                List<SequenceType> paramTypes = Collections.unmodifiableList(
                    Arrays.asList(
                        new SequenceType(
                                new ItemType(ItemTypes.Item), // TODO: revert back to ObjectItem
                                SequenceType.Arity.ZeroOrMore
                        ),
                        new SequenceType(
                                new ItemType(ItemTypes.ObjectItem),
                                SequenceType.Arity.One
                        )
                    )
                );
                SequenceType returnType = new SequenceType(
                        new ItemType(ItemTypes.FunctionItem),
                        SequenceType.Arity.One
                );

                return new FunctionItem(
                        new FunctionIdentifier(this._estimatorSparkMLClass.getName(), 2),
                        estimatorParameterNames,
                        new FunctionSignature(
                                paramTypes,
                                returnType
                        ),
                        bodyIterator
                );

            } catch (InstantiationException | IllegalAccessException e) {
                throw new OurBadException("Error while generating an instance from transformer class.", getMetadata());
            }
        }
        throw new IteratorFlowException(
                RuntimeIterator.FLOW_EXCEPTION_MESSAGE + "get-transformer function",
                getMetadata()
        );
    }
}
