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
package hu.bme.mit.sette.common;

import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.Validate;

/**
 * Static class for storing different {@link Tool}s.
 */
public final class ToolRegister {
    /** The set of tools. */
    private static SortedSet<Tool> tools;

    static {
        ToolRegister.tools = new TreeSet<>();
    }

    /** Static class. */
    private ToolRegister() {
        throw new UnsupportedOperationException("Static class");
    }

    /**
     * Gets the {@link Tool} object by its Java class.
     *
     * @param <T>
     *            the type of the tool
     * @param javaClass
     *            the Java class of the tool
     * @return the t
     */
    public static <T extends Tool> T get(final Class<T> javaClass) {
        Validate.notNull(javaClass, "The Java class must not be null");

        for (Tool tool : ToolRegister.tools) {
            if (tool.getClass().equals(javaClass)) {
                @SuppressWarnings("unchecked")
                T t = (T) tool;
                return t;
            }
        }

        return null;
    }

    /**
     * Adds a tool to the register.
     *
     * @param tool
     *            the tool
     */
    static void register(final Tool tool) {
        Validate.notNull(tool, "The tool must not be null");

        Tool another = ToolRegister.get(tool.getClass());

        if (another != null) {
            Validate.isTrue(
                    false,
                    "Another tool with this type or name "
                            + "is already present "
                            + "(javaClass: [%s], name: [%s], "
                            + "another.javaClass: [%s], another.name: [%s])",
                            tool.getClass().getName(), tool.getName(), another
                            .getClass().getName(), another.getName());
        }

        ToolRegister.tools.add(tool);
    }

    /**
     * Gets an array of tools.
     *
     * @return the array of tools
     */
    public static Tool[] toArray() {
        return ToolRegister.tools.toArray(new Tool[ToolRegister.tools
                                                   .size()]);
    }
}
