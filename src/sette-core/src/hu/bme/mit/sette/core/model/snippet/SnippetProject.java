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

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.lang.reflect.Field;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;

import hu.bme.mit.sette.common.annotations.SetteDependency;
import hu.bme.mit.sette.common.annotations.SetteSnippetContainer;
import hu.bme.mit.sette.core.util.io.PathUtils;
import hu.bme.mit.sette.core.util.reflection.ClassComparator;
import hu.bme.mit.sette.core.validator.PathValidator;
import hu.bme.mit.sette.core.validator.ValidationException;
import hu.bme.mit.sette.core.validator.Validator;
import lombok.Getter;
import lombok.NonNull;

public final class SnippetProject implements Comparable<SnippetProject> {
    /** The base directory of the snippet project */
    @Getter
    private final Path baseDir;

    /** Set of snippet source files */
    @Getter
    private final ImmutableSortedSet<Path> snippetFiles;

    /** Set of snippet input source files */
    @Getter
    private final ImmutableSortedSet<Path> snippetInputFiles;

    /** Set of Java library files */
    @Getter
    private final ImmutableSortedSet<Path> javaLibFiles;

    @Getter
    /** Class loader to load classes of the snippet projects */
    private final ClassLoader classLoader;

    @Getter
    /** Set of snippet containers */
    private final ImmutableList<SnippetContainer> snippetContainers;

    @Getter
    private final ImmutableList<SnippetDependency> snippetDependencies;

    /**
     * Parses a {@link SnippetProject} from the specified directory.
     * 
     * @param baseDir
     *            the base directory of the snippet project
     * @return the parsed {@link SnippetProject}
     * @throws ValidationException
     *             if validation of the project fails
     */
    public static SnippetProject parse(@NonNull Path baseDir) throws ValidationException {
        return new SnippetProject(baseDir);
    }

    /**
     * Creates, parses and validates a snippet project.
     * 
     * @param baseDir
     *            the base directory of the snippet project
     * @throws ValidationException
     *             if validation of the project fails
     */
    private SnippetProject(@NonNull Path baseDir) throws ValidationException {
        // parse and validate directory layout
        PathValidator.forDirectory(baseDir, true, null, true).validate();
        this.baseDir = PathUtils.toRealPath(baseDir);
        validateDirs();

        // collect and validate source & lib files
        Validator<SnippetProject> v = Validator.of(this);
        this.snippetFiles = collectAndValidateFiles(getSourceDir(), "java", v);
        this.snippetInputFiles = collectAndValidateFiles(getInputSourceDir(), "java", v);

        ImmutableSortedSet<Path> libFiles = collectAndValidateFiles(getLibDir(), "jar|dll|so",
                v);
        this.javaLibFiles = ImmutableSortedSet.copyOf(
                libFiles.stream().filter(p -> p.toString().endsWith(".jar")).iterator());
        if (libFiles.size() != javaLibFiles.size()) {
            addLibDirToNativeLibPath(getLibDir());
        }

        v.validate();

        // load classes
        this.classLoader = createClassLoader();
        this.snippetContainers = loadSnippetContainers();
        this.snippetDependencies = loadSnippetDepenencies();

        // check that snippet id is unique
        List<String> snippetIds = snippetContainers.stream()
                .map(sc -> sc.getSnippets())
                .flatMap(s -> s.values().stream())
                .map(s -> s.getId())
                .collect(toList());
        Set<String> uniqueSnippetIds = new TreeSet<>(snippetIds);

        if (snippetIds.size() != uniqueSnippetIds.size()) {
            // remove all removes ALL the occurrences
            for (String id : uniqueSnippetIds) {
                snippetIds.remove(id);
            }

            SortedSet<String> duplicates = new TreeSet<>(snippetIds);

            v.addError("Duplicate snippet IDs: " + duplicates);
        }

        v.validate();

        // class is ready
    }

