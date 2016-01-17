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
package hu.bme.mit.sette.core.validator;

import static com.google.common.base.Preconditions.checkArgument;

import java.lang.reflect.Executable;

/**
 * Validator for {@link Executable}s, i.e., for constructors and methods.
 */
public final class ClassExecutableValidator extends ReflectionValidator<Executable> {
    /**
     * Instantiates a new constructor/method validator.
     *
     * @param executable
     *            the constructor or method
     */
    public ClassExecutableValidator(Executable executable) {
        super(executable);
    }

    /**
     * Specifies required modifiers for the constructor/method.
     *
     * @param modifiers
     *            required modifiers for the constructor/method.
     * @return this object
     */
    public ClassExecutableValidator withModifiers(int modifiers) {
        addErrorIfNotWithModifiers(modifiers, getSubject().getModifiers());
        return this;
    }

    /**
     * Specifies prohibited modifiers for the constructor/method.
     *
     * @param modifiers
     *            prohibited modifiers for the constructor/method.
     * @return this object
     */
    public ClassExecutableValidator withoutModifiers(int modifiers) {
        addErrorIfNotWithoutModifiers(modifiers, getSubject().getModifiers());
        return this;
    }

    /**
     * Specifies the required parameter count for the constructor/method.
     *
     * @param parameterCount
     *            the required parameter count for the constructor/method.
     * @return this object
     */
    public ClassExecutableValidator parameterCount(int parameterCount) {
        checkArgument(parameterCount >= 0);

        addErrorIfNotEquals("parameter count", parameterCount,
                getSubject().getParameterTypes().length);
        return this;
    }
}
