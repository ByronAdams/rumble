package org.rumbledb.compiler;

import org.rumbledb.exceptions.DuplicateFunctionIdentifierException;
import org.rumbledb.exceptions.OurBadException;
import org.rumbledb.expressions.Node;
import org.rumbledb.runtime.RuntimeIterator;
import org.rumbledb.runtime.functions.base.FunctionIdentifier;
import org.rumbledb.runtime.functions.base.Functions;
import sparksoniq.jsoniq.ExecutionMode;

public class VisitorHelpers {

    public static RuntimeIterator generateRuntimeIterator(Node node) {
        return new RuntimeIteratorVisitor().visit(node, null);
    }

    public static void populateStaticContext(Node node) {
        StaticContextVisitor visitor = new StaticContextVisitor();
        visitor.visit(node, null);

        visitor.setConfigForIntermediatePasses();
        int prevUnsetCount = Functions.getCurrentUnsetUserDefinedFunctionIdentifiers().size();
        while (true) {
            visitor.visit(node, null);
            int currentUnsetCount = Functions.getCurrentUnsetUserDefinedFunctionIdentifiers().size();

            if (currentUnsetCount > prevUnsetCount) {
                throw new OurBadException(
                        "Unexpected program state reached while performing multi-pass over StaticContext."
                );
            }
            if (currentUnsetCount == prevUnsetCount) {
                setLocalExecutionForUnsetUserDefinedFunctions();
                break;
            }
            prevUnsetCount = currentUnsetCount;
        }

        visitor.setConfigForFinalPass();
        visitor.visit(node, null);
    }

    private static void setLocalExecutionForUnsetUserDefinedFunctions() {
        try {
            for (
                FunctionIdentifier functionIdentifier : Functions
                    .getCurrentUnsetUserDefinedFunctionIdentifiers()
            ) {
                Functions.addUserDefinedFunctionExecutionMode(
                    functionIdentifier,
                    ExecutionMode.LOCAL,
                    true,
                    null
                );
            }
        } catch (DuplicateFunctionIdentifierException e) {
            throw new OurBadException(
                    "Unexpected program state reached while setting local execution for unset user defined functions."
            );
        }
    }
}
