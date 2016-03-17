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
package hu.bme.mit.sette.tools.randoop;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import hu.bme.mit.sette.core.model.parserxml.SnippetInputsXml;
import hu.bme.mit.sette.core.model.runner.ResultType;
import hu.bme.mit.sette.core.model.runner.RunnerProjectUtils;
import hu.bme.mit.sette.core.model.snippet.Snippet;
import hu.bme.mit.sette.core.model.snippet.SnippetProject;
import hu.bme.mit.sette.core.tasks.RunResultParser;
import hu.bme.mit.sette.core.util.io.PathUtils;

public class RandoopParser extends RunResultParser<RandoopTool> {
    private final static Pattern TEST_COUNT_LINE_PATTERN = Pattern
            .compile("^Writing (\\d+) junit tests$");

    public RandoopParser(SnippetProject snippetProject, Path outputDir, RandoopTool tool,
            String runnerProjectTag) {
        super(snippetProject, outputDir, tool, runnerProjectTag);
    }

    @Override
    protected void parseSnippet(Snippet snippet, SnippetOutFiles outFiles,
            SnippetInputsXml inputsXml) throws Exception {
        List<String> outputLines = outFiles.readOutputLines();
        List<String> errorLines = outFiles.readErrorOutputLines();
        File lookUpDir = new File(getRunnerProjectSettings().getBaseDir(),
                "test/" + RunnerProjectUtils.getSnippetBaseFilename(snippet) + "_Test");

        // do not parse inputs
        inputsXml.setGeneratedInputs(null);

        if (outputLines.isEmpty()) {
            // FIXME extremely odd
            throw new RuntimeException("output file empty: " + outFiles.outputFile);
        }

        for (String infoLine : outFiles.readInfoLines()) {
            if (infoLine.contains("Exit value: 1")) {
                inputsXml.setResultType(ResultType.EX);
                break;
            }
        }

        if (inputsXml.getResultType() == null && !lookUpDir.exists() && !errorLines.isEmpty()) {
            String firstLine = errorLines.get(0);

            if (firstLine.startsWith("java.io.FileNotFoundException:")
                    && firstLine.endsWith("_Test/Test.java (No such file or directory)")) {
                // this means that no input was generated but the generation
                // was successful
                inputsXml.setGeneratedInputCount(0);

                if (snippet.getRequiredStatementCoverage() <= Double.MIN_VALUE
                        || snippet.getMethod().getParameterCount() == 0) {
                    // C only if the required statement coverage is 0% or
                    // the method takes no parameters
                    inputsXml.setResultType(ResultType.C);
                } else {
                    inputsXml.setResultType(ResultType.NC);
                }
            } else if (firstLine.startsWith("java.lang.Error: classForName")) {
                // exception, no output that not supported -> EX
                inputsXml.setResultType(ResultType.EX);
            } else if (firstLine.matches("[A-Za-z0-9: ]*") && !firstLine.contains("Error")
                    && !firstLine.contains("Exception")) {
                // normal text on stderr, skip
            } else {
                // TODO
                throw new RuntimeException("TODO parser problem:" + outFiles.errorOutputFile);
            }
        }

        if (inputsXml.getResultType() == null) {
            // always S for Randoop
            inputsXml.setResultType(ResultType.S);

            // get how many tests were generated
            int generatedInputCount = outputLines.stream()
                    .map(line -> TEST_COUNT_LINE_PATTERN.matcher(line.trim()))
                    .filter(m -> m.matches()).map(m -> Integer.parseInt(m.group(1))).findAny()
                    .orElse(-1);

            if (generatedInputCount >= 0) {
                inputsXml.setGeneratedInputCount(generatedInputCount);
            } else {
                // NOTE randoop did not write out the result, we have to determine it :(
                if (!lookUpDir.exists()) {
                    inputsXml.setGeneratedInputCount(0);
                } else {
                    System.err.println("Determining test case count: " + lookUpDir);

                    Validate.isTrue(lookUpDir.isDirectory());
                    File[] testFiles = lookUpDir.listFiles();

                    int cnt = 0;
                    for (File file : testFiles) {
                        log.debug("Parsing with JavaParser: {}", file);
                        CompilationUnit cu = JavaParser.parse(file);
                        log.debug("Parsed with JavaParser: {}", file);
                        ClassOrInterfaceDeclaration cls = (ClassOrInterfaceDeclaration) cu
                                .getTypes().get(0);
                        cnt += cls.getMembers().stream()
                                .filter(bd -> bd instanceof MethodDeclaration).mapToInt(bd -> {
                                    MethodDeclaration md = (MethodDeclaration) bd;
                                    if (md.getName().startsWith("test")
                                            && md.getName().length() >= 5) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }).sum();
                    }

                    System.err.println("Test case count: " + cnt);
                    inputsXml.setGeneratedInputCount(cnt);
                }
            }

            // create input placeholders
        }

        inputsXml.validate();
    }

    @Override
    protected void afterParse() {
        // fix compilation error in test suite files
        try {
            File testDir = getRunnerProjectSettings().getTestDirectory();

            Iterator<File> it = PathUtils.walk(testDir.toPath()).filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java")).map(Path::toFile).sorted()
                    .collect(Collectors.toList()).iterator();

            while (it.hasNext()) {
                File testFile = it.next();
                if (!testFile.getName().endsWith("Test.java")) {
                    continue;
                }

                List<String> lines = PathUtils.readAllLines(testFile.toPath());
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i).replace("public static Test suite() {",
                            "public static TestSuite suite() {");
                    lines.set(i, line);
                }
                PathUtils.write(testFile.toPath(), lines);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
