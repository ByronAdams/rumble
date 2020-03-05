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

package org.rumbledb.expressions.operational.base;

import org.antlr.v4.runtime.Token;
import org.rumbledb.api.Item;
import org.rumbledb.exceptions.ExceptionMetadata;
import org.rumbledb.exceptions.OurBadException;
import org.rumbledb.expressions.Expression;
import org.rumbledb.expressions.Node;
import org.rumbledb.items.ItemFactory;
import java.util.ArrayList;
import java.util.List;


public abstract class OperationalExpressionBase extends Expression {

    protected boolean isActive;
    protected Expression mainExpression;
    protected List<Operator> multipleOperators;
    protected Operator singleOperator;


    protected OperationalExpressionBase(
            Expression mainExpression,
            Operator op,
            ExceptionMetadata metadata
    ) {
        super(metadata);
        this.mainExpression = mainExpression;
        this.singleOperator = op;

    }

    protected OperationalExpressionBase(
            Expression mainExpression,
            List<Operator> ops,
            ExceptionMetadata metadata
    ) {
        super(metadata);
        this.mainExpression = mainExpression;
        this.multipleOperators = ops;
    }

    public static List<Operator> getOperatorFromOpList(List<Token> ops) {
        List<Operator> result = new ArrayList<>();
        ops.forEach(op -> result.add(getOperatorFromString(op.getText())));
        return result;
    }

    public static Operator getOperatorFromString(String token) {
        token = token.toUpperCase();
        switch (token) {
            case "EQ":
                return Operator.VC_EQ;
            case "NE":
                return Operator.VC_NE;
            case "LT":
                return Operator.VC_LT;
            case "LE":
                return Operator.VC_LE;
            case "GT":
                return Operator.VC_GT;
            case "GE":
                return Operator.VC_GE;

            case "=":
                return Operator.GC_EQ;
            case "!=":
                return Operator.GC_NE;
            case "<":
                return Operator.GC_LT;
            case "<=":
                return Operator.GC_LE;
            case ">":
                return Operator.GC_GT;
            case ">=":
                return Operator.GC_GE;

            case "+":
                return Operator.PLUS;
            case "-":
                return Operator.MINUS;

            case "*":
                return Operator.MUL;
            case "DIV":
                return Operator.DIV;
            case "IDIV":
                return Operator.IDIV;
            case "MOD":
                return Operator.MOD;

            case "TO":
                return Operator.TO;
            case "||":
                return Operator.CONCAT;
            case "INSTANCE OF":
                return Operator.INSTANCE_OF;
            case "TREAT":
                return Operator.TREAT;
            case "CAST":
                return Operator.CAST;
        }

        throw new OurBadException("Operator not recognized.");
    }

    public static String getStringFromOperator(Operator operator) {
        switch (operator) {

            case PLUS:
                return "+";
            case MINUS:
                return "-";
            case MUL:
                return "*";
            case CONCAT:
                return "||";
            case INSTANCE_OF:
                return "instance of";
            case TREAT:
                return "treat as";
            case CAST:
                return "cast as";
            default:
                return operator.toString().toLowerCase();
        }
    }

    public void validateOperators(List<Operator> validOps, List<Operator> ops) {
        for (Operator op : ops) {
            if (!validOps.contains(op)) {
                throw new IllegalArgumentException("Operational operators exception");
            }
        }
    }

    public void validateOperator(List<Operator> validOps, Operator op) {
        if (!validOps.contains(op)) {
            throw new IllegalArgumentException("Operational operators exception");
        }
    }

    public Expression getMainExpression() {
        return this.mainExpression;
    }

    @Override
    public List<Node> getChildren() {
        List<Node> result = new ArrayList<>();
        if (this.mainExpression != null) {
            result.add(this.mainExpression);
        }
        return result;
    }

    public enum Operator {
        OR,
        AND,
        NOT,

        // Value Comparison -- 0 or 1 item with compatible types are compared
        VC_EQ {
            @Override
            public Item apply(Item left, Item right) {
                int comparison = left.compareTo(right);
                return ItemFactory.getInstance().createBooleanItem(comparison == 0);
            }
        },
        VC_NE {
            @Override
            public Item apply(Item left, Item right) {
                int comparison = left.compareTo(right);
                return ItemFactory.getInstance().createBooleanItem(comparison != 0);
            }
        },
        VC_LT {
            @Override
            public Item apply(Item left, Item right) {
                int comparison = left.compareTo(right);
                return ItemFactory.getInstance().createBooleanItem(comparison < 0);
            }
        },
        VC_LE {
            @Override
            public Item apply(Item left, Item right) {
                int comparison = left.compareTo(right);
                return ItemFactory.getInstance().createBooleanItem(comparison < 0 || comparison == 0);
            }
        },
        VC_GT {
            @Override
            public Item apply(Item left, Item right) {
                int comparison = left.compareTo(right);
                return ItemFactory.getInstance().createBooleanItem(comparison > 0);
            }
        },
        VC_GE {
            @Override
            public Item apply(Item left, Item right) {
                int comparison = left.compareTo(right);
                return ItemFactory.getInstance().createBooleanItem(comparison > 0 || comparison == 0);
            }
        },

        // general Comparison -- sequences are compared
        GC_EQ {
            @Override
            public Item apply(Item left, Item right) {
                int comparison = left.compareTo(right);
                return ItemFactory.getInstance().createBooleanItem(comparison == 0);
            }
        },
        GC_NE {
            @Override
            public Item apply(Item left, Item right) {
                int comparison = left.compareTo(right);
                return ItemFactory.getInstance().createBooleanItem(comparison != 0);
            }
        },
        GC_LT {
            @Override
            public Item apply(Item left, Item right) {
                int comparison = left.compareTo(right);
                return ItemFactory.getInstance().createBooleanItem(comparison < 0);
            }
        },
        GC_LE {
            @Override
            public Item apply(Item left, Item right) {
                int comparison = left.compareTo(right);
                return ItemFactory.getInstance().createBooleanItem(comparison < 0 || comparison == 0);
            }
        },
        GC_GT {
            @Override
            public Item apply(Item left, Item right) {
                int comparison = left.compareTo(right);
                return ItemFactory.getInstance().createBooleanItem(comparison > 0);
            }
        },
        GC_GE {
            @Override
            public Item apply(Item left, Item right) {
                int comparison = left.compareTo(right);
                return ItemFactory.getInstance().createBooleanItem(comparison > 0 || comparison == 0);
            }
        },

        PLUS,
        MINUS,
        MUL,
        DIV,
        MOD,
        IDIV,

        TO,
        CONCAT,
        INSTANCE_OF,
        COMMA,
        TREAT,
        CAST;

        public Item apply(Item left, Item right) {
            return null;
        }

    }
}
