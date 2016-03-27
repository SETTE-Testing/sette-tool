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
package hu.bme.mit.sette.tools.catg;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import hu.bme.mit.sette.core.model.parserxml.InputElement;
import hu.bme.mit.sette.core.model.parserxml.ParameterElement;
import hu.bme.mit.sette.core.model.parserxml.SnippetInputsXml;
import hu.bme.mit.sette.core.model.runner.ParameterType;
import hu.bme.mit.sette.core.model.runner.ResultType;
import hu.bme.mit.sette.core.model.snippet.Snippet;
import hu.bme.mit.sette.core.model.snippet.SnippetProject;
import hu.bme.mit.sette.core.tasks.RunResultParser;

public class CatgParser extends RunResultParser<CatgTool> {
    private static final Pattern EXCEPTION_LINE_PATTERN;
    private static Pattern[] ACCEPTED_ERROR_LINE_PATTERNS;

    static {
        EXCEPTION_LINE_PATTERN = Pattern
                .compile("Exception in thread \"main\" ([0-9A-Za-z\\._]+).*");

        Stream<String> patterns = Stream.of(
                "^WARNING: [!]{17} Prediction failed [!]{17} index \\d+ history\\.size\\(\\) \\d+$",
                "^WARNING: At old iid \\d+ at iid \\d+ constraint .* at iid \\d+ and index \\d+$");
        ACCEPTED_ERROR_LINE_PATTERNS = patterns.map(p -> Pattern.compile(p))
                .toArray(Pattern[]::new);
    }

    public CatgParser(SnippetProject snippetProject, Path outputDir, CatgTool tool,
            String runnerProjectTag) {
        super(snippetProject, outputDir, tool, runnerProjectTag);
    }

    @Override
    protected void parseSnippet(Snippet snippet, SnippetOutFiles outFiles,
            SnippetInputsXml inputsXml) throws Exception {
        List<String> outputLines = outFiles.readOutputLines();
        List<String> errorLines = outFiles.readErrorOutputLines();

        if (!errorLines.isEmpty()) {
            // error / warning from CATG

            // check whether there was any exception and get result possibilities according to them
            Set<ResultType> resTypes = errorLines.stream()
                    .filter(line -> line.startsWith("Exception in thread \"main\"")).map(line -> {
                        // exceptions
                        Matcher m = EXCEPTION_LINE_PATTERN.matcher(line);

                        if (!m.matches()) {
                            System.err.println(snippet.getMethod());
                            System.err.println("NO MATCH FOR LINE: " + line);
                            throw new RuntimeException("SETTE parser problem");
                        }

                        String exceptionType = m.group(1);

                        if (exceptionType.equals("java.lang.NoClassDefFoundError")) {
                            return ResultType.NA;
                        } else if (exceptionType.equals("java.lang.VerifyError")) {
                            return ResultType.EX;
                        } else if (exceptionType.endsWith("Exception")) {
                            // enhance
                            return ResultType.EX;
                        } else {
                            System.err.println(snippet.getMethod());
                            System.err.println(outFiles.errorOutputFile);
                            System.err.println("NOT HANDLED EXCEPTION TYPE: " + exceptionType);
                            throw new RuntimeException("SETTE parser problem");
                        }
                    }).collect(Collectors.toSet());

            if (!resTypes.isEmpty()) {
                // had exception line
                if (resTypes.size() == 1) {
                    // exactly one typed exception
                    inputsXml.setResultType(resTypes.iterator().next());
                } else {
                    // very unlikely to have both NoClassDefFoundError and other exception
                    throw new RuntimeException("SETTE parser problem");
                }
            } else {
                // no exception lines
                // check whether the other lines are accepted warnings
                for (String errorLine : errorLines) {
                    if (StringUtils.isBlank(errorLine)) {
                        // skip blank lines
                    } else if (!Stream.of(ACCEPTED_ERROR_LINE_PATTERNS)
                            .anyMatch(p -> p.matcher(errorLine.trim()).matches())) {
                        System.err.println(
                                ACCEPTED_ERROR_LINE_PATTERNS[0].matcher(errorLine).matches());
                        System.err.println(String.join("\n", errorLines));
                        System.err.println("Unknown line: " + errorLine);
                        throw new RuntimeException("SETTE parser problem");
                    }
                }

                // all lines are accepted warnings
                inputsXml.setResultType(ResultType.S);
            }
        } else {
            // no error file, always S
            inputsXml.setResultType(ResultType.S);
        }

        // collect inputs if S
        if (inputsXml.getResultType() == ResultType.S) {
            // collect inputs
            if (!outputLines.get(0).startsWith("Now testing ")) {
                throw new RuntimeException("File beginning problem: " + outFiles.outputFile);
            }

            Pattern p = Pattern.compile("\\[Input (\\d+)\\]");
            Pattern p2 = Pattern.compile("  (\\w+) param(\\d+) = (.*)");

            int inputNumber = 1;

            for (int i = 1; i < outputLines.size(); inputNumber++) {
                String line = outputLines.get(i);

                Matcher m = p.matcher(line);
                if (m.matches()) {
                    if (!m.group(1).equals(String.valueOf(inputNumber))) {
                        System.err.println("Current input should be: " + inputNumber);
                        System.err.println("Current input line: " + line);
                        throw new RuntimeException(
                                "File input problem (" + line + "): " + outFiles.outputFile);
                    }

                    // find end of generated input
                    int nextInputLine = -1;
                    for (int j = i + 1; j < outputLines.size(); j++) {
                        if (p.matcher(outputLines.get(j)).matches()) {
                            nextInputLine = j;
                            break;
                        }
                    }

                    if (nextInputLine < 0) {
                        // EOF
                        nextInputLine = outputLines.size();
                    }

                    int paramCount = snippet.getMethod().getParameterTypes().length;

                    InputElement ie = new InputElement();

                    for (int j = 0; j < paramCount; j++) {
                        String l = outputLines.get(i + 1 + j + 1);
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
                                    "File input problem (" + l + "): " + outFiles.outputFile);
                        }
                    }

                    inputsXml.getGeneratedInputs().add(ie);

                    i = nextInputLine;

                    // TODO now NOT dealing with result and exception
                } else {
                    throw new RuntimeException(
                            "File input problem (" + line + "): " + outFiles.outputFile);
                }
            }
        }
    }
}
