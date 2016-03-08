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
package hu.bme.mit.sette.tools.snippetinputchecker;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.SystemUtils;

import com.google.common.io.CharStreams;
import com.google.common.io.Resources;

import hu.bme.mit.sette.common.snippets.SnippetInputContainer;
import hu.bme.mit.sette.core.SetteException;
import hu.bme.mit.sette.core.configuration.SetteConfigurationException;
import hu.bme.mit.sette.core.model.snippet.Snippet;
import hu.bme.mit.sette.core.model.snippet.SnippetContainer;
import hu.bme.mit.sette.core.model.snippet.SnippetInputFactoryContainer;
import hu.bme.mit.sette.core.model.snippet.SnippetProject;
import hu.bme.mit.sette.core.tasks.AntExecutor;
import hu.bme.mit.sette.core.tasks.RunnerProjectRunner;
import hu.bme.mit.sette.core.util.io.PathUtils;

public final class SnippetInputCheckerRunner extends RunnerProjectRunner<SnippetInputCheckerTool> {
    private final String testTemplate;
    private final ExecutorService executor;

    public SnippetInputCheckerRunner(SnippetProject snippetProject, Path outputDir,
            SnippetInputCheckerTool tool, String runnerProjectTag) {
        super(snippetProject, outputDir, tool, runnerProjectTag);
        executor = Executors.newFixedThreadPool(16);

        try {

            String templateFilename = "snippet-input-checker-test-case.template";

            InputStream templateFileStream = Resources.getResource(templateFilename).openStream();
            testTemplate = CharStreams.toString(new InputStreamReader(templateFileStream));
        } catch (IOException ex) {
            // TODO Auto-generated catch block
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean shouldKillAfterTimeout() {
        return false;
    }

    @Override
    protected void afterPrepare() throws IOException {
        // ant build
        AntExecutor.executeAnt(getRunnerProjectSettings().getBaseDir(), null);

        // delete test dir if exists
        PathUtils.deleteIfExists(getRunnerProjectSettings().getTestDirectory().toPath());
    }

    @Override
    protected void runOne(Snippet snippet, File infoFile, File outputFile, File errorFile)
            throws IOException, SetteConfigurationException {
        if (snippet.getContainer().getInputFactoryContainer() == null) {
            // no inputs => N/A
            return;
        }

        // call the "tool" on several threads, since it is only a dummy process call
        executor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                List<String> command;

                if (SystemUtils.IS_OS_WINDOWS) {
                    command = Arrays.asList("cmd.exe", "/c", "echo.");
                } else {
                    command = Arrays.asList("/bin/bash", "-c", "echo");
                }

                executeToolProcess(command, infoFile, outputFile, errorFile);
                return null;
            }
        });

        // generate and save test cases
        String testSource = generateTestSource(snippet);

        Path target = getRunnerProjectSettings().getTestDirectory().toPath()
                .resolve(snippet.getContainer().getJavaClass().getName().replace('.', '/')
                        + "_" + snippet.getName() + "_Test.java");
        PathUtils.write(target, testSource.getBytes());
    }

    @Override
    protected void afterRunAll() throws IOException, SetteException {
        // wait for all threads
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            // skip
        }
        executor.shutdown();
    }

    private String generateTestSource(Snippet snippet) {
        Map<String, String> tokens = new HashMap<>();
        SnippetContainer sc = snippet.getContainer();
        SnippetInputFactoryContainer sifc = sc.getInputFactoryContainer();

        StringBuilder testMethods = new StringBuilder();

        if (snippet.getRequiredStatementCoverage() > 0) {
            // defend against infinite loops
            try {
                SnippetInputContainer inputs = sifc.getInputFactories().get(snippet.getName())
                        .getInputs();
                for (int i = 0; i < inputs.size(); i++) {
                    testMethods.append("\n");
                    testMethods.append("    @Test\n");
                    testMethods.append("    public void test_" + i + "() {\n");
                    testMethods.append("        check(inputs.get(" + i + "));\n");
                    testMethods.append("    }\n");
                }
            } catch (Exception ex) {
                // FIXME
                throw new RuntimeException(ex);
            }
        }

        tokens.put("@@PACKAGE@@", sc.getJavaClass().getPackage().getName());
        tokens.put("@@SNIPPET_CONTAINER_NAME@@", sc.getName());
        tokens.put("@@SNIPPET@@", snippet.getName());
        tokens.put("@@SNIPPET_INPUT_CONTAINER@@", sifc.getJavaClass().getName());
        tokens.put("@@TEST_METHODS@@", testMethods.toString());

        String testSource = testTemplate;

        for (Entry<String, String> tokenEntry : tokens.entrySet()) {
            testSource = testSource.replace(tokenEntry.getKey(), tokenEntry.getValue());
        }

        return testSource;
    }

    @Override
    public void cleanUp() throws IOException, SetteException {
        // nothing to do
    }
}
