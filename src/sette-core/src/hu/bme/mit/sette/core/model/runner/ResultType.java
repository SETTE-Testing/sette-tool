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
// NOTE revise this file
package hu.bme.mit.sette.core.model.runner;

import java.util.Arrays;

import lombok.NonNull;

/**
 * Represents the result type of one run of input generation.
 */
public enum ResultType {
    /** Input generation was not able to be started. */
    NA("N/A"),
    /** Input generation failed with an exception. */
    EX("EX"),
    /** Input generation exceeded the timeout or consumed all the memory. */
    TM("T/M"),
    /** Input generation was successful. */
    S("S"),
    /** Input generation was successful but did not produce maximum coverage. */
    NC("NC"),
    /** Input generation was successful and produced maximum coverage. */
    C("C");

    /** The string representation. */
    private final String toString;

    /**
     * Instantiates a new result type.
     *
     * @param toString
     *            the string representation
     */
    private ResultType(String toString) {
        this.toString = toString;
    }

    /**
     * Parses the given string into a {@link ResultType}.
     *
     * @param string
     *            the string
     * @return the result type
     */
    public static ResultType fromString(@NonNull String string) {
        for (ResultType rt : ResultType.values()) {
            if (rt.toString.equalsIgnoreCase(string) || rt.name().equalsIgnoreCase(string)) {
                return rt;
            }
        }

        String message = String.format("Invalid string (string: [%s], validStrings: [%s]", string,
                Arrays.toString(ResultType.values()));
        throw new IllegalArgumentException(message);
    }

    @Override
    public String toString() {
        return toString;
    }
}
