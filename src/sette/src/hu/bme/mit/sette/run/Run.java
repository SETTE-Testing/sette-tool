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
package hu.bme.mit.sette.run;

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
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
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

import hu.bme.mit.sette.GeneratorUI;
import hu.bme.mit.sette.ParserUI;
import hu.bme.mit.sette.RunnerUI;
import hu.bme.mit.sette.common.Tool;
import hu.bme.mit.sette.common.ToolOutputType;
import hu.bme.mit.sette.common.ToolRegister;
import hu.bme.mit.sette.common.exceptions.SetteException;
import hu.bme.mit.sette.common.model.parserxml.SnippetInputsXml;
import hu.bme.mit.sette.common.model.runner.ResultType;
import hu.bme.mit.sette.common.model.runner.RunnerProjectSettings;
import hu.bme.mit.sette.common.model.runner.RunnerProjectUtils;
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
import hu.bme.mit.sette.tools.evosuite.EvoSuiteTool;
import hu.bme.mit.sette.tools.jpet.JPetTool;
import hu.bme.mit.sette.tools.randoop.RandoopTool;
import hu.bme.mit.sette.tools.spf.SpfTool;

public final class Run {
    private static final Logger LOG = LoggerFactory.getLogger(Run.class);
    private static final String SETTE_PROPERTIES = "sette.properties";

    public static File BASEDIR;
    public static File SNIPPET_DIR;
    public static String SNIPPET_PROJECT;
    public static File OUTPUT_DIR;
    public static int RUNNER_TIMEOUT_IN_MS;
    public static boolean SKIP_BACKUP = false;
    public static boolean CREATE_BACKUP = false;

    private static final String[] TASKS = new String[] { "exit", "generator", "runner", "parser",
            "test-generator", "test-runner", "snippet-browser", "export-csv" };

