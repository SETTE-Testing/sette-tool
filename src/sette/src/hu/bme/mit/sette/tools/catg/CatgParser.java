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
 * Copyright 2014-2015
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
// TODO z revise this file
package hu.bme.mit.sette.tools.catg;

import hu.bme.mit.sette.common.model.parserxml.InputElement;
import hu.bme.mit.sette.common.model.parserxml.ParameterElement;
import hu.bme.mit.sette.common.model.parserxml.SnippetInputsXml;
import hu.bme.mit.sette.common.model.runner.ParameterType;
import hu.bme.mit.sette.common.model.runner.ResultType;
import hu.bme.mit.sette.common.model.runner.RunnerProjectUtils;
import hu.bme.mit.sette.common.model.snippet.Snippet;
import hu.bme.mit.sette.common.model.snippet.SnippetProject;
import hu.bme.mit.sette.common.tasks.RunResultParser;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

public class CatgParser extends RunResultParser<CatgTool> {
    public CatgParser(SnippetProject snippetProject, File outputDirectory, CatgTool tool) {
        super(snippetProject, outputDirectory, tool);
    }

    @Override
    protected void parseSnippet(Snippet snippet, SnippetInputsXml inputsXml) throws Exception {
        File outputFile = RunnerProjectUtils.getSnippetOutputFile(getRunnerProjectSettings(),
                snippet);
        File errorFile = RunnerProjectUtils.getSnippetErrorFile(getRunnerProjectSettings(),
                snippet);

        if (errorFile.exists()) {
            // TODO enhance this section and make it more clear

            List<String> lines = FileUtils.readLines(errorFile);

            String firstLine = lines.get(0);

            if (firstLine.startsWith("Exception in thread \"main\"")) {
                Pattern p = Pattern.compile("Exception in thread \"main\" ([a-z0-9\\._]+).*",
                        Pattern.CASE_INSENSITIVE);

                Matcher m = p.matcher(firstLine);
                if (m.matches()) {
                    String exceptionType = m.group(1);

                    if (exceptionType.equals("java.lang.NoClassDefFoundError")) {
                        inputsXml.setResultType(ResultType.NA);
                    } else if (exceptionType.equals("java.lang.VerifyError")) {
                        inputsXml.setResultType(ResultType.EX);
                    } else if (exceptionType.endsWith("Exception")) {
                        // enhance
                        inputsXml.setResultType(ResultType.EX);
                    } else {
                        System.err.println(snippet.getMethod());
                        System.err.println("NOT HANDLED TYPE: " + exceptionType);
                    }
                } else {
                    System.err.println(snippet.getMethod());
                    System.err.println("NO MATCH");
                }
            } else if (firstLine.startsWith("java.lang.ArrayIndexOutOfBoundsException")) {
                inputsXml.setResultType(ResultType.EX);
            } else if (firstLine
                    .startsWith("WARNING: !!!!!!!!!!!!!!!!! Prediction failed !!!!!!!!!!!!!!!!!")) {
                // TODO enhance (it was just warning)
                inputsXml.setResultType(ResultType.S);
            }

            // if (firstLine
            // .startsWith("Exception in thread \"main\" java.lang.NoClassDefFoundError"))
            // {
            // inputsXml.setResultType(ResultType.NA);
            // return;
            // } else if (firstLine
            // .startsWith("Exception in thread \"main\"
            // java.lang.StringIndexOutOfBoundsException"))
            // {
            // inputsXml.setResultType(ResultType.EX);
            // return;
            // }// else if(firstLine.startsWith("))

            // TODO enhance error message

            // this is debug (only if unhandled error)
            if (inputsXml.getResultType() == null) {
                System.err.println("=============================");
                System.err.println(snippet.getMethod());
                System.err.println("=============================");

                for (String line : lines) {
                    System.err.println(line);
                }
                System.err.println("=============================");
            }
        } else {
            // TODO enhance this section
            inputsXml.setResultType(ResultType.S);

            // collect inputs
            List<String> lines = FileUtils.readLines(outputFile);
            if (!lines.get(0).startsWith("Now testing ")) {
                throw new RuntimeException("File beginning problem: " + outputFile);
            }

            Pattern p = Pattern.compile("\\[Input (\\d+)\\]");
            Pattern p2 = Pattern.compile("  (\\w+) param(\\d+) = (.*)");

            int inputNumber = 1;

            for (int i = 1; i < lines.size(); inputNumber++) {
                String line = lines.get(i);

                Matcher m = p.matcher(line);
                if (m.matches()) {
                    if (!m.group(1).equals(String.valueOf(inputNumber))) {
                        System.err.println("Current input should be: " + inputNumber);
                        System.err.println("Current input line: " + line);
                        throw new RuntimeException(
                                "File input problem (" + line + "): " + outputFile);
                    }

                    // find end of generated input
                    int nextInputLine = -1;
                    for (int j = i + 1; j < lines.size(); j++) {
                        if (p.matcher(lines.get(j)).matches()) {
                            nextInputLine = j;
                            break;
                        }
                    }

                    if (nextInputLine < 0) {
                        // EOF
                        nextInputLine = lines.size();
                    }

                    int paramCount = snippet.getMethod().getParameterTypes().length;

                    InputElement ie = new InputElement();

                    for (int j = 0; j < paramCount; j++) {
                        String l = lines.get(i + 1 + j + 1);
                        Matcher m2 = p2.matcher(l);

                        if (m2.matches()) {
                            String type = m2.group(1);
                            String value = m2.group(3);

                            ParameterElement pe = new ParameterElement();

                            if (type.equals("String")) {
                                pe.setType(ParameterType.EXPRESSION);
                                pe.setValue("\"" + value + "\"");
                            } else {
                                pe.setType(ParameterType.fromString(type));
                                pe.setValue(value);
                            }

                            ie.getParameters().add(pe);
                        } else {
                            throw new RuntimeException(
                                    "File input problem (" + l + "): " + outputFile);
                        }
                    }

                    inputsXml.getGeneratedInputs().add(ie);

                    i = nextInputLine;

                    // TODO now NOT dealing with result and exception
                } else {
                    throw new RuntimeException("File input problem (" + line + "): " + outputFile);
                }
            }
        }

    }
}
