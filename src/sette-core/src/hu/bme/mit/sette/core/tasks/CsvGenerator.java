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
package hu.bme.mit.sette.core.tasks;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.convert.AnnotationStrategy;
import org.simpleframework.xml.core.Persister;

import hu.bme.mit.sette.core.model.parserxml.SnippetInputsXml;
import hu.bme.mit.sette.core.model.parserxml.SnippetResultXml;
import hu.bme.mit.sette.core.model.runner.RunnerProjectUtils;
import hu.bme.mit.sette.core.model.snippet.Snippet;
import hu.bme.mit.sette.core.model.snippet.SnippetContainer;
import hu.bme.mit.sette.core.model.snippet.SnippetProject;
import hu.bme.mit.sette.core.tool.Tool;
import hu.bme.mit.sette.core.util.io.PathUtils;

public final class CsvGenerator extends EvaluationTaskBase<Tool> {
    private static final String FIELD_SEP = ",";

    public CsvGenerator(SnippetProject snippetProject, Path outputDir, Tool tool,
            String runnerProjectTag) {
        super(snippetProject, outputDir, tool, runnerProjectTag);
    }

    public void generate() throws Exception {
        // sort snippets
        SortedMap<String, Snippet> sortedSnippets = new TreeMap<>();
        for (SnippetContainer container : getSnippetProject().getSnippetContainers()) {
            for (Snippet snippet : container.getSnippets().values()) {
                String key = container.getJavaClass().getPackage().getName() + "/"
                        + getShortSnippetName(snippet);

                // check whether unique
                if (sortedSnippets.containsKey(key)) {
                    System.err.println(sortedSnippets.get(key).getMethod());
                    System.err.println(snippet.getMethod());
                    throw new RuntimeException("Duplicate detected");
                }

                // preserve category order
                sortedSnippets.put(key, snippet);
            }
        }

        // create file data
        List<String> lines = new ArrayList<>();
        lines.add(createHeader());
        for (Entry<String, Snippet> entry : sortedSnippets.entrySet()) {
            lines.add(createRow(entry.getValue()));
        }

        PathUtils.write(getCsvFile().toPath(), lines);
    }

    public File getCsvFile() {
        return new File(getRunnerProjectSettings().getBaseDir(), "sette-evaluation.csv");
    }

    // Category: B1a
    // Snippet: B1a_myMethod
    // Tool: CATG
    // Coverage: 96.43%
    // Status = ResultType
    // Size = TestCaseCount
    // Run = TAG
    // Duration: 43243 ms
    private static final String[] HEADER_COLUMNS = new String[] { "Category", "Snippet", "Tool",
            "Coverage", "Status", "Size", "Run", "Duration" };

    private String createHeader() {
        String header = String.join(FIELD_SEP, HEADER_COLUMNS);
        if (tool.getName().startsWith("SnippetInputChecker")) {
            header += FIELD_SEP + "RequiredStatementCoverage    ";
        }
        return header;
    }

    private String createRow(Snippet snippet) throws Exception {
        // parse data
        File infoFile = RunnerProjectUtils.getSnippetInfoFile(getRunnerProjectSettings(), snippet);
        File inputsXmlFile = RunnerProjectUtils.getSnippetInputsFile(getRunnerProjectSettings(),
                snippet);
        File resultXmlFile = RunnerProjectUtils.getSnippetResultFile(getRunnerProjectSettings(),
                snippet);

        Serializer serializer = new Persister(new AnnotationStrategy());
        SnippetInputsXml inputsXml = serializer.read(SnippetInputsXml.class, inputsXmlFile);
        inputsXml.validate();

        SnippetResultXml resultXml = serializer.read(SnippetResultXml.class, resultXmlFile);
        resultXml.validate();

        // example: Elapsed time: 2002 ms
        String elapsedTime;
        if (infoFile.exists()) {
            elapsedTime = PathUtils.lines(infoFile.toPath())
                    .filter(line -> !StringUtils.isBlank(line)
                            && line.trim().startsWith("Elapsed time:"))
                    .map(line -> line.replaceAll("Elapsed time:", "").trim()).findAny().get()
                    .replaceAll("ms", "").trim();
        } else {
            elapsedTime = "";
        }

        String testCaseCount;
        switch (resultXml.getResultType()) {
            case NC:
            case C:
                testCaseCount = String.valueOf(inputsXml.getGeneratedInputCount());
                break;

            default:
                testCaseCount = "";
                break;
        }

        // create fields
        List<String> fields = new ArrayList<>();

        String snippetShortName = getShortSnippetName(snippet);
        fields.add(snippetShortName.split("_")[0]); // category
        fields.add(snippetShortName); // snippet
        fields.add(tool.getName()); // tool
        fields.add(StringUtils.defaultIfEmpty(resultXml.getAchievedCoverage(), "").replace('%', ' ')
                .trim()); // coverage
        fields.add(resultXml.getResultType().toString()); // Status = ResultType
        fields.add(testCaseCount); // Size = TestCaseCount
        fields.add(getRunnerProjectSettings().getTag()); // Run = TAG
        fields.add(elapsedTime); // Duration: 43243 ms

        if (tool.getName().startsWith("SnippetInputChecker")) {
            fields.add(String.format("%.2f", snippet.getRequiredStatementCoverage())); // coverage
            Validate.isTrue(fields.size() == HEADER_COLUMNS.length + 1);
        } else {
            Validate.isTrue(fields.size() == HEADER_COLUMNS.length);
        }

        // assemble and return
        return String.join(FIELD_SEP, fields);
    }

    private static String getShortSnippetName(Snippet snippet) {
        String className = snippet.getContainer().getName();

        String ret;
        if (className.equals("B4_SafeArrays")) {
            ret = "B4a";
        } else if (className.equals("B4_UnsafeArrays")) {
            ret = "B4b";
        } else if (className.equals("B5a_CallPrivate")) {
            ret = "B5a1";
        } else if (className.equals("B5a_CallPublic")) {
            ret = "B5a2";
        } else if (className.equals("B5b_LimitedRecursive")) {
            ret = "B5b1";
        } else if (className.equals("B5b_UnlimitedRecursive")) {
            ret = "B5b2";
        } else {
            ret = className.split("_")[0];
        }

        ret += "_" + snippet.getMethod().getName();
        return ret;
    }
}
