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
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

import hu.bme.mit.sette.core.configuration.SetteConfigurationException;
import hu.bme.mit.sette.core.model.snippet.Snippet;
import hu.bme.mit.sette.core.model.snippet.SnippetProject;
import hu.bme.mit.sette.core.tasks.AntExecutor;
import hu.bme.mit.sette.core.tasks.RunnerProjectRunner;
import hu.bme.mit.sette.core.util.process.ProcessUtils;

public final class RandoopRunner extends RunnerProjectRunner<RandoopTool> {
    private final Random seedGenerator;

    public RandoopRunner(SnippetProject snippetProject, Path outputDir, RandoopTool tool,
            String runnerProjectTag) {
        super(snippetProject, outputDir, tool, runnerProjectTag);
        this.seedGenerator = new Random();
    }

    @Override
    protected void afterPrepare() throws IOException {
        // ant build
        AntExecutor.executeAnt(getRunnerProjectSettings().getBaseDir(), null);
    }

    @Override
    protected void runOne(Snippet snippet, File infoFile, File outputFile, File errorFile)
            throws IOException, SetteConfigurationException {
        // TODO make better
        /*
         * e.g.:
         * 
         * java -classpath
         * "/home/sette/sette/sette-tool/test-generator-tools/randoop/randoop.jar:/home/sette/sette/sette-resuults/randoop/build"
         * randoop.main.Main gentests --methodlist="methodlist.tmp" --timelimit=30
         * --junit-output-dir="test"
         * --junit-package-name=hu.bme.mit.sette.snippets._1_basic.B3_loops. B3_While_complex_Test
         * --junit-classname=Test
         * 
         * methodlist.tmp content example: (Method.toString())
         * hu.bme.mit.sette.snippets._3_objects.O1_Simple .guessObject(hu.bme.mit.
         * sette.snippets._3_objects.dependencies.SimpleObject)
         */

        File randoopJar = getTool().getToolJar().toFile();

        // TODO ???
        // String filenameBase = JavaFileUtil
        // .packageNameToFilename(snippet.getContainer()
        // .getJavaClass().getName())
        // + "_" + snippet.getMethod().getName();

        // create command
        String classpath = randoopJar.getCanonicalPath() + SystemUtils.PATH_SEPARATOR + "build";

        for (Path libraryFile : getSnippetProject().getLibFiles()) {
            classpath += SystemUtils.PATH_SEPARATOR
                    + getSnippetProject().getLibDir().toString()
                    + SystemUtils.FILE_SEPARATOR + libraryFile.toFile().getName();
        }

        int timelimit = (getTimeoutInMs() + 500) / 1000; // ceil
        String junitPackageName = snippet.getContainer().getJavaClass().getName() + "_"
                + snippet.getMethod().getName() + "_Test";

        // create method list file
        File methodList = new File(getRunnerProjectSettings().getBaseDir(),
                "methodlist_" + junitPackageName + ".tmp"); // TODO better file name
        Files.write(methodList.toPath(),
                ("method : " + getMethodNameAndParameterTypesString(snippet.getMethod()) + "\n")
                        .getBytes());

        // create command
        // String cmdFormat =
        // "java -classpath \"%s\" randoop.main.Main gentests "
        // + "--methodlist=\"%s\" --timelimit=%d --junit-output-dir=\"test\" "
        // + "--junit-package-name=%s --junit-classname=Test";
        List<String> cmd = new ArrayList<>();
        cmd.add("java");
        cmd.add("-classpath");
        cmd.add(classpath);
        cmd.add("randoop.main.Main");
        cmd.add("gentests");
        cmd.add("--methodlist=" + methodList.getAbsolutePath());
        cmd.add("--timelimit=" + timelimit);
        cmd.add("--forbid-null=false");
        cmd.add("--null-ratio=0.5");
        cmd.add("--junit-output-dir=test");
        cmd.add("--junit-package-name=" + junitPackageName);
        cmd.add("--junit-classname=Test");
        cmd.add("--randomseed=" + seedGenerator.nextInt());
        // TODO limit strings to 50
        cmd.add("--string-maxlen=50");
        // TODO limit generated test cases to 5000 (Randoop first generates,
        // then outputs, thus the number of the test written may be smaller)
        cmd.add("--inputlimit=5000");

        // String cmd = String.format(cmdFormat, classpath,
        // methodList.getAbsolutePath(), timelimit,
        // junitPackageName);

        System.out.println("  command: " + StringUtils.join(cmd, ' '));

        // run process
        // Randoop will stop generation at the given time limit (however, it
        // needs extra time for dumping test cases)
        executeToolProcess(Arrays.asList(cmd.toString().split("\\s+")), infoFile, outputFile,
                errorFile);

        // delete method list file
        Files.deleteIfExists(methodList.toPath());
    }

    @Override
    public int getTimeoutInMs() {
        // FIXME randoop
        return 0;
    }

    @Override
    public void cleanUp() throws IOException {
        // TODO better search
        ProcessUtils.searchAndTerminateProcesses("randoop.main.Main");
        System.gc();
    }

    /**
     * Gets the method name and parameter types string.
     *
     * @param method
     *            the method
     * @return the method name and parameter types string, e.g.
     *         <code>pkg.Cls.m(int[],java.lang.String[])</code>
     */
    private static String getMethodNameAndParameterTypesString(Method method) {
        // collect and join parameter type names
        String paramsString = Stream.of(method.getParameterTypes()).map(p -> p.getTypeName())
                .collect(Collectors.joining(","));

        // create string
        return String.format("%s.%s(%s)", method.getDeclaringClass().getName(), method.getName(),
                paramsString);
    }
}
