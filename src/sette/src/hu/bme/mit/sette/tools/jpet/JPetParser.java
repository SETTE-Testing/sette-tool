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
package hu.bme.mit.sette.tools.jpet;

import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import hu.bme.mit.sette.core.model.parserxml.SnippetInputsXml;
import hu.bme.mit.sette.core.model.runner.ResultType;
import hu.bme.mit.sette.core.model.snippet.Snippet;
import hu.bme.mit.sette.core.model.snippet.SnippetProject;
import hu.bme.mit.sette.core.tasks.RunResultParserBase;
import hu.bme.mit.sette.core.util.io.PathUtils;
import hu.bme.mit.sette.core.validator.PathType;
import hu.bme.mit.sette.core.validator.PathValidator;
import hu.bme.mit.sette.tools.jpet.xmlparser.JPetTestCaseXmlParser;
import hu.bme.mit.sette.tools.jpet.xmlparser.JPetTestCasesConverter;

public class JPetParser extends RunResultParserBase<JPetTool> {
    public JPetParser(SnippetProject snippetProject, Path outputDir, JPetTool tool,
            String runnerProjectTag) {
        super(snippetProject, outputDir, tool, runnerProjectTag);
    }

    private static final Pattern PATTERN_FULL_CODE = Pattern
            .compile("^Full Code Coverage of '.*': (\\d+(.\\d+)?)% \\(.*\\)$");
    private static final Pattern PATTERN_TOP_CODE = Pattern
            .compile("^Top Code Coverage of '.*': (\\d+(.\\d+)?)% \\(.*\\)$");

