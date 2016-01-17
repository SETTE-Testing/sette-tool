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

import java.lang.reflect.Modifier;

/**
 * Base validator class for reflection-related classes.
 */
public abstract class ReflectionValidator<T> extends Validator<T> {
    /**
     * Constructor to pass arguments to the superclass.
     * 
     * @param subject
     *            the subject
     */
    public ReflectionValidator(T subject) {
        super(subject);
    }

    /**
     * Adds a validation error to the validator if the actual modifiers does not contain the
     * expected ones.
     *
     * @param expected
     *            the modifiers to have
     * @param actual
     *            the modifiers of the subject
     */
    final void addErrorIfNotWithModifiers(int expected, int actual) {
        // needed because modifiers in Class/Executable/Field are not inherited from a superclass
        if ((actual & expected) != expected) {
            addError(String.format("modifiers: must have %s but has %s",
                    Modifier.toString(expected), Modifier.toString(actual)));
        }
    }

    /**
     * Adds a validation error to the validator if the actual modifiers contain any of the expected
     * ones.
     *
     * @param expected
     *            the modifiers to not have
     * @param actual
     *            the modifiers of the subject
     */
    final void addErrorIfNotWithoutModifiers(int expected, int actual) {
        // needed because modifiers in Class/Executable/Field are not inherited from a superclass
        if ((actual & expected) != 0) {
            addError(String.format("modifiers: must not have any of %s but has %s",
                    Modifier.toString(expected), Modifier.toString(actual)));
        }
    }
}
