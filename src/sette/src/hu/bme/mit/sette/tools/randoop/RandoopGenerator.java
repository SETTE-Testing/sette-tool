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
package hu.bme.mit.sette.tools.randoop;

import hu.bme.mit.sette.common.descriptors.eclipse.EclipseProject;
import hu.bme.mit.sette.common.exceptions.SetteException;
import hu.bme.mit.sette.common.model.snippet.SnippetProject;
import hu.bme.mit.sette.common.tasks.RunnerProjectGenerator;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

public class RandoopGenerator extends RunnerProjectGenerator<RandoopTool> {
    public RandoopGenerator(SnippetProject snippetProject, File outputDirectory, RandoopTool tool,
            String runnerProjectTag) {
        super(snippetProject, outputDirectory, tool, runnerProjectTag);
    }

    @Override
    protected void afterWriteRunnerProject(EclipseProject eclipseProject)
            throws IOException, SetteException {
        File buildXml = new File(getRunnerProjectSettings().getBaseDirectory(), "build.xml");
        FileUtils.copyFile(getTool().getDefaultBuildXml(), buildXml);
    }
}
