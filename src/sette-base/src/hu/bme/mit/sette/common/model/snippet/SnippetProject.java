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
// TODO z revise this file
package hu.bme.mit.sette.common.model.snippet;

import hu.bme.mit.sette.annotations.SetteSnippetContainer;
import hu.bme.mit.sette.common.descriptors.java.JavaSourceFile;
import hu.bme.mit.sette.common.exceptions.ConfigurationException;
import hu.bme.mit.sette.common.util.JavaFileUtils;
import hu.bme.mit.sette.common.util.reflection.ReflectionUtils;
import hu.bme.mit.sette.common.validator.FileType;
import hu.bme.mit.sette.common.validator.FileValidator;
import hu.bme.mit.sette.common.validator.GeneralValidator;
import hu.bme.mit.sette.common.validator.exceptions.ValidatorException;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.Validate;

/**
 * Represents a snippet code project.
 */
public final class SnippetProject {
    /**
     * Represents the state of a {@link SnippetProject} object.
     */
    public static enum State {
        /** The project object has created. */
        CREATED,
        /** The parsing has been started. */
        PARSE_STARTED,
        /** The parsing has been finished. */
        PARSED,
        /** An error occurred during parsing. */
        PARSE_ERROR
    }

    /** The state of the {@link SnippetProject} object. */
    private State state;

    /** The settings for the snippet project. */
    private final SnippetProjectSettings settings;

    /** The files of the snippet project. */
    private Files files;

    /** The Java source files of the snippet project. */
    private JavaSourceFiles javaSourceFiles;

    /** The model of the snippet project. */
    private Model model;

    /**
     * The class loader for loading snippet project classes. The class loader has all the specified
     * binary directories and JAR libraries on its path.
     */
    private ClassLoader classLoader = null;

    /**
     * Instantiates a new snippet project.
     *
     * @param settings
     *            the settings
     */
    public SnippetProject(SnippetProjectSettings settings) {
        Validate.notNull(settings, "The settings must not be null");
        this.state = State.CREATED;
        this.settings = settings;
    }

    /**
     * Parses the snippet project. The procedure is the following:
     *
     * <ol>
     * <li>Collect the snippet project files (sources, optional inputs and libraries, see
     * {@link #getFiles()}).</li>
     * <li>Creates a class loader for loading snippet project classes (see {@link #getClassLoader()}
     * )</li>
     * <li>Parse the Java source files (see {@link #getJavaSourceFiles()}).</li>
     * <li>Parse and build the model of the snippet project (see {@link #getModel()}). Also performs
     * classloading.</li>
     * </ol>
     *
     * @throws ConfigurationException
     *             if there was a configurational error
     * @throws ValidatorException
     *             if validation has failed
     */
    public void parse() throws ConfigurationException, ValidatorException {
        // validate preconditions
        validateState(State.CREATED);
        // TODO validation???
        // settings.validateExists();

        // start parsing
        state = State.PARSE_STARTED;

        // create containers
        files = new Files();
        javaSourceFiles = new JavaSourceFiles();
        model = new Model();

        // parse
        collectFiles(); // may throw ValidatorException
        createClassLoader(); // may throw SetteConfigurationException
        parseJavaSourceFiles(); // may throw SetteConfigurationException
        parseModel(); // may throw ValidatorException

        // successful
        state = State.PARSED;
    }

    /**
     * Collects the snippet project files (sources, optional inputs and libraries).
     *
     * @throws ValidatorException
     *             if validation has failed
     */
    private void collectFiles() throws ValidatorException {
        GeneralValidator validator = new GeneralValidator(this);

        // source files
        files.snippetSourceFiles
                .addAll(FileUtils.listFiles(settings.getSnippetSourceDirectory(), null, true));

        for (File file : files.snippetSourceFiles) {
            FileValidator v = new FileValidator(file);
            v.type(FileType.REGULAR_FILE).readable(true);
            v.extension(JavaFileUtils.JAVA_SOURCE_EXTENSION);
            validator.addChildIfInvalid(v);
        }

        // input files
        files.inputSourceFiles
                .addAll(FileUtils.listFiles(settings.getInputSourceDirectory(), null, true));

        for (File file : files.inputSourceFiles) {
            FileValidator v = new FileValidator(file);
            v.type(FileType.REGULAR_FILE).readable(true);
            v.extension(JavaFileUtils.JAVA_SOURCE_EXTENSION);
            validator.addChildIfInvalid(v);
        }

        // library files
        if (settings.getLibraryDirectory().exists()) {
            files.libraryFiles
                    .addAll(FileUtils.listFiles(settings.getLibraryDirectory(), null, true));

            for (File file : files.libraryFiles) {
                FileValidator v = new FileValidator(file);
                v.type(FileType.REGULAR_FILE).readable(true);
                v.extension(JavaFileUtils.JAVA_JAR_EXTENSION);
                validator.addChildIfInvalid(v);
            }
        }

        validator.validate();
    }