    private static void addLibDirToNativeLibPath(Path libDir) {
        // FIXME hack if -Djava.library.path is not specified properly
        // based on http://stackoverflow.com/a/6408467
        String libDirPath = libDir.toAbsolutePath().toString();
        try {
            Field field = ClassLoader.class.getDeclaredField("usr_paths");
            field.setAccessible(true);
            ArrayList<String> paths = Lists.newArrayList((String[]) field.get(null));
            if (!paths.contains(libDirPath)) {
                paths.add(libDirPath);
                field.set(null, paths.toArray(new String[0]));

                String propKey = "java.library.path";
                String oldProp = System.getProperty(propKey);
                String newProp = oldProp + File.pathSeparator + libDirPath;
                System.setProperty(propKey, newProp);
            }
            field.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Collects and validates the files for the specified extension from a directory using the
     * specified {@link Validator}.
     * 
     * @param dir
     *            the directory whose files needs to be collected
     * @param extension
     *            the file extension to validate (<code>null</code> means no requirement)
     * @param validator
     *            the {@link Validator} to which errors will be added
     * @return set of collected files (note: it will contain all the files regardless if some of
     *         them does not have the specified extension)
     */
    private static ImmutableSortedSet<Path> collectAndValidateFiles(@NonNull Path dir,
            String extension, @NonNull Validator<?> validator) {
        if (PathUtils.exists(dir)) {
            SortedSet<Path> files = PathUtils.walk(dir).filter(Files::isRegularFile).sorted()
                    .collect(Collectors.toCollection(TreeSet<Path>::new));
            files.forEach(f -> {
                PathValidator pv = PathValidator.forRegularFile(f, true, null, null, extension);
                validator.addChild(pv);
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
     */
    private void validateDirs() throws ValidationException {
        Validator<String> v = Validator.of("SnippetProject directories: " + baseDir);

        v.addChild(PathValidator.forDirectory(getSourceDir(), true, null, true));

        if (PathUtils.exists(getInputSourceDir())) {
            v.addChild(PathValidator.forDirectory(getInputSourceDir(), true, null, true));
        }

        if (PathUtils.exists(getLibDir())) {
            v.addChild(PathValidator.forDirectory(getLibDir(), true, null, true));
        }

        v.addChild(PathValidator.forDirectory(getBuildDir(), true, null, true));
        v.validate();

        // if (PathUtils.walk(getBuildDir()).filter(Files::isRegularFile).findAny().isPresent()) {
        // v.addError("The build directory does not contain any regular file "
        // + "(probably the project is not built)");
        // }

        v.validate();
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
            for (Path libFile : javaLibFiles) {
                urls.add(libFile.toUri().toURL());
            }

            return new URLClassLoader(urls.toArray(new URL[urls.size()]));
        } catch (MalformedURLException ex) {
            throw new RuntimeException("At least one directory/file cannot be converted to an URL",
                    ex);
        }
    }

    private ImmutableList<SnippetContainer> loadSnippetContainers()
            throws ValidationException {
        Path sourceDir = getSourceDir();

        // collect snippet container classes
        Set<Class<?>> snippetContainerClasses = new TreeSet<>(ClassComparator.INSTANCE);
        Validator<SnippetProject> v = Validator.of(this);

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
        List<SnippetContainer> sc = new ArrayList<>();

        for (Class<?> javaClass : snippetContainerClasses) {
            try {
                sc.add(new SnippetContainer(this, javaClass));
            } catch (ValidationException ex) {
                v.addChild(ex.getValidator());
            }
        }
        v.validate();

        return ImmutableList.copyOf(sc);
    }

    private ImmutableList<SnippetDependency> loadSnippetDepenencies()
            throws ValidationException {
        Path sourceDir = getSourceDir();

        // collect snippet container classes
        Set<Class<?>> snippetDepClasses = new TreeSet<>(ClassComparator.INSTANCE);
        Validator<SnippetProject> v = Validator.of(this);

        for (Path sourceFile : snippetFiles) {
            if (!sourceFile.toString().startsWith(sourceDir.toString())) {
                throw new RuntimeException(
                        String.format("The file %s should be in directory %s at this point",
                                sourceFile, sourceDir));
            }

            String relPath = sourceDir.relativize(sourceFile).toString();
            String className = relPath.replaceAll("(\\\\|/)", ".").replaceAll("\\.java$", "");

            try {
                Class<?> javaClass = Class.forName(className, true, classLoader);
                // Class<?> javaClass = classLoader.loadClass(className);
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
        List<SnippetDependency> sd = new ArrayList<>();

        for (Class<?> javaClass : snippetDepClasses) {
            try {
                sd.add(new SnippetDependency(this, javaClass));
            } catch (ValidationException ex) {
                v.addChild(ex.getValidator());
            }
        }
        v.validate();

        return ImmutableList.copyOf(sd);
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

    /**
     * @return a list of snippets
     */
    public Iterable<Snippet> getSnippets() {
        return getSnippets(null);
    }

    /**
     * @param snippetSelector
     *            filter for snippets ids (<code>null</code> will return all the snippets)
     * @return a filtered list of snippets
     */
    public Iterable<Snippet> getSnippets(Pattern snippetSelector) {
        Stream<Snippet> snippetStream = snippetContainers.stream()
                .flatMap(sc -> sc.getSnippets().values().stream());

        if (snippetSelector != null) {
            snippetStream = snippetStream.filter(s -> snippetSelector.matcher(s.getId()).matches());
        }

        return ImmutableList.copyOf(snippetStream.iterator());
    }

    @Override
    public int compareTo(@NonNull SnippetProject o) { // NOSONAR: default equals() and hashCode()
        return getName().compareToIgnoreCase(o.getName());
    }

    @Override
    public String toString() {
        return String.format("SnippetProject [name=%s, directory=%s]", getName(), baseDir);
    }
}
