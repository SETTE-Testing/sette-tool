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
package hu.bme.mit.sette.core.configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSortedSet;

import hu.bme.mit.sette.core.util.io.PathUtils;
import hu.bme.mit.sette.core.validator.PathValidator;
import hu.bme.mit.sette.core.validator.ValidationException;
import hu.bme.mit.sette.core.validator.Validator;
import lombok.Getter;
import lombok.NonNull;

/**
 * An instance of this class represents a parsed configuration for SETTE.
 */
public final class SetteConfiguration {
    private final Logger log = LoggerFactory.getLogger(getClass());

    /** The base directory */
    @Getter
    private final Path baseDir;

    /** The output directory. */
    @Getter
    private final Path outputDir;

    /** The runner timeout in milliseconds (always positive). */
    @Getter
    private final int runnerTimeoutInMs;

    /** The snippet project directories (read-only). */
    @Getter
    private final ImmutableSortedSet<Path> snippetProjectDirs;

    /** The tool configurations (read-only). */
    @Getter
    private final ImmutableSortedSet<SetteToolConfiguration> toolConfigurations;

    /**
     * Instantiates a new SETTE configuration.
     *
     * @param baseDir
     *            the base directory
     * @param outputDir
     *            the output directory
     * @param runnerTimeoutInMs
     *            the runner timeout in milliseconds (always-positive)
     * @param snippetProjectDirs
     *            the snippet project directories
     * @param toolConfigurations
     *            the tool configurations
     * @throws ValidationException
     *             if validation fails
     */
    public SetteConfiguration(@NonNull Path baseDir, @NonNull Path outputDir, int runnerTimeoutInMs,
            @NonNull Collection<Path> snippetProjectDirs,
            @NonNull Collection<SetteToolConfiguration> toolConfigurations)
                    throws ValidationException {
        this.baseDir = baseDir;
        this.outputDir = outputDir;
        this.runnerTimeoutInMs = runnerTimeoutInMs;
        this.snippetProjectDirs = ImmutableSortedSet.copyOf(snippetProjectDirs);
        this.toolConfigurations = ImmutableSortedSet.copyOf(toolConfigurations);

        validate();
    }

    /**
     * Instantiates a new SETTE configuration.
     *
     * @param configDesc
     *            the configuration descriptor
     * @throws ValidationException
     *             if validation fails
     */
    private SetteConfiguration(SetteConfigurationDescription configDesc)
            throws ValidationException {
        // Parse and validate configuration description
        log.debug("Validating configuration description: {}", configDesc);

        Validator<SetteConfigurationDescription> v = Validator.of(configDesc);

        // check: at least one base dir, one snippet project and one tool
        v.addErrorIfTrue("Please specify at least one base directory",
                configDesc.getBaseDirPaths().isEmpty());
        v.addErrorIfTrue("Please specify at least one snippet project directory",
                configDesc.getSnippetProjectDirPaths().isEmpty());
        v.addErrorIfTrue("Please specify at least one tool",
                configDesc.getToolConfigurations().isEmpty());
        v.validate();

        // baseDir: select the first existing
        baseDir = configDesc.getBaseDirPaths()
                .stream()
                .map(p -> Paths.get(p).toAbsolutePath())
                .filter(Files::exists)
                .findFirst().orElse(null);

        if (baseDir == null) {
            v.addError("None of the possible base directories exist");
            v.validate();
        }

        this.outputDir = baseDir.resolve(configDesc.getOutputDirPath());
        this.runnerTimeoutInMs = configDesc.getRunnerTimeoutInMs();

        Stream<Path> tmpSnippetProjectDirs = configDesc.getSnippetProjectDirPaths()
                .stream().map(baseDir::resolve);
        this.snippetProjectDirs = ImmutableSortedSet.copyOf(tmpSnippetProjectDirs.iterator());

        Stream<SetteToolConfiguration> tmpToolConfigurations = configDesc.getToolConfigurations()
                .stream()
                .map(t -> {
                    Path toolDir = baseDir.resolve(t.getToolDirPath());
                    return new SetteToolConfiguration(t.getClassName(), t.getName(), toolDir);
                });
        toolConfigurations = ImmutableSortedSet.copyOf(tmpToolConfigurations.iterator());

        // Validate configuration
        validate();
    }

    private void validate() throws ValidationException {
        log.debug("Validating configuration: {}", this);
        Validator<SetteConfiguration> v = Validator.of(this);

        // baseDir: non-null, exists, 07x7
        PathValidator.forDirectory(baseDir, true, null, true).addToIfInvalid(v);

        // outputDir: exists, 0777
        PathValidator.forDirectory(outputDir, true, true, true).addToIfInvalid(v);

        // runnerTimeoutInMs: must be positive
        v.addErrorIfFalse("The timeout for the runner must be positive", runnerTimeoutInMs > 0);

        // snippetProjectDirs: exists, 07x7
        for (Path dir : snippetProjectDirs) {
            PathValidator.forDirectory(dir, true, null, true).addToIfInvalid(v);
        }

        // toolsConfigrations: unique names are required, tools dirs exist, 07x7
        Set<String> toolNames = new HashSet<>();
        for (SetteToolConfiguration tc : toolConfigurations) {
            PathValidator.forDirectory(tc.getToolDir(), true, null, true).addToIfInvalid(v);
            toolNames.add(tc.getName().toLowerCase());
        }

        v.addErrorIfFalse("The tools must have a unique name (case-insensitive)",
                toolNames.size() == toolConfigurations.size());
        v.addErrorIfTrue("A tool name must not be blank", toolNames.contains(""));

        v.validate();
    }

    /**
     * Parses a SETTE configuration from a JSON string.
     * 
     * @param json
     *            the JSON string
     * @return the parsed SETTE configuration
     * @throws IOException
     *             if an I/O error occurs
     * @throws SetteConfigurationException
     *             if parsing fails or the configuration is invalid
     */
    public static SetteConfiguration parse(@NonNull String json)
            throws SetteConfigurationException {
        try {
            return new SetteConfiguration(SetteConfigurationDescription.parse(json));
        } catch (ValidationException ex) {
            throw new SetteConfigurationException(
                    "The configuration is invalid: " + ex.getMessage(), ex);
        }
    }

    /**
     * Parses a SETTE configuration from a JSON file.
     * 
     * @param json
     *            the JSON file
     * @return the parsed SETTE configuration
     * @throws IOException
     *             if an I/O error occurs
     * @throws SetteConfigurationException
     *             if parsing fails or the configuration is invalid
     */
    public static SetteConfiguration parse(@NonNull Path jsonFile)
            throws SetteConfigurationException {
        try {
            PathValidator.forRegularFile(jsonFile, true, null, null, "json").validate();
            String json = new String(PathUtils.readAllBytes(jsonFile));
            return parse(json);
        } catch (ValidationException ex) {
            throw new SetteConfigurationException("The file is invalid: " + jsonFile, ex);
        } catch (IOException ex) {
            throw new SetteConfigurationException("An I/O error occurred: " + ex.getMessage(), ex);
        }
    }

    @Override
    public String toString() {
        return "SetteConfiguration [baseDir=" + baseDir + ", outputDir=" + outputDir
                + ", runnerTimeoutInMs=" + runnerTimeoutInMs + ", snippetProjectDirs="
                + snippetProjectDirs + ", toolConfigurations=" + toolConfigurations + "]";
    }
}
