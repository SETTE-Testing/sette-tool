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
// NOTE revise this file
package hu.bme.mit.sette.core.random;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang3.Validate;

import hu.bme.mit.sette.core.model.snippet.Snippet;
import hu.bme.mit.sette.core.model.snippet.SnippetContainer;
import hu.bme.mit.sette.core.model.snippet.SnippetProject;

public final class SampleManualInputsCsvGenerator {
    public static void main(String[] args) throws Exception {
        SnippetProject snippetProject = SnippetProject
                .parse(Paths.get("../../../sette-snippets/sette-snippets"));
        File outputDirectory = new File("../../../sette-results").getCanonicalFile();

        SampleManualInputsCsvGenerator gen = new SampleManualInputsCsvGenerator(snippetProject,
                outputDirectory);
        gen.generate();
        System.err.println("Generated to: " + gen.getCsvFile());
    }

    private static final String FIELD_SEP = ",";
    private SnippetProject snippetProject;
    private File outputDirectory;

    public SampleManualInputsCsvGenerator(SnippetProject snippetProject, File outputDirectory) {
        this.snippetProject = snippetProject;
        this.outputDirectory = outputDirectory;
    }

    public void generate() throws Exception {
        // sort snippets
        SortedMap<String, Snippet> sortedSnippets = new TreeMap<>();
        for (SnippetContainer container : snippetProject.getSnippetContainers()) {
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

        Files.write(getCsvFile().toPath(), lines);
    }

    public File getCsvFile() {
        return new File(outputDirectory, "sample-manual-inputs.csv");
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

    private static String createHeader() {
        return String.join(FIELD_SEP, HEADER_COLUMNS);
    }

    private static String createRow(Snippet snippet) throws Exception {
        String testCaseCount = String.valueOf(snippet.getInputFactory().getInputs().size());

        // create fields
        List<String> fields = new ArrayList<>();

        String snippetShortName = getShortSnippetName(snippet);
        fields.add(snippetShortName.split("_")[0]); // category
        fields.add(snippetShortName); // snippet
        fields.add("Manual"); // tool
        fields.add(String.format("%.2f", snippet.getRequiredStatementCoverage())); // coverage
        fields.add("C"); // Status = ResultType
        fields.add(testCaseCount); // Size = TestCaseCount
        fields.add(""); // Run = TAG
        fields.add(""); // Duration: 43243 ms

        Validate.isTrue(fields.size() == HEADER_COLUMNS.length);

        // assemble and return
        return String.join(FIELD_SEP, fields);
    }

    private static String getShortSnippetName(Snippet snippet) {
        String className = snippet.getContainer().getJavaClass().getSimpleName();

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
