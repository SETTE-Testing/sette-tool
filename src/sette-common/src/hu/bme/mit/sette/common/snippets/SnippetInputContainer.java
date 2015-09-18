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

package hu.bme.mit.sette.common.snippets;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to define several input pairs/tuples for code snippets and stores them as
 * {@link SnippetInput} objects. Please note that this class does not perform any type checking
 * among the parameters, thus type mismatch can cause a runtime failure.
 * <p>
 * It is useful when a developer wishes to give sample inputs how to reach the desired code
 * coverage. Since a Java method signature has a fix number of parameters, it is checked runtime
 * whether the parameter count properties of all the underlying {@link SnippetInput} objects match
 * the parameter count property of this class.
 * <p>
 * For usage examples see {@link SnippetInputContainerTest}.
 */
public final class SnippetInputContainer {
    /** The number of required parameters for the code snippet. */
    private final int parameterCount;
    /** The list of inputs for the code snippet. */
    private final List<SnippetInput> inputs = new ArrayList<SnippetInput>();

    /**
     * Creates an instance with the specified parameter count. Throws an
     * {@link IllegalArgumentException} if the parameter count is negative.
     *
     * @param parameterCount
     *            The number of required parameters for the code snippet.
     */
    public SnippetInputContainer(int parameterCount) {
        if (parameterCount < 0) {
            throw new IllegalArgumentException("The parameter count must not be a negative number");
        }
        this.parameterCount = parameterCount;
    }

    /**
     * Returns the number of parameters.
     *
     * @return The number of parameters.
     */
    public int getParameterCount() {
        return parameterCount;
    }

    /**
     * Returns the number of input tuples.
     *
     * @return The number of input tuples.
     */
    public int size() {
        return inputs.size();
    }

    /**
     * Returns the input tuple at the specified position in the underlying list. Throws an
     * {@link IndexOutOfBoundsException} if the index is out of range.
     *
     * @param index
     *            Index of the input tuple to return.
     * @return The input tuple at the specified position.
     *
     */
    public SnippetInput get(int index) {
        if (0 <= index && index < size()) {
            return inputs.get(index);
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    /**
     * Returns an array of input tuples.
     *
     * @return An array of input tuples.
     */
    public SnippetInput[] toArray() {
        return inputs.toArray(new SnippetInput[size()]);
    }

    /**
     * Adds the specified input tuple to the container. Throws an {@link IllegalArgumentException}
     * if the input is null or the parameter count does not match.
     *
     * @param input
     *            The input tuple.
     * @return This object.
     */
    public SnippetInputContainer add(SnippetInput input) {
        if (input == null) {
            throw new IllegalArgumentException("The input must not be null");
        } else if (input.getParameterCount() != parameterCount) {
            throw new IllegalArgumentException("Parameter count of the input must match "
                    + "the parameter count of the container");
        }

        inputs.add(input);
        return this;
    }

    /**
     * Adds the specified input tuple to the container. Throws an {@link IllegalArgumentException}
     * if the the parameter count does not match.
     *
     * @param parameters
     *            The parameters of the input.
     * @return This object.
     */
    public SnippetInputContainer addByParameters(Object... parameters) {
        return addByExpectedAndParameters(null, parameters);
    }

    /**
     * Adds the specified input tuple to the container. Throws an {@link IllegalArgumentException}
     * if the the parameter count does not match.
     *
     * @param expected
     *            Excepted exception which is thrown when executing the snippet with the specified
     *            parameters. It should be null when no exception is thrown.
     * @param parameters
     *            The parameters of the input.
     * @return This object.
     */
    public SnippetInputContainer addByExpectedAndParameters(Class<? extends Throwable> expected,
            Object... parameters) {
        return add(new SnippetInput(expected, parameters));
    }
}