    public static void main(String[] args) {
        LOG.debug("main() called");

        //
        // Parse properties and init tools
        //
        // NOTE divide into class/methods
        Properties prop = new Properties();
        InputStream is = null;

        try {
            is = new FileInputStream(SETTE_PROPERTIES);
            prop.load(is);
        } catch (IOException ex) {
            System.err.println("Parsing  " + SETTE_PROPERTIES + " has failed");
            ex.printStackTrace();
            System.exit(1);
        } finally {
            IOUtils.closeQuietly(is);
        }

        String[] basedirs = StringUtils.split(prop.getProperty("basedir"), '|');
        String snippetDir = prop.getProperty("snippet-dir");
        String snippetProject = prop.getProperty("snippet-project");
        String catgPath = prop.getProperty("catg");
        String catgVersionFile = prop.getProperty("catg-version-file");
        String jPETPath = prop.getProperty("jpet");
        String jPETDefaultBuildXml = prop.getProperty("jpet-default-build.xml");
        String jPETVersionFile = prop.getProperty("jpet-version-file");
        String spfPath = prop.getProperty("spf");
        String spfDefaultBuildXml = prop.getProperty("spf-default-build.xml");
        String spfVersionFile = prop.getProperty("spf-version-file");
        String evoSuitePath = prop.getProperty("evosuite");
        String evoSuiteVersionFile = prop.getProperty("evosuite-version-file");
        String evoSuiteDefaultBuildXml = prop.getProperty("evosuite-default-build.xml");
        String randoopPath = prop.getProperty("randoop");
        String randoopVersionFile = prop.getProperty("randoop-version-file");
        String randoopDefaultBuildXml = prop.getProperty("randoop-default-build.xml");
        String outputDir = prop.getProperty("output-dir");

        String runnerTimeout = prop.getProperty("runner-timeout", "30");

        Validate.notEmpty(basedirs,
                "At least one basedir must be specified in " + SETTE_PROPERTIES);
        Validate.notBlank(snippetDir,
                "The property snippet-dir must be set in " + SETTE_PROPERTIES);
        Validate.notBlank(snippetProject,
                "The property snippet-project must be set in " + SETTE_PROPERTIES);
        Validate.notBlank(catgPath, "The property catg must be set in " + SETTE_PROPERTIES);
        Validate.notBlank(jPETPath, "The property jpet must be set in " + SETTE_PROPERTIES);
        Validate.notBlank(spfPath, "The property spf must be set in " + SETTE_PROPERTIES);
        Validate.notBlank(evoSuitePath, "The property evosuite must be set in " + SETTE_PROPERTIES);
        Validate.notBlank(randoopPath, "The property randoop must be set in " + SETTE_PROPERTIES);
        Validate.notBlank(outputDir, "The property output-dir must be set in " + SETTE_PROPERTIES);

        // select appropriate basedir from the list
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
                // first found
                basedir = bd;
                break;
            }
        }

        // no valid basedir found
        if (basedir == null) {
            System.err.println("basedir = " + Arrays.toString(basedirs));
            System.err
                    .println("ERROR: No valid basedir was found, please check " + SETTE_PROPERTIES);
            System.exit(2);
        }

        // save settings
        BASEDIR = new File(basedir);
        SNIPPET_DIR = new File(basedir, snippetDir);
        SNIPPET_PROJECT = snippetProject;
        OUTPUT_DIR = new File(basedir, outputDir);
        RUNNER_TIMEOUT_IN_MS = parseRunnerTimeout(runnerTimeout);

        // create tools
        try {
            String catgVersion = readToolVersion(new File(BASEDIR, catgVersionFile));
            if (catgVersion != null) {
                new CatgTool(new File(BASEDIR, catgPath), catgVersion).register();
            }

            String jPetVersion = readToolVersion(new File(BASEDIR, jPETVersionFile));
            if (jPetVersion != null) {
                new JPetTool(new File(BASEDIR, jPETPath), new File(BASEDIR, jPETDefaultBuildXml),
                        jPetVersion).register();
            }

            String spfVersion = readToolVersion(new File(BASEDIR, spfVersionFile));
            if (spfVersion != null) {
                new SpfTool(new File(BASEDIR, spfPath), new File(BASEDIR, spfDefaultBuildXml),
                        spfVersion).register();
            }

            String evoSuiteVersion = readToolVersion(new File(BASEDIR, evoSuiteVersionFile));
            if (evoSuiteVersion != null) {
                new EvoSuiteTool(new File(BASEDIR, evoSuitePath),
                        new File(BASEDIR, evoSuiteDefaultBuildXml), evoSuiteVersion).register();
            }

            String randoopVersion = readToolVersion(new File(BASEDIR, randoopVersionFile));
            if (randoopVersion != null) {
                new RandoopTool(new File(BASEDIR, randoopPath),
                        new File(BASEDIR, randoopDefaultBuildXml), randoopVersion).register();
            }

            // TODO stuff
            stuff(args);
        } catch (Exception ex) {
            System.err.println(ExceptionUtils.getStackTrace(ex));

            System.err.println("==========");
            ValidatorException vex = (ValidatorException) ex;

            for (ValidationException v : vex.getValidator().getAllExceptions()) {
                v.printStackTrace();
            }

            // System.exit(0);

            ex.printStackTrace();
            System.err.println("==========");
            ex.printStackTrace();

            if (ex instanceof ValidatorException) {
                System.err.println("Details:");
                System.err.println(((ValidatorException) ex).getFullMessage());
            } else if (ex.getCause() instanceof ValidatorException) {
                System.err.println("Details:");
                System.err.println(((ValidatorException) ex.getCause()).getFullMessage());
            }
        }
    }

    private static int parseRunnerTimeout(String runnerTimeout) {
        try {
            int timeout = Integer.parseInt(runnerTimeout.trim());
            if (timeout <= 0) {
                throw new Exception();
            } else {
                return timeout * 1000;
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException(
                    "The runner-timeout parameter must be a valid, positive number");
        }
    }

    private static String readToolVersion(File versionFile) {
        try {
            return StringUtils.trimToNull(
                    FileUtils.readFileToString(versionFile).replace("\n", "").replace("\r", ""));
        } catch (IOException ex) {
            // TODO handle error
            System.err.println("Cannot read tool version from: " + versionFile);
            return null;
        }
    }

    public static void stuff(String[] args) throws Exception {
        // Get in/out streams
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        PrintStream out = System.out;

        // Parse arguments
        /*
         * Examples:
         * 
         * ./sette.sh --task generator --tool CATG
         * 
         * ./sette.sh --task generator --tool CATG --runner-project-tag "1st-run" --runner-timeout
         * 30 --skip-backup
         * 
         * ./sette.sh --help
         * 
         */
        // create the command line parser
        CommandLineParser parser = new DefaultParser();

        // create the Options
        // NOTE consider using something better, e.g. JCommander
        Options options = new Options();

        String separatedToolNames = Stream.of(ToolRegister.toArray())
                .map(t -> t.getName().toLowerCase()).sorted().collect(Collectors.joining(", "));

        Option helpOption = Option.builder("h").longOpt("help").desc("Prints this help message")
                .build();

        Option taskOption = Option.builder().longOpt("task").hasArg().argName("TASK")
                .desc(String.format("Task to execute (%s)", String.join(", ", TASKS))).build();

        Option toolOption = Option.builder().longOpt("tool").hasArg().argName("TOOL")
                .desc(String.format("Tool to use (%s)", separatedToolNames)).build();

        Option runnerProjectTagOption = Option.builder().longOpt("runner-project-tag").hasArg()
                .argName("TAG").desc("The tag of the desired runner project").build();

        Option skipBackupOption = Option.builder().longOpt("skip-backup")
                .desc("Skip backup without asking when generating a runner project that already exists")
                .build();

        Option createBackupOption = Option.builder().longOpt("create-backup")
                .desc("Create backup without asking when generating a runner project that already exists")
                .build();

        Option runnerTimeoutOption = Option.builder().longOpt("runner-timeout").hasArg()
                .argName("SEC")
                .desc("Timeout for execution of a tool on one snippet (in seconds) - "
                        + "if missing then the setting in sette.properties will be used")
                .build();

        options.addOption(helpOption).addOption(taskOption).addOption(toolOption)
                .addOption(runnerProjectTagOption).addOption(skipBackupOption)
                .addOption(createBackupOption).addOption(runnerTimeoutOption);

        String task, toolName, runnerProjectTag;

        try {
            // parse the command line arguments
            CommandLine line = parser.parse(options, args, false);

            if (line.hasOption("h")) {
                new HelpFormatter().printHelp("sette", options, true);
                System.exit(1);
            }

            task = line.getOptionValue("task");

            toolName = line.getOptionValue("tool");
            runnerProjectTag = line.getOptionValue("runner-project-tag");
            SKIP_BACKUP = line.hasOption("skip-backup");
            CREATE_BACKUP = line.hasOption("create-backup");

            if (SKIP_BACKUP && CREATE_BACKUP) {
                System.out.println("Cannot both skip ad create a backup");
                System.exit(1);
                return;
            }

            if (line.hasOption("runner-timeout")) {
                RUNNER_TIMEOUT_IN_MS = parseRunnerTimeout(line.getOptionValue("runner-timeout"));
            }
        } catch (ParseException ex) {
            System.out.println("Cannot parse arguments: " + ex.getMessage());
            new HelpFormatter().printHelp("sette", options, true);
            System.exit(1);
            return;
        }

        // print settings
        out.println("Base directory: " + BASEDIR);
        out.println("Snippet directory: " + SNIPPET_DIR);
        out.println("Snippet project name: " + SNIPPET_PROJECT);
        out.println("Output directory: " + OUTPUT_DIR);

        if (ToolRegister.get(CatgTool.class) != null) {
            out.println("CATG directory: " + ToolRegister.get(CatgTool.class).getToolDirectory());
        }
        if (ToolRegister.get(JPetTool.class) != null) {
            out.println("jPET executable: " + ToolRegister.get(JPetTool.class).getPetExecutable());
        }
        if (ToolRegister.get(SpfTool.class) != null) {
            out.println("SPF JAR: " + ToolRegister.get(SpfTool.class).getToolJAR());
        }
        if (ToolRegister.get(EvoSuiteTool.class) != null) {
            out.println("EvoSuite JAR: " + ToolRegister.get(EvoSuiteTool.class).getToolJAR());
        }
        if (ToolRegister.get(RandoopTool.class) != null) {
            out.println("Randoop JAR: " + ToolRegister.get(RandoopTool.class).getToolJAR());
        }

        out.println("Tools:");
        for (Tool tool : ToolRegister.toArray()) {
            out.println(String.format("  %s (Version: %s, Supported Java version: %s)",
                    tool.getName(), tool.getVersion(), tool.getSupportedJavaVersion()));
        }

        // get task
        if (task == null) {
            task = Run.readTask(in, out);
        }

        if (task == null || "exit".equals(task)) {
            return;
        }

        SnippetProject snippetProject = Run.createSnippetProject(true);

        Tool tool;
        if (toolName == null) {
            tool = Run.readTool(in, out);
        } else {
            try {
                tool = Stream.of(ToolRegister.toArray())
                        .filter(t -> t.getName().equalsIgnoreCase(toolName)).findFirst().get();
            } catch (NoSuchElementException ex) {
                // NOTE enhance
                System.err.println("Invalid tool: " + toolName);
                System.exit(1);
                return;
            }
        }

        while (StringUtils.isBlank(runnerProjectTag)) {
            out.print("Enter a runner project tag: ");
            out.flush();
            runnerProjectTag = in.readLine();

            if (runnerProjectTag == null) {
                out.println("Exiting...");
                System.exit(1);
                return;
            }
        }

        runnerProjectTag = runnerProjectTag.trim();

        switch (task) {
            case "generator":
                new GeneratorUI(snippetProject, tool, runnerProjectTag).run(in, out);
                break;

            case "runner":
                new RunnerUI(snippetProject, tool, runnerProjectTag, RUNNER_TIMEOUT_IN_MS).run(in,
                        out);
                break;

            case "parser":
                new ParserUI(snippetProject, tool, runnerProjectTag).run(in, out);
                break;

            case "test-generator":
                if (tool.getOutputType() == ToolOutputType.INPUT_VALUES) {
                    new TestSuiteGenerator(snippetProject, OUTPUT_DIR, tool, runnerProjectTag)
                            .generate();
                } else {
                    out.println("This tool has already generated a test suite");
                }
                break;

            case "test-runner":
                new TestSuiteRunner(snippetProject, OUTPUT_DIR, tool, runnerProjectTag).analyze();
                break;

            case "snippet-browser":
                EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            SnippetBrowser frame = new SnippetBrowser(snippetProject);
                            frame.setVisible(true);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });
                break;

            case "export-csv":
                out.print("Target file: ");
                String file = in.readLine();
                exportCSV(snippetProject, new File(file), runnerProjectTag);
                break;

            default:
                throw new UnsupportedOperationException(
                        "Task has not been implemented yet: " + task);
        }
    }

    private static SnippetProjectSettings createSnippetProjectSettings() throws ValidatorException {
        return new SnippetProjectSettings(new File(SNIPPET_DIR, SNIPPET_PROJECT));
    }

    private static SnippetProject createSnippetProject(boolean parse) throws SetteException {
        SnippetProject ret = new SnippetProject(Run.createSnippetProjectSettings());
        if (parse) {
            ret.parse();
        }
        return ret;
    }

    private static String readTask(BufferedReader in, PrintStream out) throws IOException {
        String task = null;

        while (task == null) {
            out.println("Available tasks:");
            for (int i = 0; i < Run.TASKS.length; i++) {
                out.println(String.format("  [%d] %s", i, Run.TASKS[i]));
            }

            out.print("Select task: ");

            String line = in.readLine();

            if (line == null) {
                out.println("EOF detected, exiting");
                return null;
            } else if (StringUtils.isBlank(line)) {
                out.println("Exiting");
                return null;
            }

            task = Run.parseTask(line);
            if (task == null) {
                out.println("Invalid task: " + line.trim());
            }
        }

        out.println("Selected task: " + task);
        return task;
    }

    private static String parseTask(String task) {
        task = task.trim();
        int idx = ArrayUtils.indexOf(Run.TASKS, task.toLowerCase());

        if (idx >= 0) {
            return Run.TASKS[idx];
        } else {
            try {
                return Run.TASKS[Integer.parseInt(task)];
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private static Tool readTool(BufferedReader in, PrintStream out) throws IOException {
        // select tool
        Tool[] tools = ToolRegister.toArray();
        Tool tool = null;
        while (tool == null) {
            out.println("Available tools:");
            for (int i = 0; i < tools.length; i++) {
                out.println(String.format("  [%d] %s", i + 1, tools[i].getName()));
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
                } catch (Exception ex) {
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

    private static void exportCSV(SnippetProject snippetProject, File file, String runnerProjectTag)
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
            rpss.put(tool, new RunnerProjectSettings<>(snippetProject.getSettings(), OUTPUT_DIR,
                    tool, runnerProjectTag));

            for (ResultType resultType : resultTypes) {
                columns.add(resultType.toString() + " - " + tool.getName());
            }
        }

        StringBuilder sb = new StringBuilder();
        for (String column : columns) {
            sb.append(column).append(',');
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append("\n");

        for (SnippetContainer container : snippetProject.getModel().getContainers()) {
            for (Snippet snippet : container.getSnippets().values()) {
                sb.append(container.getCategory()).append(",");
                sb.append(container.getGoal()).append(",");
                sb.append(container.getJavaClass().getName()).append(",");
                sb.append(container.getRequiredJavaVersion()).append(",");
                sb.append(snippet.getMethod().getName()).append(",");
                sb.append(snippet.getRequiredStatementCoverage() + "%");

                for (Tool tool : tools) {
                    RunnerProjectSettings<Tool> set = rpss.get(tool);
                    File inputs = RunnerProjectUtils.getSnippetInputsFile(set, snippet);
                    File result = RunnerProjectUtils.getSnippetResultFile(set, snippet);

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
                        // out.println(tool.getFullName());
                        // out.println(snippet.getMethod());
                        // throw new RuntimeException("INPUT NOT EXISTS");
                        rt = ResultType.NA;
                    } else {
                        Serializer serializer = new Persister(new AnnotationStrategy());

                        snippetInputsXml = serializer.read(SnippetInputsXml.class, inputs);
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

        try {
            FileUtils.write(file, sb);
        } catch (IOException ex) {
            System.err.println("Operation failed");
            ex.printStackTrace();
        }

        // out.println(sb.toString());

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
