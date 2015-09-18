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
package hu.bme.mit.sette.common.tasks;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.Validate;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;

import hu.bme.mit.sette.common.Tool;
import hu.bme.mit.sette.common.descriptors.eclipse.EclipseClasspathDescriptor;
import hu.bme.mit.sette.common.descriptors.eclipse.EclipseClasspathEntry;
import hu.bme.mit.sette.common.descriptors.eclipse.EclipseClasspathEntry.Kind;
import hu.bme.mit.sette.common.descriptors.eclipse.EclipseProject;
import hu.bme.mit.sette.common.exceptions.ConfigurationException;
import hu.bme.mit.sette.common.exceptions.RunnerProjectGeneratorException;
import hu.bme.mit.sette.common.exceptions.SetteException;
import hu.bme.mit.sette.common.exceptions.XmlException;
import hu.bme.mit.sette.common.model.runner.RunnerProjectSettings;
import hu.bme.mit.sette.common.model.snippet.SnippetProject;
import hu.bme.mit.sette.common.snippets.JavaVersion;

/**
 * A SETTE task which provides base for runner project generation. The phases are the following:
 * validation, preparation, writing.
 *
 * @param <T>
 *            the type of the tool
 */
public abstract class RunnerProjectGenerator<T extends Tool> extends SetteTask<T> {
    /** The Eclipse project. */
    private EclipseProject eclipseProject;

    /**
     * Instantiates a new runner project generator.
     *
     * @param snippetProject
     *            the snippet project
     * @param outputDirectory
     *            the output directory
     * @param tool
     *            the tool
     * @param runnerProjectTag
     *            the tag of the runner project
     */
    public RunnerProjectGenerator(SnippetProject snippetProject, File outputDirectory, T tool,
            String runnerProjectTag) {
        super(snippetProject, outputDirectory, tool, runnerProjectTag);
    }

    /**
     * Generates the runner project.
     *
     * @throws RunnerProjectGeneratorException
     *             if generation has failed
     */
    public final void generate() throws RunnerProjectGeneratorException {
        String phase = null;

        try {
            // validate preconditions
            phase = "validate (do)";
            validate();
            phase = "validate (after)";
            afterValidate();

            phase = "prepare runner project (do)";
            prepareRunnerProject();
            phase = "prepare runner project (after)";
            afterPrepareRunnerProject(eclipseProject);

            phase = "write runner project (do)";
            writeRunnerProject();
            phase = "write runner project (after)";
            afterWriteRunnerProject(eclipseProject);

            phase = "complete";
        } catch (Exception xe) {
            String message = String.format(
                    "The runner project generation has failed\n(phase: [%s])\n(tool: [%s])", phase,
                    getTool().getFullName());
            throw new RunnerProjectGeneratorException(message, this, xe);
        }
    }

    /**
     * Validates both the snippet and runner project settings.
     *
     * @throws ConfigurationException
     *             if a SETTE configuration problem occurred
     */
    private void validate() throws ConfigurationException {
        Validate.isTrue(getSnippetProject().getState().equals(SnippetProject.State.PARSED),
                "The snippet project must be parsed (state: [%s]) ",
                getSnippetProject().getState().name());

        // TODO snippet project validation can fail even if it is valid
        // getSnippetProjectSettings().validateExists();
        getRunnerProjectSettings().validateNotExists();
    }

    /**
     * Prepares the writing of the runner project, i.e. make everything ready for writing out.
     */
    private void prepareRunnerProject() {
        // create the Eclipse project
        eclipseProject = new EclipseProject(getRunnerProjectSettings().getProjectName());

        // create the classpath for the project
        EclipseClasspathDescriptor cp = new EclipseClasspathDescriptor();
        eclipseProject.setClasspathDescriptor(cp);

        // add default JRE and bin directory
        cp.addEntry(EclipseClasspathEntry.JRE_CONTAINER);
        cp.addEntry(Kind.OUTPUT, "bin");

        // add snippet source directory
        cp.addEntry(Kind.SOURCE, getSnippetProjectSettings().getSnippetSourceDirectoryPath());

        // add libraries used by the snippet project
        for (File libraryFile : getSnippetProject().getFiles().getLibraryFiles()) {
            cp.addEntry(Kind.LIBRARY, String.format("%s/%s",
                    getSnippetProjectSettings().getLibraryDirectoryPath(), libraryFile.getName()));
        }
    }

