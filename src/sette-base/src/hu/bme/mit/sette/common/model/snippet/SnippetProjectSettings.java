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
package hu.bme.mit.sette.common.model.snippet;

import hu.bme.mit.sette.common.validator.FileType;
import hu.bme.mit.sette.common.validator.FileValidator;
import hu.bme.mit.sette.common.validator.exceptions.ValidatorException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

/**
 * Stores settings for a snippet code project. This object can be only created
 * if the snippet project configuration is valid (i.e. all directories specified
 * in the sette-snippets.properties file exists and they are readable).
 */
public final class SnippetProjectSettings {
    /** The name of the snippet project configuration file. */
    public static final String CONFIGURATION_FILE_NAME = "sette-snippets.properties";

    /** Name of the directory containing the snippet source files. */
    // @Deprecated
    // public static final String SOURCE_DIRNAME = "snippet-src";
    /** Name of the directory containing the input source files. */
    // @Deprecated
    // public static final String INPUT_DIRNAME = "snippet-inputs";
    /** Name of the directory containing the libraries. */
    // @Deprecated
    // public static final String LIBRARY_DIRNAME = "snippet-libs";
    /** The base directory. */
    private final File baseDirectory;

    /** The name of the directory containing the snippet source files. */
    private final String snippetSourceDirectoryPath;

    /** The relative path to the directory containing the snippet binary files. */
    private final String snippetBinaryDirectoryPath;

    /** The relative path to the directory containing the input source files. */
    private final String inputSourceDirectoryPath;

    /** The relative path to the directory containing the input binary files. */
    private final String inputBinaryDirectoryPath;

    /**
     * The relative path to the directory containing libraries used by the
     * snippets.
     */
    private final String libraryDirectoryPath;

    /** The directory containing the snippet source files. */
    private final File snippetSourceDirectory;

    /** The directory containing the snippet binary files. */
    private final File snippetBinaryDirectory;

    /** The directory containing the input source files. */
    private final File inputSourceDirectory;

    /** The directory containing the input binary files. */
    private final File inputBinaryDirectory;

    /** The directory containing libraries used by the snippets. */
    private final File libraryDirectory;

    /**
     * Creates an instance of the object.
     *
     * @param pBaseDirectory
     *            The base directory.
     * @throws ValidatorException
     *             if validation has failed
     */
    public SnippetProjectSettings(final File pBaseDirectory)
            throws ValidatorException {
        Validate.notNull(pBaseDirectory,
                "The base directory must not be null");
        baseDirectory = pBaseDirectory;

        // validate base directory and stop if already error
        new FileValidator(baseDirectory).type(FileType.DIRECTORY)
        .readable(true).executable(true).validate();

        // parse and validate configuration file
        File configurationFile = new File(baseDirectory,
                CONFIGURATION_FILE_NAME);
        FileValidator vc = new FileValidator(configurationFile);
        vc.type(FileType.REGULAR_FILE).readable(true);
        vc.validate(); // stop if already error

        Properties configuration = new Properties();

        FileInputStream is = null;
        try {
            is = new FileInputStream(configurationFile);
            configuration.load(is);
        } catch (IOException | SecurityException
                | IllegalArgumentException e) {
            IOUtils.closeQuietly(is);
            vc.addException(
                    "Cannot read file contents as Java properties", e);
            vc.validate();// stop if already error
        }

        // read properties
        snippetSourceDirectoryPath = StringUtils
                .trimToNull(configuration.getProperty("snippet-src"));
        snippetBinaryDirectoryPath = StringUtils
                .trimToNull(configuration.getProperty("snippet-bin"));
        inputSourceDirectoryPath = StringUtils.trimToNull(configuration
                .getProperty("input-src"));
        inputBinaryDirectoryPath = StringUtils.trimToNull(configuration
                .getProperty("input-bin"));
        libraryDirectoryPath = StringUtils.trimToNull(configuration
                .getProperty("snippet-libs"));

        // validate properties and save File objects
        if (snippetSourceDirectoryPath == null) {
            vc.addException("The \"snippet-src\" property must exist and must not be blank");
            // set only to enable further validation regardless this error
            snippetSourceDirectory = null;
        } else {
            snippetSourceDirectory = new File(baseDirectory,
                    snippetSourceDirectoryPath);
        }

        if (snippetBinaryDirectoryPath == null) {
            vc.addException("The \"snippet-bin\" property must exist and must not be blank");
            // set only to enable further validation regardless this error
            snippetBinaryDirectory = null;
        } else {
            snippetBinaryDirectory = new File(baseDirectory,
                    snippetBinaryDirectoryPath);
        }

        if (inputSourceDirectoryPath == null
                && inputBinaryDirectoryPath == null) {
            // no inputs
            inputSourceDirectory = null;
            inputBinaryDirectory = null;
        } else if (inputSourceDirectoryPath != null
                && inputBinaryDirectoryPath != null) {
            // has inputs
            inputSourceDirectory = new File(baseDirectory,
                    inputSourceDirectoryPath);
            inputBinaryDirectory = new File(baseDirectory,
                    inputBinaryDirectoryPath);
        } else {
            // inconsistent
            vc.addException("Both the \"input-bin\" and \"input-src\" properties must exist and must not be blank or both must be skipped");
            // set only to enable further validation regardless this error
            inputSourceDirectory = null;
            inputBinaryDirectory = null;
        }

        if (libraryDirectoryPath == null) {
            libraryDirectory = null;
        } else {
            libraryDirectory = new File(baseDirectory,
                    libraryDirectoryPath);
        }

        // validate that dirs exists and that they are readable
        File[] directories = new File[] { snippetSourceDirectory,
                snippetBinaryDirectory, inputSourceDirectory,
                inputBinaryDirectory, libraryDirectory };

        for (File dir : directories) {
            if (dir != null) {
                vc.addChildIfInvalid(new FileValidator(dir)
                .type(FileType.DIRECTORY).readable(true)
                .executable(true));
            }
        }

        // final validation check
        vc.validate();
    }

