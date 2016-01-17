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

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSortedSet;

import hu.bme.mit.sette.core.validator.PathValidator;
import hu.bme.mit.sette.core.validator.ValidationException;
import hu.bme.mit.sette.core.validator.Validator;
import lombok.Getter;
import lombok.NonNull;

/**
 * An instance of this class represents a parsed configuration for SETTE.
 */
public final class SetteConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(SetteConfiguration.class);

    /** The base directory */
    @Getter
    private final Path baseDir;

    /** The output directory, relative to the base directory. */
    @Getter
    private final Path outputDir;

    /** The runner timeout in milliseconds (always positive). */
    @Getter
    private final int runnerTimeoutInMs;

    /** Set of snippet project directories, relative to the base directory (read-only). */
    @Getter
    private final ImmutableSortedSet<Path> snippetProjectDirs;

    /** Tool name and location pairs, relative to the base directory (read-only). */
    @Getter
    private final ImmutableSortedSet<SetteToolConfiguration> toolConfigurations;

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
        LOG.debug("Validating configuration: {}", configDesc);

        Validator<SetteConfigurationDescription> v = new Validator<>(configDesc);

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
                .map(p -> Paths.get(resolveTildeInPath(p)).toAbsolutePath())
                .filter(Files::exists)
                .findFirst().orElse(null);

        if (baseDir == null) {
            v.addError("None of the possible base directories exist");
        } else {
            PathValidator.forDirectory(baseDir, true, null, true).validate();
        }

        // outputDir: try to create if does not exists
        outputDir = baseDir.resolve(configDesc.getOutputDirPath());
        if (!Files.exists(outputDir)) {
            try {
                Files.createDirectories(outputDir);
                LOG.debug("Output directory has been created: {}", outputDir);
            } catch (IOException ex) {
                LOG.debug("Output directory creation has failed: " + outputDir, ex);
                v.addError("The output directory cannot be created: " + ex.getMessage());
            }
        } else {
            PathValidator.forDirectory(outputDir, true, true, true);
        }

        // runnerTimeoutInMs: must be positive
        this.runnerTimeoutInMs = configDesc.getRunnerTimeoutInMs();
        v.addErrorIfFalse("The timeout for the runner must be positive", runnerTimeoutInMs > 0);

        // snippetProjects: check if all exists
        Stream<Path> tmpSnippetProjectDirs = configDesc.getSnippetProjectDirPaths()
                .stream()
                .map(p -> baseDir.resolve(p))
                .peek(p -> {
                    v.addErrorIfFalse("The snippet project directory does not exists: " + p,
                            Files.exists(p));
                });
        snippetProjectDirs = ImmutableSortedSet.copyOf(tmpSnippetProjectDirs.iterator());

        // toolConfigurations: tool dirs must exist and tool names must be non-empty and unique
        Stream<SetteToolConfiguration> tmpToolConfigurations = configDesc.getToolConfigurations()
                .stream()
                .map(t -> {
                    Path toolDir = baseDir.resolve(t.getToolDirPath());
                    v.addErrorIfFalse("The tool directory does not exists: " + toolDir,
                            Files.exists(toolDir));
                    return new SetteToolConfiguration(t.getClassName(), t.getName(), toolDir);
                });
        toolConfigurations = ImmutableSortedSet.copyOf(tmpToolConfigurations.iterator());

        List<String> toolNames = toolConfigurations
                .stream()
                .map(tc -> tc.getName().toLowerCase())
                .distinct()
                .collect(toList());
        v.addErrorIfFalse("The tools must have a unique name (case-insensitive)",
                toolNames.size() == toolConfigurations.size());
        v.addErrorIfTrue("The tool names must not be blank", toolNames.contains(""));

        v.validate();

        LOG.debug("Validated configuration: {}", this);
    }

    private static String resolveTildeInPath(String path) {
        // resolve tilde to user home
        if (path.startsWith("~")) {
            if (path.equals("~") || path.charAt(1) == '/' || path.charAt(1) == '\\') {
                // "~" or "~/something"
                String resolvedPath = path.replaceFirst("^~", System.getProperty("user.home"));
                LOG.debug("Path with tilde (~) '{}' was resolved to '{}'", path, resolvedPath);
                return resolvedPath;
            } else {
                // "~something", not handled (Linux: syscall to echo or ls -d, Windows: hard)
                LOG.warn("Path with tilde (~) resolution is not supported for '{}' (only current "
                        + "user's home is resolved, otherwise do not use tilde)", path);
                return path;
            }
        } else {
            return path;
        }
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
            throws SetteConfigurationException, IOException {
        try {
            return new SetteConfiguration(SetteConfigurationDescription.parse(json));
        } catch (ValidationException ex) {
            throw new SetteConfigurationException(
                    "The configuration is invalid: " + ex.getMessage());
        }
    }

    @Override
    public String toString() {
        return "SetteConfiguration [baseDir=" + baseDir + ", outputDir=" + outputDir
                + ", runnerTimeoutInMs=" + runnerTimeoutInMs + ", snippetProjectDirs="
                + snippetProjectDirs + ", toolConfigurations=" + toolConfigurations + "]";
    }
}
