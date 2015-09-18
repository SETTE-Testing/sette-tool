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
// NOTE revise this file
package hu.bme.mit.sette.common.validator.reflection;

import hu.bme.mit.sette.common.validator.AbstractValidator;
import hu.bme.mit.sette.common.validator.exceptions.reflection.ConstructorValidationException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import org.apache.commons.lang3.Validate;

/**
 * Validator for constructors.
 */
public final class ConstructorValidator extends AbstractValidator<Constructor<?>> {
    /**
     * Instantiates a new constructor validator.
     *
     * @param constructor
     *            the constructor
     */
    public ConstructorValidator(Constructor<?> constructor) {
        super(constructor);

        if (constructor == null) {
            this.addException("The constructor must not be null");
        }
    }

    @Override
    public void addException(String message, Throwable cause) {
        this.addException(new ConstructorValidationException(message, getSubject(), cause));
    }

    /**
     * Sets the required modifiers for the constructor.
     *
     * @param modifiers
     *            the required modifiers for the constructor
     * @return this object
     */
    public ConstructorValidator withModifiers(int modifiers) {
        if (getSubject() != null) {
            Constructor<?> constructor = getSubject();

            if ((constructor.getModifiers() & modifiers) != modifiers) {
                this.addException(String.format(
                        "The constructor must have all the "
                                + "specified modifiers\n(modifiers: [%s])",
                        Modifier.toString(modifiers)));
            }
        }

        return this;
    }

    /**
     * Sets the prohibited modifiers for the constructor.
     *
     * @param modifiers
     *            the prohibited modifiers for the constructor.
     * @return this object
     */
    public ConstructorValidator withoutModifiers(int modifiers) {
        if (getSubject() != null) {
            Constructor<?> constructor = getSubject();

            if ((constructor.getModifiers() & modifiers) != 0) {
                this.addException(String.format(
                        "The constructor must not have any of "
                                + "the specified modifiers\n(modifiers: [%s])",
                        Modifier.toString(modifiers)));
            }
        }

        return this;
    }

    /**
     * Sets whether the constructor should be synthetic or not.
     *
     * @param isSynthetic
     *            true if the constructor should be synthetic, false if it should not be
     * @return this object
     */
    public ConstructorValidator synthetic(boolean isSynthetic) {
        if (getSubject() != null) {
            Constructor<?> constructor = getSubject();

            if (isSynthetic ^ constructor.isSynthetic()) {
                String must;

                if (isSynthetic) {
                    must = "must";
                } else {
                    must = "must not";
                }

                this.addException(String.format("The constructor %s be synthetic", must));
            }
        }

        return this;
    }

    /**
     * Sets the required parameter count for the constructor.
     *
     * @param parameterCount
     *            the required parameter count for the constructor.
     * @return this object
     */
    public ConstructorValidator parameterCount(int parameterCount) {
        Validate.isTrue(parameterCount >= 0,
                "The required parameter count must be a non-negative number");

        if (getSubject() != null) {
            Constructor<?> constructor = getSubject();

            if (constructor.getParameterTypes().length != parameterCount) {
                if (parameterCount == 0) {
                    this.addException("The constructor must not have any parameters");
                } else if (parameterCount == 1) {
                    this.addException("The constructor must have exactly 1 parameter");
                } else {
                    this.addException(String.format(
                            "The constructor must have exactly %d parameters", parameterCount));
                }
            }
        }

        return this;
    }
}