    /**
     * Returns the name of the snippet project.
     *
     * @return The name of the snippet project.
     */
    public String getProjectName() {
        return baseDirectory.getName();
    }

    /**
     * Gets the base directory.
     *
     * @return the base directory
     */
    public File getBaseDirectory() {
        return baseDirectory;
    }

    /**
     * Gets the name of the directory containing the snippet source files.
     *
     * @return the name of the directory containing the snippet source files
     */
    public String getSnippetSourceDirectoryPath() {
        return snippetSourceDirectoryPath;
    }

    /**
     * Gets the relative path to the directory containing the snippet binary
     * files.
     *
     * @return the relative path to the directory containing the snippet binary
     *         files
     */
    public String getSnippetBinaryDirectoryPath() {
        return snippetBinaryDirectoryPath;
    }

    /**
     * Gets the relative path to the directory containing the input source
     * files.
     *
     * @return the relative path to the directory containing the input source
     *         files
     */
    public String getInputSourceDirectoryPath() {
        return inputSourceDirectoryPath;
    }

    /**
     * Gets the relative path to the directory containing the input binary
     * files.
     *
     * @return the relative path to the directory containing the input binary
     *         files
     */
    public String getInputBinaryDirectoryPath() {
        return inputBinaryDirectoryPath;
    }

    /**
     * Gets the relative path to the directory containing libraries used by the
     * snippets.
     *
     * @return the relative path to the directory containing libraries used by
     *         the snippets
     */
    public String getLibraryDirectoryPath() {
        return libraryDirectoryPath;
    }

    /**
     * Gets the directory containing the snippet source files.
     *
     * @return the directory containing the snippet source files
     */
    public File getSnippetSourceDirectory() {
        return snippetSourceDirectory;
    }

    /**
     * Gets the directory containing the snippet binary files.
     *
     * @return the directory containing the snippet binary files
     */
    public File getSnippetBinaryDirectory() {
        return snippetBinaryDirectory;
    }

    /**
     * Gets the directory containing the input source files.
     *
     * @return the directory containing the input source files
     */
    public File getInputSourceDirectory() {
        return inputSourceDirectory;
    }

    /**
     * Gets the directory containing the input binary files.
     *
     * @return the directory containing the input binary files
     */
    public File getInputBinaryDirectory() {
        return inputBinaryDirectory;
    }

    /**
     * Gets the directory containing libraries used by the snippets.
     *
     * @return the directory containing libraries used by the snippets
     */
    public File getLibraryDirectory() {
        return libraryDirectory;
    }
}
