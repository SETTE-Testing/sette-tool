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

import lombok.Getter;
import lombok.NonNull;

/**
 * Class for parsed JSON tool configuration description.
 */
public final class SetteToolConfigurationDescription {
    /** The class name of the tool, e.g, <code>com.example.MyTool</code> */
    @Getter
    private final String className;

    /** The user-specified name of the tool. */
    @Getter
    private final String name;

    /** The tool directory path, relative to the base directory */
    @Getter
    private final String toolDirPath;

    /**
     * Instantiates a new SETTE tool configuration description.
     *
     * @param className
     *            the class name of the tool, e.g, <code>com.example.MyTool</code>
     * @param name
     *            the user-specified name of the tool
     * @param toolDirPath
     *            the tool directory path, relative to the base directory
     */
    SetteToolConfigurationDescription(@NonNull String className, @NonNull String name,
            @NonNull String toolDirPath) {
        this.className = className;
        this.name = name.trim();
        this.toolDirPath = toolDirPath;
    }

    @Override
    public String toString() {
        return "SetteToolConfigurationDescription [className=" + className + ", name=" + name
                + ", toolDirPath=" + toolDirPath + "]";
    }
}
