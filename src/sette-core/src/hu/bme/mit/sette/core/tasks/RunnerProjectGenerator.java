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
package hu.bme.mit.sette.core.tasks;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;

import hu.bme.mit.sette.common.snippets.JavaVersion;
import hu.bme.mit.sette.core.SetteException;
import hu.bme.mit.sette.core.configuration.SetteConfigurationException;
import hu.bme.mit.sette.core.descriptors.eclipse.EclipseClasspathDescriptor;
import hu.bme.mit.sette.core.descriptors.eclipse.EclipseClasspathEntry;
import hu.bme.mit.sette.core.descriptors.eclipse.EclipseClasspathEntryKind;
import hu.bme.mit.sette.core.descriptors.eclipse.EclipseProject;
import hu.bme.mit.sette.core.descriptors.eclipse.EclipseProjectDescriptor;
import hu.bme.mit.sette.core.exceptions.RunnerProjectGeneratorException;
import hu.bme.mit.sette.core.exceptions.XmlException;
import hu.bme.mit.sette.core.model.runner.RunnerProjectSettings;
import hu.bme.mit.sette.core.model.snippet.SnippetProject;
import hu.bme.mit.sette.core.tool.Tool;
import hu.bme.mit.sette.core.util.io.PathUtils;

/**
 * A SETTE task which provides base for runner project generation. The phases are the following:
 * validation, preparation, writing.
 *
 * @param <T>
 *            the type of the tool
 */
public abstract class RunnerProjectGenerator<T extends Tool> extends EvaluationTask<T> {
    /** The Eclipse project. */
    private EclipseProject eclipseProject;

    /**
     * Instantiates a new runner project generator.
     *
     * @param snippetProject
     *            the snippet project
     * @param outputDir
     *            the output directory
     * @param tool
     *            the tool
     * @param runnerProjectTag
     *            the tag of the runner project
     */
    public RunnerProjectGenerator(SnippetProject snippetProject, Path outputDir, T tool,
            String runnerProjectTag) {
        super(snippetProject, outputDir, tool, runnerProjectTag);
    }

    /**
     * Generates the runner project.
     *
     * @throws RunnerProjectGeneratorException
     *             if generation fails
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
        } catch (Exception ex) {
            String message = String.format(
                    "The runner project generation has failed (phase: [%s], tool: [%s])", phase,
                    getTool().getName());
            throw new RunnerProjectGeneratorException(message, ex);
        }
    }

    /**
     * Validates both the snippet and runner project settings.
     *
     * @throws SetteConfigurationException
     *             if a SETTE configuration problem occurred
     */
    private void validate() throws SetteConfigurationException {
        // TODO snippet project validation can fail even if it is valid
        // getSnippetProjectSettings().validateExists();
        getRunnerProjectSettings().validateNotExists();
    }

    /**
     * Prepares the writing of the runner project, i.e. make everything ready for writing out.
     */
    private void prepareRunnerProject() {
        // create the classpath for the project
        EclipseClasspathDescriptor cp = new EclipseClasspathDescriptor();

        // add default JRE and bin directory
        cp.addEntry(EclipseClasspathEntry.JRE_CONTAINER);
        cp.addEntry(EclipseClasspathEntryKind.OUTPUT, "bin");

        // add snippet source directory
        // FIXME relativize paths
        cp.addEntry(EclipseClasspathEntryKind.SOURCE, getSnippetProject().getBaseDir()
                .relativize(getSnippetProject().getSourceDir()).toString());

        // add libraries used by the snippet project
        for (Path libraryFile : getSnippetProject().getJavaLibFiles()) {
            cp.addEntry(EclipseClasspathEntryKind.LIBRARY,
                    getSnippetProject().getBaseDir().relativize(libraryFile).toString());
        }

        // create the Eclipse project
        String projectName = getRunnerProjectSettings().getProjectName();
        eclipseProject = new EclipseProject(new EclipseProjectDescriptor(projectName, null), cp);
    }

