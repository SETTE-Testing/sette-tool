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

import java.io.IOException;
import java.nio.file.Path;

import com.google.common.io.Resources;

import hu.bme.mit.sette.core.SetteException;
import hu.bme.mit.sette.core.descriptors.eclipse.EclipseClasspathEntry;
import hu.bme.mit.sette.core.descriptors.eclipse.EclipseClasspathEntryKind;
import hu.bme.mit.sette.core.descriptors.eclipse.EclipseProject;
import hu.bme.mit.sette.core.model.snippet.SnippetProject;
import hu.bme.mit.sette.core.tasks.RunnerProjectGenerator;
import hu.bme.mit.sette.core.util.io.PathUtils;

public class SnippetInputCheckerGenerator extends RunnerProjectGenerator<SnippetInputCheckerTool> {
    public SnippetInputCheckerGenerator(SnippetProject snippetProject, Path outputDir,
            SnippetInputCheckerTool tool, String runnerProjectTag) {
        super(snippetProject, outputDir, tool, runnerProjectTag);
    }

    @Override
    protected void afterPrepareRunnerProject(EclipseProject eclipseProject) {
        EclipseClasspathEntry ec = new EclipseClasspathEntry(EclipseClasspathEntryKind.SOURCE,
                "snippet-input-src");
        eclipseProject.getClasspathDescriptor().addEntry(ec);

        ec = new EclipseClasspathEntry(EclipseClasspathEntryKind.SOURCE, "test");
        eclipseProject.getClasspathDescriptor().addEntry(ec);

        ec = new EclipseClasspathEntry(EclipseClasspathEntryKind.SOURCE, "/sette-common");
        eclipseProject.getClasspathDescriptor().addEntry(ec);

        ec = new EclipseClasspathEntry(EclipseClasspathEntryKind.CONTAINER,
                "org.eclipse.jdt.junit.JUNIT_CONTAINER/4");
        eclipseProject.getClasspathDescriptor().addEntry(ec);
    }

    @Override
    protected void afterWriteRunnerProject(EclipseProject eclipseProject)
            throws IOException, SetteException {
        Path baseDir = getRunnerProjectSettings().getBaseDir().toPath();

        // delete generated snippet inputs and copy with SETTE annotations
        Path snippetSourceDir = getRunnerProjectSettings().getSnippetSourceDirectory().toPath();
        PathUtils.delete(snippetSourceDir);
        PathUtils.copy(getSnippetProject().getSourceDir(), snippetSourceDir);

        // copy snippet input sources
        Path originalInputSourceDir = getSnippetProject().getInputSourceDir();
        String inputSourceDirname = originalInputSourceDir.getFileName().toString();
        PathUtils.copy(originalInputSourceDir, baseDir.resolve(inputSourceDirname));

        // copy build.xml
        PathUtils.copy(Resources.getResource("snippet-input-checker-build.xml").openStream(),
                baseDir.resolve("build.xml"));
        PathUtils.copy(Resources.getResource("snippet-input-checker-build-test.xml").openStream(),
                baseDir.resolve("build-test.xml"));
    }
}
