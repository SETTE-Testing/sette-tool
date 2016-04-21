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

import static java.util.stream.Collectors.joining;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

import hu.bme.mit.sette.core.configuration.SetteConfigurationException;
import hu.bme.mit.sette.core.model.snippet.Snippet;
import hu.bme.mit.sette.core.model.snippet.SnippetProject;
import hu.bme.mit.sette.core.tasks.AntExecutor;
import hu.bme.mit.sette.core.tasks.RunnerProjectRunnerBase;
import hu.bme.mit.sette.core.util.io.PathUtils;
import hu.bme.mit.sette.core.util.process.ProcessExecutionException;
import hu.bme.mit.sette.core.util.process.ProcessUtils;

public final class RandoopRunner extends RunnerProjectRunnerBase<RandoopTool> {
    private final Random seedGenerator;

    public RandoopRunner(SnippetProject snippetProject, Path outputDir, RandoopTool tool,
            String runnerProjectTag) {
        super(snippetProject, outputDir, tool, runnerProjectTag);
        this.seedGenerator = new Random();
    }

    @Override
    public boolean shouldKillAfterTimeout() {
        return false;
    }

    @Override
    protected void afterPrepare() {
        // ant build
        AntExecutor.executeAnt(getRunnerProjectSettings().getBaseDir(), null);
    }

    @Override
    protected void runOne(Snippet snippet, Path infoFile, Path outputFile, Path errorFile)
            throws SetteConfigurationException {
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

        Path randoopJar = tool.getToolJar();

        // TODO ???
        // String filenameBase = JavaFileUtil
        // .packageNameToFilename(snippet.getContainer()
        // .getJavaClass().getName())
        // + "_" + snippet.getMethod().getName();

        // create command
        String classpath = randoopJar.toString() + SystemUtils.PATH_SEPARATOR + "build";

        for (Path libraryFile : getSnippetProject().getJavaLibFiles()) {
            classpath += SystemUtils.PATH_SEPARATOR
                    + getRunnerProjectSettings().getSnippetLibraryDir().getFileName().toString()
                    + SystemUtils.FILE_SEPARATOR + libraryFile.toFile().getName();
        }

        int timelimit = (getTimeoutInMs() + 500) / 1000; // ceil
        String junitPackageName = snippet.getContainer().getJavaClass().getName() + "_"
                + snippet.getMethod().getName() + "_Test";

        // create method list file
        Path methodList = getRunnerProjectSettings().getBaseDir()
                .resolve("methodlist_" + junitPackageName + ".tmp"); // TODO better file name

        PathUtils.write(methodList, createMethodListLines(snippet));

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
        // cmd.add("--classlist=" + getClassListFile().toAbsolutePath());
        cmd.add("--methodlist=" + methodList.toString().replace('\\', '/'));
        cmd.add("--timelimit=" + timelimit);
        // cmd.add("--forbid-null=false"); // use default false
        // cmd.add("--null-ratio=0.5"); // use default 0.05
        cmd.add("--junit-output-dir=test");
        cmd.add("--junit-package-name=" + junitPackageName);
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
        executeToolProcess(cmd, infoFile, outputFile, errorFile);

        // TODO preserve for reproduction
        // delete method list file
        // PathUtils.deleteIfExists(methodList);
    }

    @Override
    public void cleanUp() throws ProcessExecutionException {
        if (!SystemUtils.IS_OS_WINDOWS) {
            // TODO better search
            ProcessUtils.searchAndTerminateProcesses("randoop.main.Main");
        }
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
        String paramsString = createParamsString(method);
        return String.format("%s.%s(%s)", method.getDeclaringClass().getName(), method.getName(),
                paramsString);
    }

    private static String getConstructorNameAndParameterTypesString(Constructor<?> ctor) {
        String paramsString = createParamsString(ctor);
        return String.format("%s.<init>(%s)", ctor.getDeclaringClass().getName(), paramsString);
    }

    private static String createParamsString(Executable executable) {
        // collect and join parameter type names
        return Stream.of(executable.getParameterTypes())
                .map(p -> p.getName())
                .collect(joining(","));
    }

    private static List<String> createMethodListLines(Snippet snippet) {
        List<String> lines = new ArrayList<>();
        lines.add("method : " + getMethodNameAndParameterTypesString(snippet.getMethod()));
        lines.add("cons : java.lang.Object.<init>()");

        // add constructors of non-primitive parameter types
        Stream.of(snippet.getMethod().getParameterTypes())
                .filter(cls -> !cls.isPrimitive())
                .distinct()
                .flatMap(cls -> Stream.of(cls.getConstructors()))
                .filter(ctor -> Modifier.isPublic(ctor.getModifiers()))
                .forEach(ctor -> {
                    lines.add("cons : " + getConstructorNameAndParameterTypesString(ctor));
                });

        Collections.sort(lines);
        return lines;
    }
}
