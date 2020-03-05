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

package iq.base;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.junit.Assert;
import org.rumbledb.compiler.TranslationVisitor;
import org.rumbledb.compiler.VisitorHelpers;
import org.rumbledb.exceptions.ExceptionMetadata;
import org.rumbledb.exceptions.ParsingException;
import org.rumbledb.exceptions.SemanticException;
import org.rumbledb.exceptions.SparksoniqRuntimeException;
import org.rumbledb.expressions.module.MainModule;
import org.rumbledb.parser.JsoniqLexer;
import org.rumbledb.parser.JsoniqParser;
import org.rumbledb.runtime.RuntimeIterator;
import org.rumbledb.runtime.functions.base.Functions;

import utils.FileManager;
import utils.annotations.AnnotationParseException;
import utils.annotations.AnnotationProcessor;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AnnotationsTestsBase {
    protected static int counter = 0;
    protected AnnotationProcessor.TestAnnotation currentAnnotation;
    protected List<File> testFiles = new ArrayList<>();


    public void initializeTests(File dir) {
        FileManager.loadJiqFiles(dir).forEach(file -> this.testFiles.add(file));
        this.testFiles.sort(Comparator.comparing(File::getName));
    }

    /**
     * Tests annotations
     */
    protected MainModule testAnnotations(String path)
            throws IOException {
        RuntimeIterator runtimeIterator = null;
        try {
            this.currentAnnotation = AnnotationProcessor.readAnnotation(new FileReader(path));
        } catch (AnnotationParseException e) {
            e.printStackTrace();
            Assert.fail();
        }
        MainModule mainModule = null;
        try {
            mainModule = this.parse(path);
            Functions.clearUserDefinedFunctions(); // clear UDFs between each test run

            VisitorHelpers.populateStaticContext(mainModule);
            runtimeIterator = VisitorHelpers.generateRuntimeIterator(mainModule);
            // PARSING
        } catch (ParsingException exception) {
            String errorOutput = exception.getMessage();
            checkErrorCode(
                errorOutput,
                this.currentAnnotation.getErrorCode(),
                this.currentAnnotation.getErrorMetadata()
            );
            if (this.currentAnnotation.shouldParse()) {
                Assert.fail("Program did not parse when expected to.\nError output: " + errorOutput + "\n");
                return mainModule;
            } else {
                System.out.println(errorOutput);
                return mainModule;
            }

            // SEMANTIC
        } catch (SemanticException exception) {
            String errorOutput = exception.getMessage();
            checkErrorCode(
                errorOutput,
                this.currentAnnotation.getErrorCode(),
                this.currentAnnotation.getErrorMetadata()
            );
            try {
                if (this.currentAnnotation.shouldCompile()) {
                    Assert.fail("Program did not compile when expected to.\nError output: " + errorOutput + "\n");
                    return mainModule;
                } else {
                    System.out.println(errorOutput);
                    Assert.assertTrue(true);
                    return mainModule;
                }
            } catch (Exception ex) {
            }

            // RUNTIME
        } catch (SparksoniqRuntimeException exception) {
            String errorOutput = exception.getMessage();
            checkErrorCode(
                errorOutput,
                this.currentAnnotation.getErrorCode(),
                this.currentAnnotation.getErrorMetadata()
            );
            try {
                if (this.currentAnnotation.shouldRun()) {
                    Assert.fail("Program did not run when expected to.\nError output: " + errorOutput + "\n");
                    return mainModule;
                } else {
                    System.out.println(errorOutput);
                    Assert.assertTrue(true);
                    return mainModule;
                }
            } catch (Exception ex) {
            }
        }

        try {
            if (!this.currentAnnotation.shouldCompile()) {
                Assert.fail("Program compiled when not expected to.\n");
                return mainModule;
            }
        } catch (Exception ex) {
        }

        if (!this.currentAnnotation.shouldParse()) {
            Assert.fail("Program parsed when not expected to.\n");
            return mainModule;
        }

        // PROGRAM SHOULD RUN
        if (
            this.currentAnnotation instanceof AnnotationProcessor.RunnableTestAnnotation
                &&
                this.currentAnnotation.shouldRun()
        ) {
            try {
                checkExpectedOutput(this.currentAnnotation.getOutput(), runtimeIterator);
            } catch (SparksoniqRuntimeException exception) {
                String errorOutput = exception.getMessage();
                exception.printStackTrace();
                Assert.fail("Program did not run when expected to.\nError output: " + errorOutput + "\n");
            }
        } else {
            // PROGRAM SHOULD CRASH
            if (
                this.currentAnnotation instanceof AnnotationProcessor.UnrunnableTestAnnotation
                    &&
                    !this.currentAnnotation.shouldRun()
            ) {
                try {
                    checkExpectedOutput(this.currentAnnotation.getOutput(), runtimeIterator);
                } catch (Exception exception) {
                    String errorOutput = exception.getMessage();
                    checkErrorCode(
                        errorOutput,
                        this.currentAnnotation.getErrorCode(),
                        this.currentAnnotation.getErrorMetadata()
                    );
                    return mainModule;
                }

                Assert.fail("Program executed when not expected to");
            }
        }
        return mainModule;
    }

    private MainModule parse(String path) throws IOException {
        JsoniqLexer lexer = new JsoniqLexer(CharStreams.fromFileName(path));
        JsoniqParser parser = new JsoniqParser(new CommonTokenStream(lexer));
        parser.setErrorHandler(new BailErrorStrategy());

        try {
            // the original
            /*
             * JsoniqParser.ModuleContext unit = parser.module();
             * JsoniqParser.MainModuleContext main = unit.main;
             * visitor.visit(unit);
             */

            JsoniqParser.ModuleContext module = parser.module();
            JsoniqParser.MainModuleContext main = module.main;
            MainModule mainModule = (MainModule) new TranslationVisitor().visit(main);
            return mainModule;
        } catch (ParseCancellationException ex) {
            ParsingException e = new ParsingException(
                    lexer.getText(),
                    new ExceptionMetadata(
                            lexer.getLine(),
                            lexer.getCharPositionInLine()
                    )
            );
            e.initCause(ex);
            throw e;
        }
    }

    protected void checkExpectedOutput(String expectedOutput, RuntimeIterator runtimeIterator) {
        Assert.assertTrue(true);
    }

    protected void checkErrorCode(String errorOutput, String expectedErrorCode, String errorMetadata) {
        if (errorOutput != null && expectedErrorCode != null)
            Assert.assertTrue(
                "Unexpected error code returned; Expected: "
                    + expectedErrorCode
                    +
                    "; Error: "
                    + errorOutput,
                errorOutput.contains(expectedErrorCode)
            );
        if (errorOutput != null && errorMetadata != null)
            Assert.assertTrue(
                "Unexpected metadata returned; Expected: "
                    + errorMetadata
                    +
                    "; Error: "
                    + errorOutput,
                errorOutput.contains(errorMetadata)
            );
    }
}
