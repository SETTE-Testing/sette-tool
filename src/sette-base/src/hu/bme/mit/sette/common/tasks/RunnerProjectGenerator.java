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
package hu.bme.mit.sette.common.tasks;

import hu.bme.mit.sette.common.Tool;
import hu.bme.mit.sette.common.descriptors.eclipse.EclipseClasspathDescriptor;
import hu.bme.mit.sette.common.descriptors.eclipse.EclipseClasspathEntry;
import hu.bme.mit.sette.common.descriptors.eclipse.EclipseClasspathEntry.Kind;
import hu.bme.mit.sette.common.descriptors.eclipse.EclipseProject;
import hu.bme.mit.sette.common.exceptions.RunnerProjectGeneratorException;
import hu.bme.mit.sette.common.exceptions.SetteConfigurationException;
import hu.bme.mit.sette.common.exceptions.SetteException;
import hu.bme.mit.sette.common.model.runner.RunnerProjectSettings;
import hu.bme.mit.sette.common.model.snippet.SnippetProject;
import hu.bme.mit.sette.common.snippets.JavaVersion;
import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.ImportDeclaration;
import japa.parser.ast.body.BodyDeclaration;
import japa.parser.ast.body.TypeDeclaration;
import japa.parser.ast.expr.AnnotationExpr;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.Validate;

/**
 * A SETTE task which provides base for runner project generation. The phases
 * are the following: validation, preparation, writing.
 *
 * @param <T>
 *            the type of the tool
 */
