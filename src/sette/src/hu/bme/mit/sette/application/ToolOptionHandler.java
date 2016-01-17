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
package hu.bme.mit.sette.application;

import static com.google.common.base.Preconditions.checkState;

import java.util.Optional;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.Messages;
import org.kohsuke.args4j.spi.OneArgumentOptionHandler;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Setter;

import hu.bme.mit.sette.core.configuration.SetteConfiguration;
import hu.bme.mit.sette.core.configuration.SetteToolConfiguration;
import lombok.NonNull;

/**
 * args4j {@link OptionHandler} for the tool parameter of {@link ArgumentParser}. This class is
 * needed because tool name is an enum-like structure, but the list of tools are determined by the
 * configuration at runtime and not at compile-time.
 */
public class ToolOptionHandler extends OneArgumentOptionHandler<SetteToolConfiguration> {
    /**
     * The {@link SetteConfiguration} to be used to determine tool names when parsing arguments.
     * Never should be <code>null</code> when parsing arguments.
     * <p>
     * Note: this is required to be static because the class instantiated by args4j
     * {@link CmdLineParser} dynamically and the class will have no real connection to
     * {@link ArgumentParser} (args4j parses this class as a bean and does not saves a reference to
     * the instance).
     */
    public static SetteConfiguration configuration = null;

    private static void checkConfiguration() {
        checkState(configuration != null, "The SETTE configuration is not set for %s",
                ToolOptionHandler.class.getName());
    }

    /**
     * Creates an instance of the option handler. This constructor is called by args4j via
     * reflection, thus the parameters and the visibility of the class and the constructor shall
     * never change.
     * 
     * @param parser
     *            the {@link CmdLineParser} instance which the handler will belong to
     * @param option
     *            the option which the handler shall parse
     * @param setter
     *            the setter which the handler shall call if it has parsed the option value
     */
    public ToolOptionHandler(@NonNull CmdLineParser parser, @NonNull OptionDef option,
            @NonNull Setter<? super SetteToolConfiguration> setter) {
        super(parser, option, setter);
        checkConfiguration();

    }

    @Override
    protected SetteToolConfiguration parse(@NonNull String toolName)
            throws NumberFormatException, CmdLineException {
        Optional<SetteToolConfiguration> toolConfig = configuration.getToolConfigurations()
                .stream()
                .filter(tc -> tc.getName().equalsIgnoreCase(toolName))
                .findAny();

        if (toolConfig.isPresent()) {
            return toolConfig.get();
        } else {
            throw new CmdLineException(owner, Messages.ILLEGAL_OPERAND, option.toString(),
                    toolName);
        }
    }

    @Override
    public String getDefaultMetaVariable() {
        // returns available tool names, e.g., "[ Tool1 | Tool2 | ... ]"
        checkConfiguration();

        StringBuffer rv = new StringBuffer();
        rv.append("[");
        for (SetteToolConfiguration tc : configuration.getToolConfigurations()) {
            rv.append(tc.getName()).append(" | ");
        }
        rv.delete(rv.length() - 3, rv.length());
        rv.append("]");
        return rv.toString();
    }
}