    /**
     * Creates a class loader for loading snippet project classes. The class loader will have all
     * the specified binary directories and JAR libraries on its path.
     *
     * @throws ConfigurationException
     *             if there was a configurational error
     */
    private void createClassLoader() throws ConfigurationException {
        try {
            // collect all bytecode resources
            List<URL> urls = new ArrayList<>();
            urls.add(settings.getSnippetBinaryDirectory().toURI().toURL());
            urls.add(settings.getInputBinaryDirectory().toURI().toURL());

            if (files != null && files.libraryFiles != null) {
                for (File libraryFile : files.libraryFiles) {
                    urls.add(libraryFile.toURI().toURL());
                }
            }

            // instantiate class loader
            classLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]));
        } catch (MalformedURLException e) {
            throw new ConfigurationException(
                    "At least one directory/file cannot be converted to an URL", e);
        }
    }

    /**
     * Parses the Java source files.
     *
     * @throws ConfigurationException
     *             if there was a configurational error
     * @throws IOException
     */
    private void parseJavaSourceFiles() throws ConfigurationException {
        // source Java files
        for (File file : files.snippetSourceFiles) {
            JavaSourceFile jsf = JavaSourceFile.fromFile(settings.getSnippetSourceDirectory(), file,
                    classLoader);
            javaSourceFiles.snippetSources.put(jsf.getJavaClass(), jsf);
        }

        // input Java files
        for (File file : files.inputSourceFiles) {
            JavaSourceFile jsf = JavaSourceFile.fromFile(settings.getInputSourceDirectory(), file,
                    classLoader);
            javaSourceFiles.inputSources.put(jsf.getJavaClass(), jsf);
        }
    }

    /**
     * Parses and builds the model of the snippet project. Also performs classloading.
     *
     * @throws ValidatorException
     *             if validation has failed
     */
    private void parseModel() throws ValidatorException {
        GeneralValidator validator = new GeneralValidator(this);

        for (JavaSourceFile jsf : javaSourceFiles.snippetSources.values()) {
            Class<?> javaClass = jsf.getJavaClass();

            try {
                if (javaClass.getAnnotation(SetteSnippetContainer.class) != null) {
                    // create and add snippet container object
                    SnippetContainer sc = new SnippetContainer(javaClass, classLoader);
                    model.containers.add(sc);

                    // add input factory container
                    if (sc.getInputFactoryContainer() != null) {
                        model.inputFactoryContainers.add(sc.getInputFactoryContainer());
                    }
                } else {
                    // create and add dependency object
                    SnippetDependency dep = new SnippetDependency(javaClass, classLoader);
                    model.dependencies.add(dep);
                }
            } catch (ValidatorException e) {
                validator.addChild(e.getValidator());
            }
        }

        validator.validate();
    }

    /**
     * Gets the state of the {@link SnippetProject} object.
     *
     * @return the state of the {@link SnippetProject} object
     */
    public State getState() {
        return state;
    }

    /**
     * Gets the settings for the snippet project.
     *
     * @return the settings for the snippet project
     */
    public SnippetProjectSettings getSettings() {
        return settings;
    }

    /**
     * Gets the files (sources, optional inputs and libraries) of the snippet project.
     *
     * @return the files (sources, optional inputs and libraries) of the snippet project
     */
    public Files getFiles() {
        validateState(State.PARSED);
        return files;
    }

    /**
     * Gets the Java source files of the snippet project.
     *
     * @return the Java source files of the snippet project
     */
    public JavaSourceFiles getJavaSourceFiles() {
        validateState(State.PARSED);
        return javaSourceFiles;
    }

    /**
     * Gets the model of the snippet project.
     *
     * @return the model of the snippet project
     */
    public Model getModel() {
        validateState(State.PARSED);
        return model;
    }

    /**
     * Gets the class loader for loading snippet project classes. The class loader has all the
     * specified binary directories and JAR libraries on its path.
     *
     * @return the class loader for loading snippet project classes
     */
    public ClassLoader getClassLoader() {
        Validate.validState(classLoader != null,
                "Invalid state: the class loader has not benn created yet");
        return classLoader;
    }

    /**
     * Validates the state.
     *
     * @param required
     *            the required state
     */
    private void validateState(State required) {
        Validate.validState(state.equals(required), "Invalid state (state: [%s], required: [%s])",
                state, required);
    }

    /**
     * Container class for {@link File}s in the snippet project.
     */
    public final class Files {
        /** The snippet files (Java source files). */
        private final SortedSet<File> snippetSourceFiles;

        /** The snippet input files (Java source files). */
        private final SortedSet<File> inputSourceFiles;

        /** The referenced libraries (JAR files). */
        private final SortedSet<File> libraryFiles;

        /**
         * Instantiates a new object.
         */
        public Files() {
            snippetSourceFiles = new TreeSet<>();
            inputSourceFiles = new TreeSet<>();
            libraryFiles = new TreeSet<>();
        }

        /**
         * Gets the snippet files (Java source files).
         *
         * @return the snippet files (Java source files)
         */
        public SortedSet<File> getSnippetSourceFiles() {
            return Collections.unmodifiableSortedSet(snippetSourceFiles);
        }

        /**
         * Gets the snippet input files (Java source files).
         *
         * @return the snippet input files (Java source files)
         */
        public SortedSet<File> getInputSourceFiles() {
            return Collections.unmodifiableSortedSet(inputSourceFiles);
        }

        /**
         * Gets the referenced libraries (JAR files).
         *
         * @return the referenced libraries (JAR files)
         */
        public SortedSet<File> getLibraryFiles() {
            return Collections.unmodifiableSortedSet(libraryFiles);
        }
    }

    /**
     * Container class for {@link JavaSourceFile}s in the snippet project.
     */
    public final class JavaSourceFiles {
        /** The snippet sources. */
        private final SortedMap<Class<?>, JavaSourceFile> snippetSources;

        /** The snippet input sources. */
        private final SortedMap<Class<?>, JavaSourceFile> inputSources;

        /**
         * Instantiates a new object.
         */
        private JavaSourceFiles() {
            snippetSources = new TreeMap<>(ReflectionUtils.CLASS_COMPARATOR);
            inputSources = new TreeMap<>(ReflectionUtils.CLASS_COMPARATOR);
        }

        /**
         * Gets the snippet sources.
         *
         * @return the snippet sources
         */
        public SortedMap<Class<?>, JavaSourceFile> getSnippetSources() {
            return Collections.unmodifiableSortedMap(snippetSources);
        }

        /**
         * Gets the snippet input sources.
         *
         * @return the snippet input sources
         */
        public SortedMap<Class<?>, JavaSourceFile> getInputSources() {
            return Collections.unmodifiableSortedMap(inputSources);
        }

    }

    /**
     * Container class for the whole snippet model.
     */
    public final class Model {
        /** The snippet containers. */
        private final SortedSet<SnippetContainer> containers;

        /** The snippet dependencies. */
        private final SortedSet<SnippetDependency> dependencies;

        /** The input factory containers. */
        private final SortedSet<SnippetInputFactoryContainer> inputFactoryContainers;

        /**
         * Instantiates a new model.
         */
        public Model() {
            containers = new TreeSet<>();
            dependencies = new TreeSet<>();
            inputFactoryContainers = new TreeSet<>();
        }

        /**
         * Gets the snippet containers.
         *
         * @return the snippet containers
         */
        public SortedSet<SnippetContainer> getContainers() {
            return Collections.unmodifiableSortedSet(containers);
        }

        /**
         * Gets the snippet dependencies.
         *
         * @return the snippet dependencies
         */
        public SortedSet<SnippetDependency> getDependencies() {
            return Collections.unmodifiableSortedSet(dependencies);
        }

        /**
         * Gets the input factory containers.
         *
         * @return the input factory containers
         */
        public SortedSet<SnippetInputFactoryContainer> getInputFactoryContainers() {
            return Collections.unmodifiableSortedSet(inputFactoryContainers);
        }
    }
}
