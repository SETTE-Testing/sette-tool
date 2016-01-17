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
package hu.bme.mit.sette.core.model.snippet;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSortedSet;

import hu.bme.mit.sette.common.annotations.SetteDependency;
import hu.bme.mit.sette.common.annotations.SetteSnippetContainer;
import hu.bme.mit.sette.core.validator.PathValidator;
import hu.bme.mit.sette.core.validator.ValidationContext;
import hu.bme.mit.sette.core.validator.ValidationException;
import hu.bme.mit.sette.core.validator.Validator;
import lombok.Getter;
import lombok.NonNull;

public final class SnippetProject {
    /** The base directory of the snippet project */
    @Getter
    private final Path baseDir;

    /** Set of snippet source files (unmodifiable) */
    @Getter
    private final ImmutableSortedSet<Path> snippetFiles;

    /** Set of snippet input source files (unmodifiable) */
    @Getter
    private final ImmutableSortedSet<Path> snippetInputFiles;

    /** Set of library files (unmodifiable) */
    @Getter
    private final ImmutableSortedSet<Path> libFiles;

    @Getter
    /** Class loader to load classes of the snippet projects */
    private final ClassLoader classLoader;

    @Getter
    /** Set of snippet containers (unmodifiable) */
    private final ImmutableSortedSet<SnippetContainer> snippetContainers;

    @Getter
    private final ImmutableSortedSet<SnippetDependency> snippetDependencies;

    /**
     * Parses a {@link SnippetProject} from the specified directory.
     * 
     * @param baseDir
     *            the base directory of the snippet project
     * @return the parsed {@link SnippetProject}
     * @throws ValidationException
     *             if validation of the project fails
     * @throws IOException
     *             if an I/O exception occurs
     */
    public static SnippetProject parse(@NonNull Path baseDir)
            throws ValidationException, IOException {
        return new SnippetProject(baseDir);
    }

    /**
     * Creates, parses and validates a snippet project.
     * 
     * @param baseDir
     *            the base directory of the snippet project
     * @throws ValidationException
     *             if validation of the project fails
     * @throws IOException
     *             if an I/O exception occurs
     */
    private SnippetProject(@NonNull Path baseDir) throws ValidationException, IOException {
        // parse and validate directory layout
        PathValidator.forDirectory(baseDir, true, null, true).validate();
        this.baseDir = baseDir.toRealPath();
        validateDirs();

        // collect and validate source & lib files
        ValidationContext vc = new ValidationContext(this);
        this.snippetFiles = collectAndValidateFiles(getSourceDir(), "java", vc);
        this.snippetInputFiles = collectAndValidateFiles(getInputSourceDir(), "java", vc);
        this.libFiles = collectAndValidateFiles(getLibDir(), "jar", vc);

        // load classes
        this.classLoader = createClassLoader();
        this.snippetContainers = loadSnippetContainers();
        this.snippetDependencies = loadSnippetDepenencies();

        // class is ready
    }

    /**
     * Collects and validates the files for the specified extension from a directory using the
     * specified {@link ValidationContext}.
     * 
     * @param dir
     *            the directory whose files needs to be collected
     * @param extension
     *            the file extension to validate (<code>null</code> means no requirement)
     * @param validationContext
     *            the {@link ValidationContext} to which errors will be added
     * @return set of collected files (note: it will contain all the files regardless if some of
     *         them does not have the specified extension)
     * @throws IOException
     *             if an I/O exception occurs
     */
    private static ImmutableSortedSet<Path> collectAndValidateFiles(@NonNull Path dir,
            String extension,
            @NonNull ValidationContext validationContext) throws IOException {
        if (Files.exists(dir)) {
            SortedSet<Path> files = Files.walk(dir).filter(Files::isRegularFile).sorted()
                    .collect(Collectors.toCollection(TreeSet<Path>::new));
            files.forEach(f -> {
                PathValidator pv = PathValidator.forRegularFile(f, true, null, null, extension);
                validationContext.addValidatorIfInvalid(pv);
            });
            return ImmutableSortedSet.copyOf(files);
        } else {
            return ImmutableSortedSet.of();
        }
    }

    /**
     * Validates the directories of the snippet project.
     * 
     * @throws ValidationException
     *             if directory validation fails
     * @throws IOException
     *             if an I/O error had occurred
     */
    private void validateDirs() throws ValidationException, IOException {
        ValidationContext vc = new ValidationContext(this);

        vc.addValidator(PathValidator.forDirectory(getSourceDir(), true, null, true));

        if (Files.exists(getInputSourceDir())) {
            vc.addValidator(PathValidator.forDirectory(getInputSourceDir(), true, null, true));
        }

        if (Files.exists(getLibDir())) {
            vc.addValidator(PathValidator.forDirectory(getLibDir(), true, null, true));
        }

        PathValidator v = PathValidator.forDirectory(getBuildDir(), true, null, true);
        v.validate();
        
        if (Files.walk(getBuildDir()).filter(Files::isRegularFile).findAny().isPresent()) {
            v.addError("The build directory does not contain any regular file "
                    + "(probably the project is not built)");
        }
        vc.addValidator(v);

        vc.validate();
    }

