/*
 * SETTE - Symbolic Execution based Test Tool Evaluator
 *
 * SETTE is a tool to help the evaluation and comparison of symbolic execution
 * based test input generator tools.
 *
 * Budapest University of Technology and Economics (BME)
 *
 * Authors: Lajos Cseppentő <lajos.cseppento@inf.mit.bme.hu>, Zoltán Micskei
 * <micskeiz@mit.bme.hu>
 *
 * Copyright 2014
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package hu.bme.mit.sette.run;

import hu.bme.mit.sette.GeneratorUI;
import hu.bme.mit.sette.ParserUI;
import hu.bme.mit.sette.RunnerUI;
import hu.bme.mit.sette.common.Tool;
import hu.bme.mit.sette.common.ToolRegister;
import hu.bme.mit.sette.common.exceptions.SetteException;
import hu.bme.mit.sette.common.model.runner.ResultType;
import hu.bme.mit.sette.common.model.runner.RunnerProjectSettings;
import hu.bme.mit.sette.common.model.runner.RunnerProjectUtils;
import hu.bme.mit.sette.common.model.runner.xml.SnippetInputsXml;
import hu.bme.mit.sette.common.model.snippet.Snippet;
import hu.bme.mit.sette.common.model.snippet.SnippetContainer;
import hu.bme.mit.sette.common.model.snippet.SnippetProject;
import hu.bme.mit.sette.common.model.snippet.SnippetProjectSettings;
import hu.bme.mit.sette.common.tasks.TestSuiteGenerator;
import hu.bme.mit.sette.common.tasks.TestSuiteRunner;
import hu.bme.mit.sette.common.validator.FileType;
import hu.bme.mit.sette.common.validator.FileValidator;
import hu.bme.mit.sette.common.validator.exceptions.ValidationException;
import hu.bme.mit.sette.common.validator.exceptions.ValidatorException;
import hu.bme.mit.sette.snippetbrowser.SnippetBrowser;
import hu.bme.mit.sette.tools.catg.CatgTool;
import hu.bme.mit.sette.tools.jpet.JPetTool;
import hu.bme.mit.sette.tools.spf.SpfTool;

import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.convert.AnnotationStrategy;
import org.simpleframework.xml.core.Persister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Run {
    private static final Logger LOG = LoggerFactory
            .getLogger(Run.class);
    private static final String SETTE_PROPERTIES = "sette.properties";

    public static File BASEDIR;
    public static File SNIPPET_DIR;
    public static String SNIPPET_PROJECT;
    public static File OUTPUT_DIR;

    private static final String[] scenarios = new String[] { "exit",
        "generator", "runner", "parser", "tests-generator",
        "tests-run", "snippet-browser", "export-csv" };

    public static void main(String[] args) {
        LOG.debug("main() called");

        // parse properties
        Properties prop = new Properties();
        InputStream is = null;

        try {
            is = new FileInputStream(SETTE_PROPERTIES);
            prop.load(is);
        } catch (IOException e) {
            System.err.println("Parsing  " + SETTE_PROPERTIES
                    + " has failed");
            e.printStackTrace();
            System.exit(1);
        } finally {
            IOUtils.closeQuietly(is);
        }

        String[] basedirs = StringUtils.split(
                prop.getProperty("basedir"), '|');
        String snippetDir = prop.getProperty("snippet-dir");
        String snippetProject = prop.getProperty("snippet-project");
        String catgPath = prop.getProperty("catg");
        String catgVersionFile = prop.getProperty("catg-version-file");
        String jPETPath = prop.getProperty("jpet");
        String jPETDefaultBuildXml = prop
                .getProperty("jpet-default-build.xml");
        String jPETVersionFile = prop.getProperty("jpet-version-file");
        String spfPath = prop.getProperty("spf");
        String spfDefaultBuildXml = prop
                .getProperty("spf-default-build.xml");
        String spfVersionFile = prop.getProperty("spf-version-file");
        String outputDir = prop.getProperty("output-dir");

        Validate.notEmpty(basedirs,
                "At least one basedir must be specified in "
                        + SETTE_PROPERTIES);
        Validate.notBlank(snippetDir,
                "The property snippet-dir must be set in "
                        + SETTE_PROPERTIES);
        Validate.notBlank(snippetProject,
                "The property snippet-project must be set in "
                        + SETTE_PROPERTIES);
        Validate.notBlank(catgPath, "The property catg must be set in "
                + SETTE_PROPERTIES);
        Validate.notBlank(jPETPath, "The property jpet must be set in "
                + SETTE_PROPERTIES);
        Validate.notBlank(spfPath, "The property spf must be set in "
                + SETTE_PROPERTIES);
        Validate.notBlank(outputDir,
                "The property output-dir must be set in "
                        + SETTE_PROPERTIES);

        String basedir = null;
        for (String bd : basedirs) {
            bd = StringUtils.trimToEmpty(bd);

            if (bd.startsWith("~")) {
                // Linux home
                bd = System.getProperty("user.home") + bd.substring(1);
            }

            FileValidator v = new FileValidator(new File(bd));
            v.type(FileType.DIRECTORY);

            if (v.isValid()) {
                basedir = bd;
                break;
            }
        }

        if (basedir == null) {
            System.err
            .println("basedir = " + Arrays.toString(basedirs));
            System.err
            .println("ERROR: No valid basedir was found, please check "
                    + SETTE_PROPERTIES);
            System.exit(2);
        }

        BASEDIR = new File(basedir);
        SNIPPET_DIR = new File(basedir, snippetDir);
        SNIPPET_PROJECT = snippetProject;
        OUTPUT_DIR = new File(basedir, outputDir);

        try {
            new CatgTool(new File(BASEDIR, catgPath),
                    readToolVersion(new File(BASEDIR, catgVersionFile)));
            new JPetTool(new File(BASEDIR, jPETPath), new File(BASEDIR,
                    jPETDefaultBuildXml), readToolVersion(new File(
                            BASEDIR, jPETVersionFile)));
            new SpfTool(new File(BASEDIR, spfPath), new File(BASEDIR,
                    spfDefaultBuildXml), readToolVersion(new File(
                            BASEDIR, spfVersionFile)));

            // TODO stuff
            stuff(args);
        } catch (Exception e) {
            System.err.println(ExceptionUtils.getStackTrace(e));

            ValidatorException vex = (ValidatorException) e;

            for (ValidationException v : vex.getValidator()
                    .getAllExceptions()) {
                v.printStackTrace();
            }

            // System.exit(0);

            e.printStackTrace();
            System.err.println("==========");
            e.printStackTrace();

            if (e instanceof ValidatorException) {
                System.err.println("Details:");
                System.err.println(((ValidatorException) e)
                        .getFullMessage());
            } else if (e.getCause() instanceof ValidatorException) {
                System.err.println("Details:");
                System.err.println(((ValidatorException) e.getCause())
                        .getFullMessage());
            }
        }
    }

    private static String readToolVersion(File versionFile) {
        try {
            return StringUtils.trimToNull(FileUtils
                    .readFileToString(versionFile).replace("\n", "")
                    .replace("\r", ""));
        } catch (IOException e) {
            // TODO handle error
            System.err.println("Cannot read tool version from: "
                    + versionFile);
            return null;
        }
    }

    public static void stuff(String[] args) throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(
                System.in));
        PrintStream out = System.out;

        // print settings
        System.out.println("Base directory: " + BASEDIR);
        System.out.println("Snippet directory: " + SNIPPET_DIR);
        System.out.println("Snippet project name: " + SNIPPET_PROJECT);
        System.out.println("Output directory: " + OUTPUT_DIR);

        System.out.println("CATG directory: "
                + ToolRegister.get(CatgTool.class).getToolDirectory());
        System.out.println("jPET executable: "
                + ToolRegister.get(JPetTool.class).getPetExecutable());
        System.out.println("SPF JAR: "
                + ToolRegister.get(SpfTool.class).getToolJAR());

        System.out.println("Tools:");
        for (Tool tool : ToolRegister.toArray()) {
            System.out.println(String.format(
                    "  %s (Version: %s, Supported Java version: %s)",
                    tool.getName(), tool.getVersion(),
                    tool.getSupportedJavaVersion()));
        }

        // get scenario
        String scenario = Run.readScenario(args, in, out);
        if (scenario == null) {
            return;
        }

        switch (scenario) {
        case "exit":
            break;

        case "generator":
            new GeneratorUI(Run.createSnippetProject(true),
                    Run.readTool(in, out)).run(in, out);
            break;

        case "runner":
            new RunnerUI(Run.createSnippetProject(true), Run.readTool(
                    in, out)).run(in, out);
            break;

        case "parser":
            new ParserUI(Run.createSnippetProject(true), Run.readTool(
                    in, out)).run(in, out);
            break;

        case "tests-generator":
            new TestSuiteGenerator(Run.createSnippetProject(true),
                    OUTPUT_DIR, Run.readTool(in, out)).generate();
            break;

        case "tests-run":
            new TestSuiteRunner(Run.createSnippetProject(true),
                    OUTPUT_DIR, Run.readTool(in, out)).analyze();
            break;

        case "snippet-browser":
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    try {
                        SnippetBrowser frame = new SnippetBrowser(Run
                                .createSnippetProject(true));
                        frame.setVisible(true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            break;

        case "export-csv":
            exportCSV(Run.createSnippetProject(true));
            break;

        default:
            throw new UnsupportedOperationException(
                    "Scenario has not been implemented yet: "
                            + scenario);
        }
    }

    private static SnippetProjectSettings createSnippetProjectSettings()
            throws ValidatorException {
        return new SnippetProjectSettings(new File(SNIPPET_DIR,
                SNIPPET_PROJECT));
    }

    private static SnippetProject createSnippetProject(boolean parse)
            throws SetteException {
        SnippetProject ret = new SnippetProject(
                Run.createSnippetProjectSettings());
        if (parse) {
            ret.parse();
        }
        return ret;
    }

    private static String readScenario(String[] args,
            BufferedReader in, PrintStream out) throws IOException {
        String scenario = null;

        if (args.length > 1) {
            out.println("Usage: java -jar SETTE.jar [scenario]");
            out.println("Available scenarios:");
            for (int i = 0; i < Run.scenarios.length; i++) {
                out.println(String.format("  [%d] %s", i,
                        Run.scenarios[i]));
            }
        } else if (args.length == 1) {
            scenario = Run.parseScenario(args[0]);
            if (scenario == null) {
                out.println("Invalid scenario: " + args[0].trim());
                out.println("Available scenarios:");
                for (int i = 0; i < Run.scenarios.length; i++) {
                    out.println(String.format("  [%d] %s", i,
                            Run.scenarios[i]));
                }
            }
        } else {
            while (scenario == null) {
                out.println("Available scenarios:");
                for (int i = 0; i < Run.scenarios.length; i++) {
                    out.println(String.format("  [%d] %s", i,
                            Run.scenarios[i]));
                }

                out.print("Select scenario: ");

                String line = in.readLine();

                if (line == null) {
                    out.println("EOF detected, exiting");
                    return null;
                } else if (StringUtils.isBlank(line)) {
                    out.println("Exiting");
                    return null;
                }

                scenario = Run.parseScenario(line);
                if (scenario == null) {
                    out.println("Invalid scenario: " + line.trim());
                }
            }
        }

        out.println("Selected scenario: " + scenario);
        return scenario;
    }

    private static String parseScenario(String scenario) {
        scenario = scenario.trim();
        int idx = ArrayUtils.indexOf(Run.scenarios,
                scenario.toLowerCase());

        if (idx >= 0) {
            return Run.scenarios[idx];
        } else {
            try {
                return Run.scenarios[Integer.parseInt(scenario)];
            } catch (Exception e) {
                return null;
            }
        }
    }

    private static Tool readTool(BufferedReader in, PrintStream out)
            throws IOException {
        // select tool
        Tool[] tools = ToolRegister.toArray();
        Tool tool = null;
        while (tool == null) {
            out.println("Available tools:");
            for (int i = 0; i < tools.length; i++) {
                out.println(String.format("  [%d] %s", i + 1,
                        tools[i].getName()));
            }

            out.print("Select tool: ");

            String line = in.readLine();

            if (line == null) {
                out.println("EOF detected, exiting");
                return null;
            } else if (StringUtils.isBlank(line)) {
                out.println("Exiting");
                return null;
            }

            line = line.trim();
            int idx = -1;

            for (int i = 0; i < tools.length; i++) {
                if (tools[i].getName().equalsIgnoreCase(line)) {
                    idx = i;
                    break;
                }
            }

            if (idx >= 0) {
                tool = tools[idx];
            } else {
                try {
                    tool = tools[Integer.parseInt(line) - 1];
                } catch (Exception e) {
                    tool = null;
                }
            }

            if (tool == null) {
                out.println("Invalid tool: " + line.trim());
            }
        }

        out.println("Selected tool: " + tool.getName());

        return tool;
    }

    private Run() {
        throw new UnsupportedOperationException("Static class");
    }

    private static void exportCSV(SnippetProject snippetProject)
            throws Exception {
        // TODO enhance this method
        Tool[] tools = ToolRegister.toArray();

        Arrays.sort(tools, new Comparator<Tool>() {
            @Override
            public int compare(Tool o1, Tool o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });

        List<String> columns = new ArrayList<>();
        columns.add("Category");
        columns.add("Goal");
        columns.add("Container");
        columns.add("Required Java version");
        columns.add("Snippet");
        columns.add("Required coverage");

        Map<Tool, RunnerProjectSettings<Tool>> rpss = new HashMap<>();
        ResultType[] resultTypes = ResultType.values();

        for (Tool tool : tools) {
            rpss.put(
                    tool,
                    new RunnerProjectSettings<>(snippetProject
                            .getSettings(), OUTPUT_DIR, tool));

            for (ResultType resultType : resultTypes) {
                columns.add(resultType.toString() + " - "
                        + tool.getName());
            }
        }

        StringBuilder sb = new StringBuilder();
        for (String column : columns) {
            sb.append(column).append(',');
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append("\n");

        for (SnippetContainer container : snippetProject.getModel()
                .getContainers()) {
            for (Snippet snippet : container.getSnippets().values()) {
                sb.append(container.getCategory()).append(",");
                sb.append(container.getGoal()).append(",");
                sb.append(container.getJavaClass().getName()).append(
                        ",");
                sb.append(container.getRequiredJavaVersion()).append(
                        ",");
                sb.append(snippet.getMethod().getName()).append(",");
                sb.append(snippet.getRequiredStatementCoverage() + "%");

                for (Tool tool : tools) {
                    RunnerProjectSettings<Tool> set = rpss.get(tool);
                    File inputs = RunnerProjectUtils
                            .getSnippetInputsFile(set, snippet);
                    File result = RunnerProjectUtils
                            .getSnippetResultFile(set, snippet);

                    if (result.exists()) {
                        System.out.println(tool.getFullName());
                        System.out.println(snippet.getMethod());
                        // TODO error handling
                        throw new RuntimeException("RESULT EXISTS");
                    }

                    ResultType rt;

                    SnippetInputsXml snippetInputsXml;
                    if (!inputs.exists()) {
                        // TODO input should exist, revise this section
                        // System.out.println(tool.getFullName());
                        // System.out.println(snippet.getMethod());
                        // throw new RuntimeException("INPUT NOT EXISTS");
                        rt = ResultType.NA;
                    } else {
                        Serializer serializer = new Persister(
                                new AnnotationStrategy());

                        snippetInputsXml = serializer.read(
                                SnippetInputsXml.class, inputs);
                        snippetInputsXml.validate();
                        rt = snippetInputsXml.getResultType();
                    }

                    int pos = ArrayUtils.indexOf(resultTypes, rt);

                    for (int i = 0; i < pos; i++) {
                        sb.append(",");
                    }
                    sb.append(",1");
                    for (int i = pos + 1; i < resultTypes.length; i++) {
                        sb.append(",");
                    }
                }

                sb.append("\n");
            }
        }

        System.out.println(sb.toString());

        // StringBuilder sb = new StringBuilder(testCaseToolInputs.size() *
        // 100);
        //
        // if (testCaseToolInputs.size() <= 0)
        // return sb.append("No data");
        //
        // List<Tool> tools = new ArrayList<>(testCases.get(0)
        // .generatedToolInputs().keySet());
        // Collections.sort(tools);
        //
        // sb.append(";;");
        // for (Tool tool : tools) {
        // sb.append(';').append(tool.getName()).append(";;;;;");
        // }
        //
        // sb.append('\n');
        //
        // sb.append("Package;Class;Test case");
        //
        // for (int i = 0; i < tools.size(); i++) {
        // sb.append(";N/A;EX;T/M;NC;C;Note");
        // }
        //
        // sb.append('\n');
        //
        // Collections.sort(testCases);
        //
        // for (TestCase tc : testCases) {
        // sb.append(tc.getPkg()).append(';');
        // sb.append(tc.getCls()).append(';');
        // sb.append(tc.getName());
        //
        // for (Tool tool : tools) {
        // TestCaseToolInput tcti = tc.generatedToolInputs().get(tool);
        //
        // switch (tcti.getResult()) {
        // case NA:
        // // sb.append(";1;0;0;0;0;");
        // sb.append(";X;;;;;");
        // break;
        // case EX:
        // // sb.append(";0;1;0;0;0;");
        // sb.append(";;X;;;;");
        // break;
        // case TM:
        // // sb.append(";0;0;1;0;0;");
        // sb.append(";;;X;;;");
        // break;
        // case NC:
        // // sb.append(";0;0;0;1;0;");
        // sb.append(";;;;X;;");
        // break;
        // case C:
        // // sb.append(";0;0;0;0;1;");
        // sb.append(";;;;;X;");
        // break;
        // case UNKNOWN:
        // default:
        // sb.append(";UNKNOWN;UNKNOWN;UNKNOWN;UNKNOWN;UNKNOWN;");
        // break;
        //
        // }
        //
        // sb.append(tcti.getNote());
        // }
        //
        // sb.append('\n');
        // }
        //
        // return sb;

    }
}
