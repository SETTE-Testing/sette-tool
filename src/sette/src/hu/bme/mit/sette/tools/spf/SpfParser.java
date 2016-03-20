/*
 * SETTE - Symbolic Execution based Test Tool Evaluator
 *
 * SETTE is a tool to help the evaluation and comparison of symbolic execution based test input 
 * generator tools.
 *
 * Budapest University of Technology and Economics (BME)
 *
 * Authors: Lajos Cseppentő <lajos.cseppento@inf.mit.bme.hu>, Zoltán Micskei <micskeiz@mit.bme.hu>
 *
 * Copyright 2014-2016
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except 
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the 
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either 
 * express or implied. See the License for the specific language governing permissions and 
 * limitations under the License.
 */
// NOTE revise this file
package hu.bme.mit.sette.tools.spf;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.google.common.primitives.Primitives;

import hu.bme.mit.sette.core.model.parserxml.InputElement;
import hu.bme.mit.sette.core.model.parserxml.ParameterElement;
import hu.bme.mit.sette.core.model.parserxml.SnippetInputsXml;
import hu.bme.mit.sette.core.model.runner.ParameterType;
import hu.bme.mit.sette.core.model.runner.ResultType;
import hu.bme.mit.sette.core.model.snippet.Snippet;
import hu.bme.mit.sette.core.model.snippet.SnippetProject;
import hu.bme.mit.sette.core.tasks.RunResultParser;

public class SpfParser extends RunResultParser<SpfTool> {
    public SpfParser(SnippetProject snippetProject, Path outputDir, SpfTool tool,
            String runnerProjectTag) {
        super(snippetProject, outputDir, tool, runnerProjectTag);
    }

