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
package hu.bme.mit.sette.tools.jpet;

  import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import hu.bme.mit.sette.core.SetteException;
import hu.bme.mit.sette.core.descriptors.eclipse.EclipseProject;
import hu.bme.mit.sette.core.model.snippet.SnippetProject;
import hu.bme.mit.sette.core.tasks.RunnerProjectGenerator;

public class JPetGenerator extends RunnerProjectGenerator<JPetTool> {
    public JPetGenerator(SnippetProject snippetProject, File outputDirectory, JPetTool tool,
            String runnerProjectTag) {
        super(snippetProject, outputDirectory, tool, runnerProjectTag);
    }

    @Override
    protected void afterWriteRunnerProject(EclipseProject eclipseProject)
            throws IOException, SetteException {
        File buildXml = new File(getRunnerProjectSettings().getBaseDir(), "build.xml");
        Files.copy(getTool().getDefaultBuildXml(), buildXml.toPath());
    }
}