    /**
     * Writes the runner project out.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     * @throws ParseException
     *             If the source code has parser errors.
     * @throws XmlException
     *             If an XML related exception occurs.
     */
    private void writeRunnerProject() throws IOException, XmlException, ParseException {
        // TODO revise whole method
        // TODO now using a newer JAPA (suuports java 8), -> maybe ANTLR supports better

        // create INFO file
        writeInfoFile();

        // copy snippets
        FileUtils.copyDirectory(getSnippetProjectSettings().getSnippetSourceDirectory(),
                getRunnerProjectSettings().getSnippetSourceDirectory(), false);

        // remove SETTE annotations and imports from file
        Collection<File> filesWritten = FileUtils
                .listFiles(getRunnerProjectSettings().getSnippetSourceDirectory(), null, true);

        for (File file : filesWritten) {
            // parse source with JavaParser
            CompilationUnit compilationUnit = JavaParser.parse(file);

            // extract type
            List<TypeDeclaration> types = ListUtils.emptyIfNull(compilationUnit.getTypes());
            if (types.size() != 1) {
                // NOTE better exception type
                throw new RuntimeException(
                        "Java source files containing more that one types are not supported");
            }

            TypeDeclaration type = types.get(0);

            // skip file if Java version is not supported by the tool (@SetteSnippetContainer)
            // NOTE it can be also done with snippet containers... (and also done in CATG
            // generator!)
            List<AnnotationExpr> classAnnotations = ListUtils.emptyIfNull(type.getAnnotations());
            JavaVersion reqJavaVer = getRequiredJavaVersion(classAnnotations);

            if (reqJavaVer != null && !getTool().supportsJavaVersion(reqJavaVer)) {
                System.err.println(
                        "Skipping file: " + file + " (required Java version: " + reqJavaVer + ")");
                FileUtils.forceDelete(file);
            } else {
                // remove SETTE annotations from the class
                Predicate<AnnotationExpr> isSetteAnnotation = (a -> a.getName().getName()
                        .startsWith("Sette"));
                classAnnotations.removeIf(isSetteAnnotation);

                // remove SETTE annotations from the members
                for (BodyDeclaration member : ListUtils.emptyIfNull(type.getMembers())) {
                    ListUtils.emptyIfNull(member.getAnnotations()).removeIf(isSetteAnnotation);
                }

                // remove SETTE imports
                ListUtils.emptyIfNull(compilationUnit.getImports()).removeIf(importDeclaration -> {
                    // TODO enhance
                    List<String> toRemovePrefixes = new ArrayList<>();
                    toRemovePrefixes.add("hu.bme.mit.sette.annotations");
                    toRemovePrefixes.add("hu.bme.mit.sette.snippets.inputs");
                    toRemovePrefixes.add("hu.bme.mit.sette.common.snippets.JavaVersion");

                    String impDecl = importDeclaration.getName().toString();
                    for (String prefix : toRemovePrefixes) {
                        if (impDecl.startsWith(prefix)) {
                            return true;
                        }
                    }
                    return false;
                });

                // save edited source code
                FileUtils.write(file, compilationUnit.toString());
            }
        }

        // copy libraries
        if (getSnippetProjectSettings().getLibraryDirectory().exists()) {
            FileUtils.copyDirectory(getSnippetProjectSettings().getLibraryDirectory(),
                    getRunnerProjectSettings().getSnippetLibraryDirectory(), false);
        }

        // create project
        this.eclipseProject.save(getRunnerProjectSettings().getBaseDirectory());
    }

    private void writeInfoFile() throws IOException {
        // TODO later maybe use an XML file!!!
        File infoFile = new File(getRunnerProjectSettings().getBaseDirectory(), "SETTE-INFO");

        StringBuilder infoFileData = new StringBuilder();
        infoFileData.append("Runner project name: " + getRunnerProjectSettings().getProjectName())
                .append('\n');
        infoFileData.append("Snippet project name: " + getSnippetProjectSettings().getProjectName())
                .append('\n');
        infoFileData.append("Tool name: " + getTool().getName()).append('\n');
        infoFileData.append("Tool version: " + getTool().getVersion()).append('\n');
        infoFileData.append("Tool supported Java version: " + getTool().getSupportedJavaVersion())
                .append('\n');

        // TODO externalise somewhere the date format string
        String generatedAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        infoFileData.append("Generated at: ").append(generatedAt).append('\n');

        FileUtils.write(infoFile, infoFileData);
    }

    private static JavaVersion getRequiredJavaVersion(List<AnnotationExpr> classAnnotations) {
        Optional<AnnotationExpr> containerAnnotation = classAnnotations.stream()
                .filter(a -> "SetteSnippetContainer".equals(a.getName().getName())).findAny();

        if (containerAnnotation.isPresent()) {
            List<Node> children = ListUtils
                    .emptyIfNull(containerAnnotation.get().getChildrenNodes());

            Optional<String> reqJavaVerStr = children.stream()
                    .filter(c -> c instanceof MemberValuePair).map(c -> (MemberValuePair) c)
                    .filter(mvp -> "requiredJavaVersion".equals(mvp.getName()))
                    .map(mvp -> mvp.getValue().toString()).findAny();

            if (reqJavaVerStr.isPresent()) {
                Optional<JavaVersion> reqJavaVer = Stream.of(JavaVersion.values())
                        .filter(jv -> reqJavaVerStr.get().endsWith(jv.name())).findAny();

                if (reqJavaVer.isPresent()) {
                    return reqJavaVer.get();
                } else {
                    // NOTE make better
                    throw new RuntimeException("Cannot recignize java version:" + reqJavaVerStr);
                }
            }
        }
        return null;
    }

    /**
     * This method is called after validation but before preparation.
     *
     * @throws ConfigurationException
     *             if a SETTE configuration problem occurred
     */
    protected void afterValidate() throws ConfigurationException {
        // to be implemented by the subclass
    }

    /**
     * This method is called after preparation but before writing.
     *
     * @param eclipseProject
     *            the Eclipse project
     */
    protected void afterPrepareRunnerProject(
            @SuppressWarnings("hiding") EclipseProject eclipseProject) {
        // to be implemented by the subclass if needed
    }

    /**
     * This method is called after writing.
     *
     * @param eclipseProject
     *            the Eclipse project
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     * @throws SetteException
     *             if a SETTE problem occurred
     */
    protected void afterWriteRunnerProject(
            @SuppressWarnings("hiding") EclipseProject eclipseProject)
                    throws IOException, SetteException {
        // to be implemented by the subclass if needed
    }

    /**
     * This method adds the generated directory to the classpath of the Eclipse project.
     */
    protected final void addGeneratedDirectoryToProject() {
        this.eclipseProject.getClasspathDescriptor().addEntry(Kind.SOURCE,
                RunnerProjectSettings.GENERATED_DIRNAME);
    }
}
