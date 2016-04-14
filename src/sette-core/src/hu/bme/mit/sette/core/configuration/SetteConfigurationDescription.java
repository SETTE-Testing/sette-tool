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

import static java.lang.String.format;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import hu.bme.mit.sette.core.validator.ValidationException;
import hu.bme.mit.sette.core.validator.Validator;
import lombok.Getter;
import lombok.NonNull;

/**
 * Class to parse the configuration from a JSON string or file. Please note that the role of this
 * class is to verify that the format of the JSON is correct and parse the data into objects. The
 * configuration is validated and finalised by the {@link SetteConfiguration} class.
 */
final class SetteConfigurationDescription {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String NODE_BASEDIRS = "baseDirs";
    private static final String NODE_OUTPUT_DIR = "outputDir";
    private static final String NODE_RUNNER_TIMEOUT_IN_MS = "runnerTimeoutInMs";
    private static final String NODE_SNIPPET_PROJECT_DIRS = "snippetProjectDirs";
    private static final String NODE_TOOLS = "tools";
    private static final String NODE_TOOL_CLASS_NAME = "className";
    private static final String NODE_TOOL_NAME = "name";
    private static final String NODE_TOOL_DIR = "toolDir";
    private static final ImmutableSet<String> TOP_FIELDS = ImmutableSet.of(NODE_BASEDIRS,
            NODE_OUTPUT_DIR, NODE_RUNNER_TIMEOUT_IN_MS, NODE_SNIPPET_PROJECT_DIRS,
            NODE_TOOLS);
    private static final ImmutableSet<String> TOOL_FIELDS = ImmutableSet.of(NODE_TOOL_CLASS_NAME,
            NODE_TOOL_NAME, NODE_TOOL_DIR);

    /** Validator used during parsing */
    private final Validator<String> validator = Validator.of(getClass().getSimpleName());

    /** List of possible base directory paths */
    @Getter
    private final ImmutableList<String> baseDirPaths;

    @Getter
    /** The output directory path, relative to the base directory */
    private final String outputDirPath;

    @Getter
    /** The timeout for the tool runner in ms */
    private final int runnerTimeoutInMs;

    @Getter
    /** List of available snippet project directory paths, relative to the base directory */
    private final ImmutableList<String> snippetProjectDirPaths;

    @Getter
    /** List of the tool configuration descriptions */
    private final ImmutableList<SetteToolConfigurationDescription> toolConfigurations;

    private SetteConfigurationDescription(String json) throws IOException, ValidationException {
        log.debug("Parsing configuration from JSON: {}", json);
        JsonNode rootNode = new ObjectMapper().readTree(json);

        validateObjectFieldNames(rootNode, TOP_FIELDS);

        baseDirPaths = parseStringArray(rootNode, NODE_BASEDIRS);
        outputDirPath = parseString(rootNode, NODE_OUTPUT_DIR);
        runnerTimeoutInMs = parseInt(rootNode, NODE_RUNNER_TIMEOUT_IN_MS);
        snippetProjectDirPaths = parseStringArray(rootNode, NODE_SNIPPET_PROJECT_DIRS);

        // parse tools
        JsonNode toolsNode = rootNode.get(NODE_TOOLS);
        List<SetteToolConfigurationDescription> tmpToolConfs = new ArrayList<>();

        if (toolsNode.isArray()) {
            for (JsonNode toolNode : toolsNode) {
                validateObjectFieldNames(toolNode, TOOL_FIELDS);

                SetteToolConfigurationDescription tc = new SetteToolConfigurationDescription(
                        parseString(toolNode, NODE_TOOL_CLASS_NAME),
                        parseString(toolNode, NODE_TOOL_NAME),
                        parseString(toolNode, NODE_TOOL_DIR));

                tmpToolConfs.add(tc);
            }
        } else {
            validator.addError(NODE_TOOLS + ": must be an array");
        }

        toolConfigurations = ImmutableList.copyOf(tmpToolConfs);

        // no spaces and control chars in minified JSON
        String minifiedJson = new ObjectMapper().writer().writeValueAsString(rootNode);
        final CharMatcher allowedChars = CharMatcher.inRange((char) 0x20, (char) 0x7E);
        if (!allowedChars.matchesAllOf(minifiedJson)) {
            String msg = "All the characters of the minified JSON must be in the 0x20-0x7E range"
                    + " (it means that non-ASCII characters are prohibted in the configuration)";
            validator.addError(msg);
        }

        validator.validate();

        log.debug("Parsed configuration: {}", this);
    }

    private void validateObjectFieldNames(JsonNode node, Set<String> expectedFieldNames)
            throws ValidationException {
        // call validate iff this method has added an error
        if (node.isObject()) {
            ImmutableSet<String> fieldNames = ImmutableSet.copyOf(node.fieldNames());
            if (!expectedFieldNames.equals(fieldNames)) {
                String msg = format("The JSON object does not have the required fields"
                        + " (expected: %s, actual: %s)", expectedFieldNames,
                        fieldNames);
                validator.addError(msg);
                validator.validate();
            }
        } else {
            validator.addError("The node JSON is not an object: " + node);
            validator.validate();
        }
    }

    private int parseInt(JsonNode parentNode, String fieldName) {
        JsonNode node = parentNode.get(fieldName);
        if (node.canConvertToInt()) {
            return node.asInt();
        } else {
            validator.addError(fieldName + ": must be an integer");
            return 0;
        }
    }

    private String parseString(JsonNode parentNode, String fieldName) {
        JsonNode node = parentNode.get(fieldName);
        if (node.isTextual()) {
            return node.asText();
        } else {
            validator.addError(fieldName + ": must be a string");
            return "";
        }
    }

    private ImmutableList<String> parseStringArray(JsonNode parentNode, String fieldName) {
        JsonNode node = parentNode.get(fieldName);
        if (node.isArray()) {
            List<String> ret = new ArrayList<>(node.size());
            for (JsonNode n : ImmutableList.copyOf(node.elements())) {
                if (n.isTextual()) {
                    ret.add(n.asText());
                } else {
                    validator.addError(fieldName + ": must contain only strings");
                    return ImmutableList.of();
                }
            }
            return ImmutableList.copyOf(ret);
        } else {
            validator.addError(fieldName + ": must be an array");
            return ImmutableList.of();
        }
    }

    /**
     * Parses the configuration from a JSON string.
     *
     * @param json
     *            the JSON string
     * @return the parsed JSON object
     * @throws SetteConfigurationException
     * @throws SetteConfigurationException
     *             if parsing fails or the configuration is invalid
     * @throws IOException
     *             if an I/O error occurs
     */
    public static SetteConfigurationDescription parse(@NonNull String json)
            throws SetteConfigurationException {
        try {
            return new SetteConfigurationDescription(json);
        } catch (JsonProcessingException | ValidationException ex) {
            throw new SetteConfigurationException(
                    "The JSON has an invalid format: " + ex.getMessage(), ex);
        } catch (IOException ex) {
            throw new SetteConfigurationException("An I/O error occurred: " + ex.getMessage(), ex);
        }
    }

    @Override
    public String toString() {
        return "SetteConfigurationDescription [baseDirPaths=" + baseDirPaths + ", outputDirPath="
                + outputDirPath + ", runnerTimeoutInMs=" + runnerTimeoutInMs
                + ", snippetProjectDirPaths=" + snippetProjectDirPaths + ", toolConfigurations="
                + toolConfigurations + "]";
    }
}