    @Override
    protected void parseSnippet(Snippet snippet, SnippetOutFiles outFiles,
            SnippetInputsXml inputsXml) throws Exception {
        List<String> outputLines = outFiles.readOutputLines();
        List<String> errorLines = outFiles.readErrorOutputLines();

        if (!errorLines.isEmpty()) {
            // TODO enhance this section and make it clear
            String firstLine = errorLines.get(0);

            if (firstLine
                    .startsWith("ERROR: test_data_generator:unfold_bck/6: Undefined procedure:")) {
                inputsXml.setResultType(ResultType.NA);
            } else if (firstLine
                    .startsWith("ERROR: Domain error: `clpfd_expression' expected, found")) {
                inputsXml.setResultType(ResultType.NA);
            } else if (firstLine.startsWith("ERROR: Unknown message: error(resolve_classfile/")) {
                inputsXml.setResultType(ResultType.NA);
            } else
                if (firstLine.startsWith("ERROR: local_control:unfold/3: Undefined procedure:")) {
                inputsXml.setResultType(ResultType.NA);
            } else {
                // TODO enhance error handling

                // this is debug (only if unhandled error)
                System.err.println("=============================");
                System.err.println(snippet.getMethod());
                System.err.println("=============================");

                for (String line : errorLines) {
                    System.err.println(line);
                }
                System.err.println("=============================");

                // TODO enhance error handling
                throw new RuntimeException("PARSER PROBLEM, UNHANDLED ERROR");
            }
        } else {
            // TODO enhance
            if (outputLines.get(outputLines.size() - 1)
                    .startsWith("Error loading bytecode program")) {
                // System.err.println(snippet.getMethod().getName());
                // System.err.println("BYTECODE PROBLEM");
                inputsXml.setResultType(ResultType.EX);
                // throw new RuntimeException(""
            } else {
                // extract coverage
                if (outputLines.size() >= 8) {
                    String fullCode = outputLines.get(outputLines.size() - 3).trim();
                    String topCode = outputLines.get(outputLines.size() - 2).trim();

                    Matcher mFull = PATTERN_FULL_CODE.matcher(fullCode);
                    Matcher mTop = PATTERN_TOP_CODE.matcher(topCode);

                    // TODO should not use jPET coverage information in the
                    // future
                    if (mFull.matches() && mTop.matches()) {
                        double full = Double.parseDouble(mFull.group(1));
                        double top = Double.parseDouble(mTop.group(1));

                        if (full == 100.0 && top == 100.0) {
                            // full -> C
                            inputsXml.setResultType(ResultType.C);
                        } else if (snippet.getIncludedConstructors().isEmpty()
                                && snippet.getIncludedMethods().isEmpty()) {
                            // only consider top, no included things
                            if (top >= snippet.getRequiredStatementCoverage()) {
                                inputsXml.setResultType(ResultType.C);
                            } else {
                                // FIXME
                                // inputsXml.setResultType(ResultType.NC);

                                // System.err.println(snippet.getMethod()
                                // .getName());
                                // System.err
                                // .println(String
                                // .format("No incl. no statis. - Full: %.2f Top: %.2f Req: %.2f",
                                // full,
                                // top,
                                // snippet.getRequiredStatementCoverage()));
                            }
                        } else {
                            // few cases, not very usefol top and full now...
                            // System.err.println(snippet.getMethod()
                            // .getName());
                            // System.err.println(String.format(
                            // "Has included - Full: %.2f Top: %.2f Req: %.2f",
                            // full,
                            // top,
                            // snippet.getRequiredStatementCoverage()));

                        }
                    } else {
                        // TODO error handling
                        throw new RuntimeException("Both should match");
                    }
                }

                if (inputsXml.getResultType() == null) {
                    if (snippet.getRequiredStatementCoverage() == 0) {
                        inputsXml.setResultType(ResultType.C);
                    } else {
                        inputsXml.setResultType(ResultType.S);
                    }
                }

                // extract inputs
                outputLines = null;

                Path testCasesFile = JPetTool.getTestCaseXmlFile(getRunnerProjectSettings(),
                        snippet);
                new PathValidator(testCasesFile).type(PathType.REGULAR_FILE).validate();

                if (PathUtils.size(testCasesFile) / 1000.0 / 1000.0 > 10) {
                    // just to not kill the XML parser with extremely big files
                    // TODO enhance this section
                    System.err.println(String.format("Filesize is bigger than 10 MB (%.2f MB): %s",
                            PathUtils.size(testCasesFile) / 1000.0 / 1000.0,
                            testCasesFile.getFileName().toString()));
                }
                // TODO it was used to dump the cases where jpet cannot decide coverage

                SAXParserFactory factory = SAXParserFactory.newInstance();
                SAXParser saxParser = factory.newSAXParser();

                JPetTestCaseXmlParser testCasesParser = new JPetTestCaseXmlParser();

                saxParser.parse(testCasesFile.toFile(), testCasesParser);

                JPetTestCasesConverter.convert(snippet, testCasesParser.getTestCases(), inputsXml);
            }

            // NOTE old code, revise and act
            // find input lines
            //
            // List<String> inputLines = new ArrayList<>();
            // boolean shouldCollect = false;
            // while (lines.hasNext()) {
            // String line = lines.next();
            // if (line.trim()
            // .equals("====================================================== Method Summaries"))
            // {
            // shouldCollect = true;
            // } else if (shouldCollect) {
            // if
            // (line.startsWith("======================================================"))
            // {
            // // start of next section
            // shouldCollect = false;
            // break;
            // } else {
            // if (!StringUtils.isBlank(line)) {
            // inputLines.add(line.trim());
            // }
            // }
            // }
            // }
            //
            // // close iterator
            // lines.close();
            //
            // // remove duplicates
            // inputLines = new ArrayList<>(new LinkedHashSet<>(inputLines));
            //
            // String firstLine = inputLines.get(0);
            // if (!firstLine.startsWith("Inputs: ")) throw new RuntimeException();
            // firstLine = firstLine.substring(7).trim();
            // String[] parameterStrings = StringUtils.split(firstLine, ',');
            // ParameterType[] parameterTypes = new
            // ParameterType[parameterStrings.length];
            //
            // if (inputLines.size() == 2
            // && inputLines.get(1).startsWith("No path conditions for")) {
            // InputElement input = new InputElement();
            // for (int i = 0; i < parameterStrings.length; i++) {
            // // no path conditions, only considering the "default" inputs
            // Class<?> type = snippet.getMethod().getParameterTypes()[i];
            // parameterTypes[i] = getParameterType(type);
            // input.parameters().add(
            // new ParameterElement(parameterTypes[i],
            // getDefaultParameterString(type)));
            // }
            // inputsXml.generatedInputs().add(input);
            // } else {
            // // parse parameter types
            //
            // Class<?>[] paramsJavaClass = snippet.getMethod()
            // .getParameterTypes();
            //
            // for (int i = 0; i < parameterStrings.length; i++) {
            // String parameterString = parameterStrings[i];
            // Class<?> pjc = ClassUtils
            // .primitiveToWrapper(paramsJavaClass[i]);
            //
            // if (parameterString.endsWith("SYMINT")) {
            // if (pjc == Boolean.class) {
            // parameterTypes[i] = ParameterType.BOOLEAN;
            // } else if (pjc == Byte.class) {
            // parameterTypes[i] = ParameterType.BYTE;
            // } else if (pjc == Short.class) {
            // parameterTypes[i] = ParameterType.SHORT;
            // } else if (pjc == Integer.class) {
            // parameterTypes[i] = ParameterType.INT;
            // } else if (pjc == Long.class) {
            // parameterTypes[i] = ParameterType.LONG;
            // } else {
            // // int for something else
            // parameterTypes[i] = ParameterType.INT;
            // }
            // } else if (parameterString.endsWith("SYMREAL")) {
            // if (pjc == Float.class) {
            // parameterTypes[i] = ParameterType.FLOAT;
            // } else if (pjc == Float.class) {
            // parameterTypes[i] = ParameterType.DOUBLE;
            // } else {
            // // int for something else
            // parameterTypes[i] = ParameterType.DOUBLE;
            // }
            // } else {
            // // TODO error handling
            // // int for something else
            // throw new RuntimeException("PARSER PROBLEM");
            // }
            // }
            //
            // // example
            // // inheritsAPIGuessTwoPrimitives(11,-2147483648(don't care)) -->
            // // "java.lang.IllegalArgumentException..."
            // // inheritsAPIGuessTwoPrimitives(9,11) -->
            // // "java.lang.IllegalArgumentException..."
            // // inheritsAPIGuessTwoPrimitives(7,9) -->
            // // "java.lang.RuntimeException: Out of range..."
            // // inheritsAPIGuessTwoPrimitives(4,1) --> Return Value: 1
            // // inheritsAPIGuessTwoPrimitives(0,0) --> Return Value: 0
            // // inheritsAPIGuessTwoPrimitives(9,-88) -->
            // // "java.lang.IllegalArgumentException..."
            // // inheritsAPIGuessTwoPrimitives(-88,-2147483648(don't care))
            // // --> "java.lang.IllegalArgumentException..."
            //
            // String ps = String.format("^%s\\((.*)\\) --> (.*)$", snippet
            // .getMethod().getName());
            //
            // ps = String.format("^%s(.*) --> (.*)$", snippet.getMethod()
            // .getName());
            // Pattern p = Pattern.compile(ps);
            //
            // // parse inputs
            // int i = -1;
            // for (String line : inputLines) {
            // i++;
            //
            // if (i == 0) {
            // // first line
            // continue;
            // } else if (StringUtils.isEmpty(line)) {
            // continue;
            // }
            //
            // Matcher m = p.matcher(line);
            //
            // if (m.matches()) {
            // String paramsString = StringUtils.substring(m.group(1)
            // .trim(), 1, -1);
            // String resultString = m.group(2).trim();
            //
            // paramsString = StringUtils.replace(paramsString,
            // "(don't care)", "");
            //
            // String[] paramsStrings = StringUtils.split(
            // paramsString, ',');
            //
            // InputElement input = new InputElement();
            //
            // // if index error -> lesser inputs than parameters
            // for (int j = 0; j < parameterTypes.length; j++) {
            // if (parameterTypes[j] == ParameterType.BOOLEAN
            // && paramsStrings[j].contains("-2147483648")) {
            // // don't care -> 0
            // paramsStrings[j] = "false";
            // }
            //
            // ParameterElement pe = new ParameterElement(
            // parameterTypes[j], paramsStrings[j].trim());
            //
            // try {
            // // just check the type format
            // pe.validate();
            // } catch (Exception ex) {
            // // TODO debug - remove or log
            // System.out.println(parameterTypes[j]);
            // System.out.println(paramsStrings[j]);
            // System.out.println(pe.getType());
            // System.out.println(pe.getValue());
            // ex.printStackTrace();
            // throw ex;
            //
            // System.err
            // .println("=============================");
            // System.err.println(snippet.getMethod());
            // System.err
            // .println("=============================");
            // for (String lll : inputLines) {
            // System.err.println(lll);
            // }
            // System.err
            // .println("=============================");
            //
            // throw new RuntimeException("EXIT")
            // }
            //
            // input.parameters().add(pe);
            // }
            //
            // if (resultString.startsWith("Return Value:")) {
            // // has retval, nothing to do
            // } else {
            // // exception; example (" is present inside the
            // // string!!!):
            // // "java.lang.ArithmeticException: div by 0..."
            // // "java.lang.IndexOutOfBoundsException: Index: 1, Size: 5..."
            //
            // int pos = resultString.indexOf(':');
            // if (pos < 0) {
            // // not found :, search for ...
            // pos = resultString.indexOf("...");
            // }
            //
            // String ex = resultString.substring(1, pos);
            //
            // input.setExpected((Class<? extends Throwable>) Class
            // .forName(ex));
            //
            // // System.err.println(resultString);
            // // System.err.println(ex);
            // // // input.setExpected(expected);
            // }
            //
            // inputsXml.generatedInputs().add(input);
            // } else {
            // System.err.println("NO MATCH");
            // System.err.println(ps);
            // System.err.println(line);
            // throw new Exception("NO MATCH: " + line);
            // }
            // }
            // }
            //
            // inputsXml.validate();
        }
    }
}