    /**
     * Creates a class loader for the snippet project.
     * 
     * @return An {@link URLClassLoader} which is able to load the classes of the snippet project.
     */
    private URLClassLoader createClassLoader() {
        try {
            List<URL> urls = new ArrayList<>();
            urls.add(getBuildDir().toUri().toURL());
            for (Path libFile : libFiles) {
                urls.add(libFile.toUri().toURL());
            }

            return new URLClassLoader(urls.toArray(new URL[urls.size()]));
        } catch (MalformedURLException ex) {
            throw new RuntimeException("At least one directory/file cannot be converted to an URL",
                    ex);
        }
    }

    private ImmutableSortedSet<SnippetContainer> loadSnippetContainers()
            throws ValidationException {
        Path sourceDir = getSourceDir();

        // collect snippet container classes
        Set<Class<?>> snippetContainerClasses = new TreeSet<>();
        Validator<SnippetProject> v = new Validator<>(this);

        for (Path sourceFile : snippetFiles) {
            if (!sourceFile.toString().startsWith(sourceDir.toString())) {
                throw new RuntimeException(
                        String.format("The file %s should be in directory %s at this point",
                                sourceFile, sourceDir));
            }

            String relPath = sourceDir.relativize(sourceFile).toString();
            String className = relPath.replaceAll("(\\\\|/)", ".").replaceAll("\\.java$", "");

            try {
                Class<?> javaClass = classLoader.loadClass(className);
                if (javaClass.getAnnotation(SetteSnippetContainer.class) != null) {
                    snippetContainerClasses.add(javaClass);
                }
            } catch (ClassNotFoundException ex) {
                v.addError(String.format("ClassNotFoundException when loading class %s: %s",
                        className, ex.getMessage()));
            }
        }

        v.validate();

        // create snippet container objects
        SortedSet<SnippetContainer> sc = new TreeSet<>();

        for (Class<?> javaClass : snippetContainerClasses) {
            try {
                sc.add(new SnippetContainer(this, javaClass));
            } catch (ValidationException ex) {
                v.addError(ex.getMessage());
            }
        }
        v.validate();

        return ImmutableSortedSet.copyOf(sc);
    }

    private ImmutableSortedSet<SnippetDependency> loadSnippetDepenencies()
            throws ValidationException {
        Path sourceDir = getSourceDir();

        // collect snippet container classes
        Set<Class<?>> snippetDepClasses = new TreeSet<>();
        Validator<SnippetProject> v = new Validator<>(this);

        for (Path sourceFile : snippetFiles) {
            if (!sourceFile.toString().startsWith(sourceDir.toString())) {
                throw new RuntimeException(
                        String.format("The file %s should be in directory %s at this point",
                                sourceFile, sourceDir));
            }

            String relPath = sourceDir.relativize(sourceFile).toString();
            String className = relPath.replaceAll("(\\\\|/)", ".").replaceAll("\\.java$", "");

            try {
                Class<?> javaClass = classLoader.loadClass(className);
                if (javaClass.getAnnotation(SetteDependency.class) != null) {
                    snippetDepClasses.add(javaClass);
                }
            } catch (ClassNotFoundException ex) {
                v.addError(String.format("ClassNotFoundException when loading class %s: %s",
                        className, ex.getMessage()));
            }
        }

        v.validate();

        // create snippet container objects
        SortedSet<SnippetDependency> sd = new TreeSet<>();

        for (Class<?> javaClass : snippetDepClasses) {
            try {
                sd.add(new SnippetDependency(this, javaClass));
            } catch (ValidationException ex) {
                v.addError(ex.getMessage());
            }
        }
        v.validate();

        return ImmutableSortedSet.copyOf(sd);
    }

    /**
     * @return the name of the snippet project
     */
    public String getName() {
        return baseDir.getFileName().toString();
    }

    /**
     * @return the snippet source directory
     */
    public Path getSourceDir() {
        return baseDir.resolve("snippet-src");
    }

    /**
     * @return the snippet input source directory
     */
    public Path getInputSourceDir() {
        return baseDir.resolve("snippet-input-src");
    }

    /**
     * @return the snippet library directory
     */
    public Path getLibDir() {
        return baseDir.resolve("snippet-lib");
    }

    /**
     * @return the snippet build directory
     */
    public Path getBuildDir() {
        return baseDir.resolve("build");
    }

    @Override
    public String toString() {
        return String.format("SnippetProject [name=%s, directory=%s]", getName(), baseDir);
    }
}
