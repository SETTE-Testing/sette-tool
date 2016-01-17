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
package hu.bme.mit.sette.application;

import java.io.BufferedReader;
import java.io.PrintStream;
import java.nio.file.Path;

import hu.bme.mit.sette.core.model.snippet.SnippetProject;
import hu.bme.mit.sette.core.tool.Tool;
import lombok.Data;

@Data
// no builder to force to set all the fields (compile-time)
public final class ExecutionContext {
    private final BufferedReader input;
    private final PrintStream output;
    private final PrintStream errorOutput;

    private final SnippetProject snippetProject;
    private final Tool tool;
    private final String runnerProjectTag;
    private final int runnerTimeoutInMs;
    private final BackupPolicy backupPolicy;
    private final Path outputDir;
}