    /**
     * Writes the runner project out.
     *
     * @throws ParseException
     *             If the source code has parser errors.
     * @throws XmlException
     *             If an XML related exception occurs.
     */
    private void writeRunnerProject() throws XmlException, ParseException {
        // TODO revise whole method
        // TODO now using a newer JAPA (suuports java 8), -> maybe ANTLR supports better

        PathUtils.createDir(getRunnerProjectSettings().getBaseDir().toPath());

        // copy snippets
        PathUtils.copy(getSnippetProject().getSourceDir(),
                getRunnerProjectSettings().getSnippetSourceDirectory().toPath());

        // create INFO file
        writeInfoFile();

        // remove SETTE annotations and imports from file
        Collection<File> filesWritten = PathUtils
                .walk(getRunnerProjectSettings().getSnippetSourceDirectory().toPath())
                .filter(Files::isRegularFile).map(Path::toFile).sorted()
                .collect(Collectors.toList());

        for (File file : filesWritten) {
            // parse source with JavaParser
            log.debug("Parsing with JavaParser: {}", file);
            CompilationUnit compilationUnit;
            try {
                compilationUnit = JavaParser.parse(file);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
            log.debug("Parsed with JavaParser: {}", file);

            // extract type
            List<TypeDeclaration> types = compilationUnit.getTypes();
            if (types.size() != 1) {
                // NOTE better exception type
                throw new RuntimeException(
                        "Java source files containing more that one types are not supported");
            }

            TypeDeclaration type = types.get(0);

            // skip file if Java version is not supported by the tool (@SetteSnippetContainer)
            // NOTE it can be also done with snippet containers... (and also done in CATG
            // generator!)
            List<AnnotationExpr> classAnnotations = type.getAnnotations();
            JavaVersion reqJavaVer = getRequiredJavaVersion(classAnnotations);

            if (reqJavaVer != null && !getTool().supportsJavaVersion(reqJavaVer)) {
                System.err.println(
                        "Skipping file: " + file + " (required Java version: " + reqJavaVer + ")");
                PathUtils.delete(file.toPath());
            } else {
                // remove SETTE annotations from the class
                Predicate<AnnotationExpr> isSetteAnnotation = (a -> a.getName().getName()
                        .startsWith("Sette"));
                classAnnotations.removeIf(isSetteAnnotation);

                // remove SETTE annotations from the members
                for (BodyDeclaration member : type.getMembers()) {
                    member.getAnnotations().removeIf(isSetteAnnotation);
                }

                // TODO enhance
                List<String> toRemovePrefixes = new ArrayList<>();
                toRemovePrefixes.add("hu.bme.mit.sette.snippets.inputs");
                toRemovePrefixes.add("hu.bme.mit.sette.common");

                // remove SETTE imports
                compilationUnit.getImports().removeIf(importDeclaration -> {

                    String impDecl = importDeclaration.getName().toString();
                    for (String prefix : toRemovePrefixes) {
                        if (impDecl.startsWith(prefix)) {
                            return true;
                        }
                    }
                    return false;
                });

                // save edited source code
                String source = compilationUnit.toString();
                if (type instanceof EnumDeclaration) {
                    // FIXME remove after javaparser bug is fixed
                    source = source.replaceFirst(type.getName() + "\\s+implements\\s*\\{",
                            type.getName() + " {");
                }
                PathUtils.write(file.toPath(), source.getBytes());
            }
        }

        // copy libraries
        if (getSnippetProject().getLibDir().toFile().exists()) {
            PathUtils.copy(getSnippetProject().getLibDir(),
                    getRunnerProjectSettings().getSnippetLibraryDirectory().toPath());
        }

        // create project
        this.eclipseProject.save(getRunnerProjectSettings().getBaseDir().toPath());
    }

    private void writeInfoFile() {
        // TODO later maybe use an XML file!!!
        File infoFile = new File(getRunnerProjectSettings().getBaseDir(), "SETTE-INFO");

        StringBuilder infoFileData = new StringBuilder();
        infoFileData.append("Runner project name: " + getSnippetProject().getName())
                .append('\n');
        infoFileData.append("Snippet project name: " + getSnippetProject().getName())
                .append('\n');
        infoFileData.append("Tool name: " + getTool().getName()).append('\n');
        infoFileData.append("Tool version: " + getTool().getVersion()).append('\n');
        infoFileData.append("Tool supported Java version: " + getTool().getSupportedJavaVersion())
                .append('\n');

        // TODO externalise somewhere the date format string
        String generatedAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        infoFileData.append("Generated at: ").append(generatedAt).append('\n');

        PathUtils.write(infoFile.toPath(), infoFileData.toString().getBytes());
    }

    private static JavaVersion getRequiredJavaVersion(List<AnnotationExpr> classAnnotations) {
        Optional<AnnotationExpr> containerAnnotation = classAnnotations.stream()
                .filter(a -> "SetteSnippetContainer".equals(a.getName().getName())).findAny();

        if (containerAnnotation.isPresent()) {
            List<Node> children = containerAnnotation.get().getChildrenNodes();

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
     * @throws SetteConfigurationException
     *             if a SETTE configuration problem occurred
     */
    protected void afterValidate() throws SetteConfigurationException {
        // to be implemented by the subclass
    }

    /**
     * This method is called after preparation but before writing.
     *
     * @param anEclipseProject
     *            the Eclipse project
     */
    protected void afterPrepareRunnerProject(EclipseProject anEclipseProject) {
        // to be implemented by the subclass if needed
    }

    /**
     * This method is called after writing.
     *
     * @param anEclipseProject
     *            the Eclipse project
     * @throws SetteException
     *             if a SETTE problem occurred
     */
    protected void afterWriteRunnerProject(EclipseProject anEclipseProject) throws SetteException {
        // to be implemented by the subclass if needed
    }

    /**
     * This method adds the generated directory to the classpath of the Eclipse project.
     */
    protected final void addGeneratedDirToProject() {
        this.eclipseProject.getClasspathDescriptor().addEntry(EclipseClasspathEntryKind.SOURCE,
                RunnerProjectSettings.GENERATED_DIRNAME);
    }
}
