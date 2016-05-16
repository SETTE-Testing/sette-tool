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
package hu.bme.mit.sette.core.model.runner;

import static com.google.common.base.Preconditions.checkArgument;

import java.nio.file.Path;
import java.util.List;

import hu.bme.mit.sette.core.model.snippet.Snippet;
import hu.bme.mit.sette.core.model.xml.SnippetCoverageXml;
import hu.bme.mit.sette.core.model.xml.SnippetInfoXml;
import hu.bme.mit.sette.core.model.xml.SnippetInputsXml;
import hu.bme.mit.sette.core.model.xml.SnippetResultXml;
import hu.bme.mit.sette.core.util.io.PathUtils;
import hu.bme.mit.sette.core.util.xml.XmlElement;
import hu.bme.mit.sette.core.util.xml.XmlException;
import hu.bme.mit.sette.core.util.xml.XmlUtils;
import lombok.Getter;
import lombok.NonNull;

// FIXME future replacement for RunnerProjectSettings abnd RunnerProjectUtils
/*
 * better api for runner projects
 * 
 * features
 *  - get err/out/info files for a snippet
 */
public final class RunnerProjectSnippet implements Comparable<RunnerProjectSnippet> {
    @Getter
    private final RunnerProject runnerProject;

    @Getter
    private final Snippet snippet;

    public RunnerProjectSnippet(@NonNull RunnerProject runnerProject, @NonNull Snippet snippet) {
        if (runnerProject.getSnippetProject() != snippet.getContainer().getSnippetProject()) {
            throw new IllegalStateException(String.format(
                    "The runner project and the snippet belong to different snippet projects:"
                            + " %s != %s",
                    runnerProject.getSnippetProject(), snippet.getContainer().getSnippetProject()));
        }

        this.runnerProject = runnerProject;
        this.snippet = snippet;
    }

    public Path getInfoXmlFile() {
        return getSnippetFile("info.xml");
    }

    public SnippetInfoXml readInfoXml() throws XmlException {
        return deserializeSnippetFile(SnippetInfoXml.class, getInfoXmlFile());
    }

    public void writeInfoXml(@NonNull SnippetInfoXml info) throws XmlException {
        serializeSnippetFile(info, getInfoXmlFile());
    }

    public Path getOutputFile() {
        return getSnippetFile("out");
    }

    public List<String> readOutputLines() {
        return PathUtils.readAllLinesOrEmpty(getOutputFile());
    }

    public Path getErrorOutputFile() {
        return getSnippetFile("err");
    }

    public List<String> readErrorOutputLines() {
        return PathUtils.readAllLinesOrEmpty(getErrorOutputFile());
    }

    public Path getInputsXmlFile() {
        return getSnippetFile("inputs.xml");
    }

    public SnippetInputsXml readInputsXml() throws XmlException {
        return deserializeSnippetFile(SnippetInputsXml.class, getInputsXmlFile());
    }

    public void writeInputsXml(@NonNull SnippetInputsXml inputsXml) throws XmlException {
        serializeSnippetFile(inputsXml, getInputsXmlFile());
    }

    public Path getResultXmlFile() {
        return getSnippetFile("result.xml");
    }

    public SnippetResultXml readResultXml() throws XmlException {
        return deserializeSnippetFile(SnippetResultXml.class, getResultXmlFile());
    }

    public void writeResultXml(@NonNull SnippetResultXml resultXml) throws XmlException {
        serializeSnippetFile(resultXml, getResultXmlFile());
    }

    public Path getCoverageXmlFile() {
        return getSnippetFile("coverage.xml");
    }

    public SnippetCoverageXml readCoverageXml() throws XmlException {
        return deserializeSnippetFile(SnippetCoverageXml.class, getCoverageXmlFile());
    }

    public void writeCoverageXml(@NonNull SnippetCoverageXml coverageXml) throws XmlException {
        serializeSnippetFile(coverageXml, getCoverageXmlFile());
    }

    public Path getCoverageHtmlFile() {
        return getSnippetFile("html");
    }

    private Path getSnippetFile(@NonNull String extension) {
        checkArgument(!extension.isEmpty());

        String baseName = snippet.getContainer().getJavaClass().getName().replace('.', '/') + "_"
                + snippet.getMethod().getName();
        String relativePath = baseName + '.' + extension;

        return runnerProject.getRunnerOutputDir().resolve(relativePath);
    }

    private static void serializeSnippetFile(@NonNull XmlElement object, @NonNull Path file)
            throws XmlException {
        XmlUtils.serializeToXml(object, file);
    }

    private static <T extends XmlElement> T deserializeSnippetFile(@NonNull Class<T> cls,
            @NonNull Path file) throws XmlException {
        if (PathUtils.exists(file)) {
            return XmlUtils.deserializeFromXml(cls, file);
        } else {
            return null;
        }
    }

    @Override
    public int compareTo(@NonNull RunnerProjectSnippet o) {// NOSONAR: default equals() and
                                                           // hashCode()
        return snippet.compareTo(o.snippet);
    }
}
