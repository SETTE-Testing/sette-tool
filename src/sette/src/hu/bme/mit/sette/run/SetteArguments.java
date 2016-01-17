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
package hu.bme.mit.sette.run;



public class SetteArguments {
//    @OptionSó
//    
//
//    public static void stuff(String[] args) throws Exception {
//        // Get in/out streams
//        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
//        PrintStream out = System.out;
//
//        // Parse arguments
//        /*
//         * Examples:
//         * 
//         * ./sette.sh --task generator --tool CATG
//         * 
//         * ./sette.sh --task generator --tool CATG --runner-project-tag "1st-run" --runner-timeout
//         * 30 --skip-backup
//         * 
//         * ./sette.sh --help
//         * 
//         */
//        // create the command line parser
//        CommandLineParser parser = new DefaultParser();
//
//        // create the Options
//        // NOTE consider using something better, e.g. JCommander
//        Options options = new Options();
//
//        String separatedToolNames = Joiner.on(", ").join(ToolRegister.toMap().keySet());
//
//        Option helpOption = Option.builder("h").longOpt("help").desc("Prints this help message")
//                .build();
//
//        Option taskOption = Option.builder().longOpt("task").hasArg().argName("TASK")
//                .desc(String.format("Task to execute (%s)", String.join(", ", TASKS))).build();
//
//        Option toolOption = Option.builder().longOpt("tool").hasArg().argName("TOOL")
//                .desc(String.format("Tool to use (%s)", separatedToolNames)).build();
//
//        Option runnerProjectTagOption = Option.builder().longOpt("runner-project-tag").hasArg()
//                .argName("TAG").desc("The tag of the desired runner project").build();
//
//        Option skipBackupOption = Option.builder().longOpt("skip-backup")
//                .desc("Skip backup without asking when generating a runner project that already exists")
//                .build();
//
//        Option createBackupOption = Option.builder().longOpt("create-backup")
//                .desc("Create backup without asking when generating a runner project that already exists")
//                .build();
//
//        Option runnerTimeoutOption = Option.builder().longOpt("runner-timeout").hasArg()
//                .argName("SEC")
//                .desc("Timeout for execution of a tool on one snippet (in seconds) - "
//                        + "if missing then the setting in sette.properties will be used")
//                .build();
//
//        Option snippetProjectOption = Option.builder().longOpt("snippet-project").hasArg()
//                .argName("PROJECT_NAME").desc("Name of the snippet project use - "
//                        + "if missing then the setting in sette.properties will be used")
//                .build();
//
//        options.addOption(helpOption).addOption(taskOption).addOption(toolOption)
//                .addOption(runnerProjectTagOption).addOption(skipBackupOption)
//                .addOption(createBackupOption).addOption(runnerTimeoutOption)
//                .addOption(snippetProjectOption);
//
//        String task, toolName, runnerProjectTag;
//
//        try {
//            // parse the command line arguments
//            CommandLine line = parser.parse(options, args, false);
//
//            if (line.hasOption("h")) {
//                new HelpFormatter().printHelp("sette", options, true);
//                System.exit(1);
//            }
//
//            task = line.getOptionValue("task");
//
//            toolName = line.getOptionValue("tool");
//            runnerProjectTag = line.getOptionValue("runner-project-tag");
//            SKIP_BACKUP = line.hasOption("skip-backup");
//            CREATE_BACKUP = line.hasOption("create-backup");
//
//            if (SKIP_BACKUP && CREATE_BACKUP) {
//                System.out.println("Cannot both skip ad create a backup");
//                System.exit(1);
//                return;
//            }
//
//            if (line.hasOption("runner-timeout")) {
//                RUNNER_TIMEOUT_IN_MS = parseRunnerTimeout(line.getOptionValue("runner-timeout"));
//            }
//
//            if (line.hasOption("snippet-project")) {
//                SNIPPET_PROJECT = line.getOptionValue("snippet-project");
//                Validate.notBlank(SNIPPET_PROJECT, "The snippet-project must not be blank");
//            }
//        } catch (ParseException ex) {
//            System.out.println("Cannot parse arguments: " + ex.getMessage());
//            new HelpFormatter().printHelp("sette", options, true);
//            System.exit(1);
//            return;
//        }
//
//        // print settings
//        out.println("Base directory: " + BASEDIR);
//        out.println("Snippet directory: " + SNIPPET_DIR);
//        out.println("Snippet project name: " + SNIPPET_PROJECT);
//        out.println("Output directory: " + OUTPUT_DIR);
//
//        if (ToolRegister.get(CatgTool.class) != null) {
//            out.println("CATG directory: " + ToolRegister.get(CatgTool.class).getDir());
//        }
//        if (ToolRegister.get(JPetTool.class) != null) {
//            out.println("jPET executable: " + ToolRegister.get(JPetTool.class).getPetExecutable());
//        }
//        if (ToolRegister.get(SpfTool.class) != null) {
//            out.println("SPF JAR: " + ToolRegister.get(SpfTool.class).getToolJAR());
//        }
//        if (ToolRegister.get(EvoSuiteTool.class) != null) {
//            out.println("EvoSuite JAR: " + ToolRegister.get(EvoSuiteTool.class).getToolJAR());
//        }
//        if (ToolRegister.get(RandoopTool.class) != null) {
//            out.println("Randoop JAR: " + ToolRegister.get(RandoopTool.class).getToolJAR());
//        }
//
//        out.println("Tools:");
//        for (Tool tool : ToolRegister.toMap().values()) {
//            out.println(String.format("  %s (Version: %s, Supported Java version: %s)",
//                    tool.getName(), tool.getVersion(), tool.getSupportedJavaVersion()));
//        }
//
//        // get task
//        if (task == null) {
//            task = Run.readTask(in, out);
//        }
//
//        if (task == null || "exit".equals(task)) {
//            return;
//        }
//
//        SnippetProject snippetProject = Run.createSnippetProject();
//        // NOTE shortcut to batch csv
//        if ("export-csv-batch".equals(task)) {
//            new CsvBatchGenerator(snippetProject, OUTPUT_DIR, toolName, runnerProjectTag)
//                    .generateAll();
//        } else {
//            Tool tool;
//            if (toolName == null) {
//                tool = Run.readTool(in, out);
//            } else {
//                tool = ToolRegister.get(toolName);
//
//                if (tool == null) {
//                    // NOTE enhance
//                    System.err.println("Invalid tool: " + toolName);
//                    System.exit(1);
//                    return;
//                }
//            }
//
//            while (StringUtils.isBlank(runnerProjectTag)) {
//                out.print("Enter a runner project tag: ");
//                out.flush();
//                runnerProjectTag = in.readLine();
//
//                if (runnerProjectTag == null) {
//                    out.println("Exiting...");
//                    System.exit(1);
//                    return;
//                }
//            }
//
//            runnerProjectTag = runnerProjectTag.trim();
//
//            switch (task) {
//                case "generator":
//                    new GeneratorUI(snippetProject, tool, runnerProjectTag).run(in, out);
//                    break;
//
//                case "runner":
//                    new RunnerUI(snippetProject, tool, runnerProjectTag, RUNNER_TIMEOUT_IN_MS)
//                            .run(in, out);
//                    break;
//
//                case "parser":
//                    new ParserUI(snippetProject, tool, runnerProjectTag).run(in, out);
//                    break;
//
//                case "test-generator":
//                    // NOTE now the generator skips the test suite generation and only generates the
//                    // ant
//                    // build file
//                    // if (tool.getOutputType() == ToolOutputType.INPUT_VALUES) {
//                    new TestSuiteGenerator(snippetProject, OUTPUT_DIR, tool, runnerProjectTag)
//                            .generate();
//                    // } else {
//                    // out.println("This tool has already generated a test suite");
//                    // }
//                    break;
//
//                case "test-runner":
//                    new TestSuiteRunner(snippetProject, OUTPUT_DIR, tool, runnerProjectTag)
//                            .analyze();
//                    break;
//
//                case "snippet-browser":
//                    EventQueue.invokeLater(new Runnable() {
//                        @Override
//                        public void run() {
//                            try {
//                                SnippetBrowser frame = new SnippetBrowser(snippetProject);
//                                frame.setVisible(true);
//                            } catch (Exception ex) {
//                                ex.printStackTrace();
//                            }
//                        }
//                    });
//                    break;
//
//                case "export-csv":
//                    new CsvGenerator(snippetProject, OUTPUT_DIR, tool, runnerProjectTag).generate();
//                    // NOTE old code
//                    // out.print("Target file: ");
//                    // String file = in.readLine();
//                    // exportCsvOld(snippetProject, new File(file), runnerProjectTag);
//                    break;
//
//                default:
//                    throw new UnsupportedOperationException(
//                            "Task has not been implemented yet: " + task);
//            }
//        }
    //    }
}
