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

/**
 * Holds parameters and the type of the produced exception (if any) for a code snippet method. The
 * return value is not stored since a code snippet should be deterministic and independent from the
 * environment, always returning the same value for the same parameters. Please note that this class
 * does not perform any type checking among the parameters, thus type mismatch can cause a runtime
 * failure.
 * <p>
 * This class is immutable, thus the array of parameters and the type of the exception cannot be
 * modified. However, this class only returns a shallow copy of the array, so the attributes of the
 * parameters and the exception can be modified, but it should be avoided.
 * <p>
 * For usage examples see {@link SnippetInputTest}.
 */
public final class SnippetInput {
    /** Array of code snippet method parameters. */
    private final Object[] parameters;

    /**
     * Excepted exception which is thrown when executing the snippet with the specified parameters.
     * It should be null when no exception is thrown.
     */
    private final Class<? extends Throwable> expected;

    /**
     * Creates an instance with the specified excepted exception and parameters.
     *
     * @param expected
     *            Excepted exception which is thrown when executing the snippet with the specified
     *            parameters. It should be <code>null</code> when no exception is thrown.
     * @param parameters
     *            The parameters (input for the code snippet method).
     */
    public SnippetInput(Class<? extends Throwable> expected, Object... parameters) {
        if (parameters == null) {
            this.parameters = new Object[0];
        } else {
            this.parameters = new Object[parameters.length];
            System.arraycopy(parameters, 0, this.parameters, 0, parameters.length);
        }

        this.expected = expected;
    }

    /**
     * Returns the number of the parameters.
     *
     * @return The number of the parameters.
     */
    public int getParameterCount() {
        return parameters.length;
    }

    /**
     * Returns the parameter at the specified position in the underlying array. Throws an
     * {@link IndexOutOfBoundsException} if the index is out of range.
     *
     * @param index
     *            Index of the parameter to return.
     * @return The parameter at the specified position.
     *
     */
    public Object getParameter(int index) {
        if (0 <= index && index < parameters.length) {
            return parameters[index];
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    /**
     * Returns a shallow copy of the array containing the parameters. Modifications to the returned
     * copy should be avoided.
     *
     * @return An array containing the parameters.
     */
    public Object[] getParameters() {
        Object[] copy = new Object[parameters.length];
        System.arraycopy(parameters, 0, copy, 0, parameters.length);
        return copy;
    }

    /**
     * Returns the type of the expected exception.
     *
     * @return The type of the expected exception.
     */
    public Class<? extends Throwable> getExpected() {
        return expected;
    }
}