    @Override
    protected void parseSnippet(Snippet snippet, SnippetOutFiles outFiles,
            SnippetInputsXml inputsXml) throws Exception {
        List<String> outputLines = outFiles.readOutputLines();
        List<String> errorLines = outFiles.readErrorOutputLines();

        if (!errorLines.isEmpty()) {
            // TODO make this section simple and clear
            String firstLine = errorLines.get(0);

            if (firstLine
                    .startsWith("java.lang.RuntimeException: ## Error: Operation not supported!")) {
                inputsXml.setResultType(ResultType.NA);
            } else if (firstLine.startsWith("java.lang.NullPointerException")) {
                inputsXml.setResultType(ResultType.EX);
            } else if (firstLine.startsWith(
                    "java.lang.RuntimeException: ## Error: symbolic log10 not implemented")) {
                inputsXml.setResultType(ResultType.NA);
            } else if (firstLine.startsWith("***********Warning: everything false")) {
                // TODO enhance
                // now skip
            } else if (firstLine.startsWith(
                    "java.lang.RuntimeException: ERROR: you need to turn debug option on")) {
                // TODO JPF/SPF compilation differences between javac and ecj:
                // https://groups.google.com/forum/#!topic/java-pathfinder/jhOkvLx-SKE
                // now just accept

                // constant() and always() exception snippets (void return value and no parameters
                // -> useless methods)
            } else if (firstLine
                    .startsWith("java.lang.RuntimeException: Arrays: symbolic index not handled")) {
                inputsXml.setResultType(ResultType.NA);
            } else {
                // TODO error handling

                // this is debug (only if unhandled error)
                System.err.println("=============================");
                System.err.println(snippet.getMethod());
                System.err.println("== ERROR OUTPUT =============");

                for (String line : errorLines) {
                    System.err.println(line);
                }
                System.err.println("=============================");

                // TODO error handling
                throw new RuntimeException("PARSER PROBLEM, UNHANDLED ERROR");
            }
        }

        if (inputsXml.getResultType() == null) {
            // TODO enhance
            inputsXml.setResultType(ResultType.S);

            // if (snippet.getMethod().toString().contains("_Constants")
            // || snippet.getMethod().toString().contains(".always()")) {
            // // TODO JPF/SPF compilation differences between javac and ecj:
            // // https://groups.google.com/forum/#!topic/java-pathfinder/jhOkvLx-SKE
            // // now just accept
            //
            // // no inputs for constant tests, just call them once
            // inputsXml.getGeneratedInputs().add(new InputElement());
            // } else {
            Iterator<String> lines = outputLines.iterator();

            // find input lines

            List<String> inputLines = new ArrayList<>();
            boolean shouldCollect = false;
            while (lines.hasNext()) {
                String line = lines.next();
                if (line.trim().equals(
                        "====================================================== Method Summaries")) {
                    shouldCollect = true;
                } else if (shouldCollect) {
                    if (line.startsWith("======================================================")) {
                        // start of next section
                        shouldCollect = false;
                        break;
                    } else {
                        if (!StringUtils.isBlank(line)) {
                            inputLines.add(line.trim());
                        }
                    }
                }
            }

            // remove duplicates
            inputLines = new ArrayList<>(new LinkedHashSet<>(inputLines));

            if (!inputLines.isEmpty()) {
                String firstLine = inputLines.get(0);
                if (!firstLine.startsWith("Inputs:")) {
                    throw new RuntimeException("Cannot parse: " + outFiles.outputFile);
                }

                firstLine = firstLine.substring(7).trim();
                String[] parameterStrings = StringUtils.split(firstLine, ',');
                ParameterType[] parameterTypes = new ParameterType[parameterStrings.length];

                if (inputLines.size() == 2
                        && inputLines.get(1).startsWith("No path conditions for")) {
                    InputElement input = new InputElement();
                    for (int i = 0; i < parameterStrings.length; i++) {
                        // no path conditions, only considering the "default"
                        // inputs
                        Class<?> type = snippet.getMethod().getParameterTypes()[i];
                        parameterTypes[i] = getParameterType(type);
                        input.getParameters().add(new ParameterElement(parameterTypes[i],
                                getDefaultParameterValueString(type)));
                    }
                    inputsXml.getGeneratedInputs().add(input);
                } else {
                    // parse parameter types

                    Class<?>[] paramsJavaClass = snippet.getMethod().getParameterTypes();

                    for (int i = 0; i < parameterStrings.length; i++) {
                        String parameterString = parameterStrings[i];
                        Class<?> pjc = Primitives.wrap(paramsJavaClass[i]);

                        if (parameterString.endsWith("SYMINT")) {
                            if (pjc == Boolean.class) {
                                parameterTypes[i] = ParameterType.BOOLEAN;
                            } else if (pjc == Byte.class) {
                                parameterTypes[i] = ParameterType.BYTE;
                            } else if (pjc == Short.class) {
                                parameterTypes[i] = ParameterType.SHORT;
                            } else if (pjc == Integer.class) {
                                parameterTypes[i] = ParameterType.INT;
                            } else if (pjc == Long.class) {
                                parameterTypes[i] = ParameterType.LONG;
                            } else {
                                // int for something else
                                parameterTypes[i] = ParameterType.INT;
                            }
                        } else if (parameterString.endsWith("SYMREAL")) {
                            if (pjc == Float.class) {
                                parameterTypes[i] = ParameterType.FLOAT;
                            } else if (pjc == Float.class) {
                                parameterTypes[i] = ParameterType.DOUBLE;
                            } else {
                                // int for something else
                                parameterTypes[i] = ParameterType.DOUBLE;
                            }
                        } else if (parameterString.endsWith("SYMSTRING")) {
                            parameterTypes[i] = ParameterType.EXPRESSION;
                        } else {
                            // TODO error handling
                            // int for something else
                            System.out.println(snippet.getMethod());
                            System.err.println(parameterString);
                            throw new RuntimeException("PARSER PROBLEM");
                        }
                    }

                    // example
                    // inheritsAPIGuessTwoPrimitives(11,-2147483648(don't care))
                    // -->
                    // "java.lang.IllegalArgumentException..."
                    // inheritsAPIGuessTwoPrimitives(9,11) -->
                    // "java.lang.IllegalArgumentException..."
                    // inheritsAPIGuessTwoPrimitives(7,9) -->
                    // "java.lang.RuntimeException: Out of range..."
                    // inheritsAPIGuessTwoPrimitives(4,1) --> Return Value: 1
                    // inheritsAPIGuessTwoPrimitives(0,0) --> Return Value: 0
                    // inheritsAPIGuessTwoPrimitives(9,-88) -->
                    // "java.lang.IllegalArgumentException..."
                    // inheritsAPIGuessTwoPrimitives(-88,-2147483648(don't
                    // care))
                    // --> "java.lang.IllegalArgumentException..."

                    String ps = String.format("^%s\\((.*)\\)\\s+-->\\s+(.*)$",
                            snippet.getMethod().getName());

                    // ps = String.format("^%s(.*)\\s+-->\\s+(.*)$",
                    // snippet.getMethod()
                    // .getName());
                    ps = String.format("^(%s\\.)?%s(.*)\\s+-->\\s+(.*)$",
                            snippet.getContainer().getJavaClass().getName(),
                            snippet.getMethod().getName());
                    Pattern p = Pattern.compile(ps);

                    // parse inputs
                    int i = -1;
                    for (String line : inputLines) {
                        i++;

                        if (i == 0) {
                            // first line
                            continue;
                        } else if (StringUtils.isEmpty(line)) {
                            continue;
                        }

                        Matcher m = p.matcher(line);

                        if (m.matches()) {
                            String paramsString = StringUtils.substring(m.group(2).trim(), 1, -1);
                            String resultString = m.group(3).trim();

                            paramsString = StringUtils.replace(paramsString, "(don't care)", "");

                            String[] paramsStrings = StringUtils.split(paramsString, ',');

                            InputElement input = new InputElement();

                            // if index error -> lesser inputs than parameters
                            for (int j = 0; j < parameterTypes.length; j++) {
                                if (parameterTypes[j] == ParameterType.BOOLEAN
                                        && paramsStrings[j].contains("-2147483648")) {
                                    // don't care -> 0
                                    paramsStrings[j] = "false";
                                }

                                ParameterElement pe = new ParameterElement(parameterTypes[j],
                                        paramsStrings[j].trim());

                                try {
                                    // just check the type format
                                    pe.validate();
                                } catch (Exception ex) {
                                    // TODO error handling
                                    System.out.println(parameterTypes[j]);
                                    System.out.println(paramsStrings[j]);
                                    System.out.println(pe.getType());
                                    System.out.println(pe.getValue());
                                    ex.printStackTrace();

                                    System.err.println("=============================");
                                    System.err.println(snippet.getMethod());
                                    System.err.println("=============================");
                                    for (String lll : inputLines) {
                                        System.err.println(lll);
                                    }
                                    System.err.println("=============================");

                                    throw new RuntimeException(ex);
                                }

                                input.getParameters().add(pe);
                            }

                            if (resultString.startsWith("Return Value:")) {
                                // has retval, nothing to do
                            } else {
                                // exception; example (" is present inside the
                                // string!!!):
                                // "java.lang.ArithmeticException: div by 0..."
                                // "java.lang.IndexOutOfBoundsException: Index: 1, Size: 5..."

                                int pos = resultString.indexOf(':');
                                if (pos < 0) {
                                    // not found :, search for ...
                                    pos = resultString.indexOf("...");
                                }

                                String ex = resultString.substring(1, pos);
                                input.setExpected(ex);

                                // System.err.println(resultString);
                                // System.err.println(ex);
                                // // input.setExpected(expected);
                            }

                            inputsXml.getGeneratedInputs().add(input);
                        } else {
                            System.err.println("NO MATCH");
                            System.err.println(snippet.getMethod());
                            System.err.println(ps);
                            System.err.println(line);
                            throw new Exception("NO MATCH: " + line);
                        }
                    }
                }
            }

            // inputsXml.getGeneratedInputs().add(new InputElement());
            // }
            inputsXml.validate();
        }
    }
}