public abstract class RunnerProjectGenerator<T extends Tool> extends
        SetteTask<T> {
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
     */
    public RunnerProjectGenerator(final SnippetProject snippetProject,
            final File outputDirectory, final T tool) {
        super(snippetProject, outputDirectory, tool);
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
            afterPrepareRunnerProject(this.eclipseProject);

            phase = "write runner project (do)";
            writeRunnerProject();
            phase = "write runner project (after)";
            afterWriteRunnerProject(this.eclipseProject);

            phase = "complete";
        } catch (Exception e) {
            String message = String.format(
                    "The runner project generation has failed\n"
                            + "(phase: [%s])\n(tool: [%s])", phase,
                    getTool().getFullName());
            throw new RunnerProjectGeneratorException(message, this, e);
        }
    }

    /**
     * Validates both the snippet and runner project settings.
     *
     * @throws SetteConfigurationException
     *             if a SETTE configuration problem occurred
     */
    private void validate() throws SetteConfigurationException {
        Validate.isTrue(
                getSnippetProject().getState().equals(
                        SnippetProject.State.PARSED),
                "The snippet project must be parsed (state: [%s]) ",
                getSnippetProject().getState().name());

        // TODO snippet proj. val. can fail even if it is valid
        // getSnippetProjectSettings().validateExists();
        getRunnerProjectSettings().validateNotExists();
    }

    /**
     * Prepares the writing of the runner project, i.e. make everything ready
     * for writing out.
     */
    private void prepareRunnerProject() {
        // create the Eclipse project
        this.eclipseProject = new EclipseProject(
                getRunnerProjectSettings().getProjectName());

        // create the classpath for the project
        EclipseClasspathDescriptor cp = new EclipseClasspathDescriptor();
        this.eclipseProject.setClasspathDescriptor(cp);

        // add default JRE and bin directory
        cp.addEntry(EclipseClasspathEntry.JRE_CONTAINER);
        cp.addEntry(Kind.OUTPUT, "bin");

        // add snippet source directory
        cp.addEntry(Kind.SOURCE, getSnippetProjectSettings()
                .getSnippetSourceDirectoryPath());

        // add libraries used by the snippet project
        for (File libraryFile : getSnippetProject().getFiles()
                .getLibraryFiles()) {
            cp.addEntry(Kind.LIBRARY,
                    getSnippetProjectSettings()
                            .getLibraryDirectoryPath()
                            + '/'
                            + libraryFile.getName());
        }
    }

    /**
     * Writes the runner project out.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     * @throws ParseException
     *             If the source code has parser errors.
     * @throws ParserConfigurationException
     *             If a DocumentBuilder cannot be created which satisfies the
     *             configuration requested or when it is not possible to create
     *             a Transformer instance.
     * @throws TransformerException
     *             If an unrecoverable error occurs during the course of the
     *             transformation.
     */
    private void writeRunnerProject() throws IOException,
            ParseException, ParserConfigurationException,
            TransformerException {
        // TODO revise whole method
        // TODO now using JAPA, which does not support Java 7/8 -> maybe ANTLR
        // supports
        // better

        // create INFO file
        // TODO later maybe use an XML file!!!

        File infoFile = new File(getRunnerProjectSettings()
                .getBaseDirectory(), "SETTE-INFO");

        StringBuilder infoFileData = new StringBuilder();
        infoFileData.append("Tool name: " + getTool().getName())
                .append('\n');
        infoFileData.append("Tool version: " + getTool().getVersion())
                .append('\n');
        infoFileData.append(
                "Tool supported Java version: "
                        + getTool().getSupportedJavaVersion()).append(
                '\n');

        // TODO externalise somewhere the date format string
        String generatedAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new Date());
        infoFileData.append("Generated at: ").append(generatedAt)
                .append('\n');

        String id = generatedAt + ' ' + getTool().getName() + " ("
                + getTool().getVersion() + ')';
        infoFileData.append("ID: ").append(id).append('\n');

        FileUtils.write(infoFile, infoFileData);

        // copy snippets
        FileUtils.copyDirectory(getSnippetProjectSettings()
                .getSnippetSourceDirectory(),
                getRunnerProjectSettings().getSnippetSourceDirectory(),
                false);

        // remove SETTE annotations and imports from file
        Collection<File> filesWritten = FileUtils.listFiles(
                getRunnerProjectSettings().getSnippetSourceDirectory(),
                null, true);

        mainLoop: for (File file : filesWritten) {
            CompilationUnit compilationUnit = JavaParser.parse(file);

            // remove SETTE annotations
            if (compilationUnit.getTypes() != null) {
                for (TypeDeclaration type : compilationUnit.getTypes()) {
                    if (type.getAnnotations() != null) {
                        for (Iterator<AnnotationExpr> iterator = type
                                .getAnnotations().iterator(); iterator
                                .hasNext();) {
                            AnnotationExpr annot = iterator.next();

                            String annotStr = annot.toString().trim();

                            // TODO enhance
                            // if container and has req version and it is java 7
                            // and tool does not support -> remove
                            if (annotStr
                                    .startsWith("@SetteSnippetContainer")
                                    && annotStr
                                            .contains("requiredJavaVersion")
                                    && annotStr
                                            .contains("JavaVersion.JAVA_7")
                                    && !getTool().supportsJavaVersion(
                                            JavaVersion.JAVA_7)) {
                                // TODO support java version JAVA_8
                                // TODO error handling
                                // remove file
                                System.err.println("Skipping file: "
                                        + file
                                        + " (required Java version: "
                                        + JavaVersion.JAVA_7 + ")");
                                FileUtils.forceDelete(file);
                                continue mainLoop;

                            }

                            // TODO enhance
                            if (annot.getName().toString()
                                    .startsWith("Sette")) {
                                iterator.remove();
                            }
                        }
                    }

                    if (type.getMembers() != null) {
                        for (BodyDeclaration member : type.getMembers()) {
                            if (member.getAnnotations() != null) {
                                for (Iterator<AnnotationExpr> iterator = member
                                        .getAnnotations().iterator(); iterator
                                        .hasNext();) {
                                    AnnotationExpr annotation = iterator
                                            .next();

                                    // TODO enhance
                                    if (annotation.getName().toString()
                                            .startsWith("Sette")) {
                                        iterator.remove();
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // remove SETTE imports
            if (compilationUnit.getImports() != null) {
                for (Iterator<ImportDeclaration> iterator = compilationUnit
                        .getImports().iterator(); iterator.hasNext();) {
                    ImportDeclaration importDeclaration = iterator
                            .next();

                    // TODO enhance
                    String p1 = "hu.bme.mit.sette.annotations";
                    String p2 = "hu.bme.mit.sette.snippets.inputs";
                    // TODO enhance
                    String p3 = "catg.CATG";
                    String p4 = "hu.bme.mit.sette.common.snippets.JavaVersion";

                    if (importDeclaration.getName().toString()
                            .equals(p3)) {
                        // keep CATG
                    } else if (importDeclaration.getName().toString()
                            .equals(p4)) {
                        iterator.remove();
                    } else if (importDeclaration.getName().toString()
                            .startsWith(p1)) {
                        iterator.remove();
                    } else if (importDeclaration.getName().toString()
                            .startsWith(p2)) {
                        iterator.remove();
                    }
                }
            }

            // save edited source code
            FileUtils.write(file, compilationUnit.toString());
        }

        // copy libraries
        if (getSnippetProjectSettings().getLibraryDirectory().exists()) {
            FileUtils.copyDirectory(getSnippetProjectSettings()
                    .getLibraryDirectory(), getRunnerProjectSettings()
                    .getSnippetLibraryDirectory(), false);
        }

        // create project
        this.eclipseProject.save(getRunnerProjectSettings()
                .getBaseDirectory());
    }

    /**
     * This method is called after validation but before preparation.
     *
     * @throws SetteConfigurationException
     *             if a SETTE configuration problem occurred
     */
    protected void afterValidate() throws SetteConfigurationException {
    }

    /**
     * This method is called after preparation but before writing.
     *
     * @param pEclipseProject
     *            the Eclipse project
     */
    protected void afterPrepareRunnerProject(
            final EclipseProject pEclipseProject) {
    }

    /**
     * This method is called after writing.
     *
     * @param pEclipseProject
     *            the Eclipse project
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     * @throws SetteException
     *             if a SETTE problem occurred
     */
    protected void afterWriteRunnerProject(
            final EclipseProject pEclipseProject) throws IOException,
            SetteException {
    }

    /**
     * This method adds the generated directory to the classpath of the Eclipse
     * project.
     */
    protected final void addGeneratedDirectoryToProject() {
        this.eclipseProject.getClasspathDescriptor().addEntry(
                Kind.SOURCE, RunnerProjectSettings.GENERATED_DIRNAME);
    }
}
