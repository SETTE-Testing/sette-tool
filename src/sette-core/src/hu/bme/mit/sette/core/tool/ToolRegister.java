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
package hu.bme.mit.sette.core.tool;

import java.util.SortedMap;
import java.util.TreeMap;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedMap;

import lombok.NonNull;

/**
 * Static class for storing different {@link Tool}s.
 */
// TODO moving to a solution where SetteConfiguration contains the tools
@Deprecated
public final class ToolRegister {
    /** The set of tools. */
    private static final SortedMap<String, Tool> tools = new TreeMap<>();

    /** Static class. */
    private ToolRegister() {
        throw new UnsupportedOperationException("Static class");
    }

    /**
     * Gets the {@link Tool} object by its name.
     *
     * @param name
     *            the name of the tool
     * @return the {@link Tool} object or <code>null</code> if not found
     */
    public synchronized static Tool get(@NonNull String name) {
        return tools.get(name);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Tool> T get(@NonNull Class<T> toolClass) {
        return (T) tools.values().stream().filter(t -> t.getClass() == toolClass).findAny()
                .orElseGet(null);
    }

    /**
     * Adds a tool to the register.
     *
     * @param tool
     *            the tool
     */
    synchronized static void register(@NonNull Tool tool) {
        Preconditions.checkArgument(!tools.containsKey(tool.getName()),
                "The register already contains a tool with this name: %s", tool.getName());

        tools.put(tool.getName(), tool);
    }

    /**
     * Returns an immutable mapping of tool names and tool objects.
     *
     * @return an immutable mapping of tool names and tool objects
     */
    public synchronized static ImmutableSortedMap<String, Tool> toMap() {
        return ImmutableSortedMap.copyOf(tools);
    }
}
