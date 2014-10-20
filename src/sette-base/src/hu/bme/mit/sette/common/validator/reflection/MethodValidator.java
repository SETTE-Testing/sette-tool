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
package hu.bme.mit.sette.common.validator.reflection;

import hu.bme.mit.sette.common.validator.AbstractValidator;
import hu.bme.mit.sette.common.validator.exceptions.reflection.MethodValidationException;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Validator for methods.
 */
public final class MethodValidator extends AbstractValidator<Method> {
    /**
     * Instantiates a new method validator.
     *
     * @param method
     *            the method
     */
    public MethodValidator(final Method method) {
        super(method);

        if (method == null) {
            this.addException("The method must not be null");
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * hu.bme.mit.sette.common.validator.AbstractValidator#addException(java
     * .lang.String, java.lang.Throwable)
     */
    @Override
    public void addException(final String message, final Throwable cause) {
        this.addException(new MethodValidationException(message,
                getSubject(), cause));
    }

    /**
     * Sets the required modifiers for the method.
     *
     * @param modifiers
     *            the required modifiers for the method
     * @return this object
     */
    public MethodValidator withModifiers(final int modifiers) {
        if (getSubject() != null) {
            Method method = getSubject();

            if ((method.getModifiers() & modifiers) != modifiers) {
                this.addException(String.format("The method must have "
                        + "all the specified modifiers\n"
                        + "(modifiers: [%s])",
                        Modifier.toString(modifiers)));
            }
        }

        return this;
    }

    /**
     * Sets the prohibited modifiers for the method.
     *
     * @param modifiers
     *            the prohibited modifiers for the method.
     * @return this object
     */
    public MethodValidator withoutModifiers(final int modifiers) {
        if (getSubject() != null) {
            Method method = getSubject();

            if ((method.getModifiers() & modifiers) != 0) {
                this.addException(String.format(
                        "The method must not have "
                                + "any of the specified modifiers\n"
                                + "(modifiers: [%s])",
                                Modifier.toString(modifiers)));
            }
        }

        return this;
    }

    /**
     * Sets whether the method should be synthetic or not.
     *
     * @param isSynthetic
     *            true if the method should be synthetic, false if it should not
     *            be
     * @return this object
     */
    public MethodValidator synthetic(final boolean isSynthetic) {
        if (getSubject() != null) {
            Method method = getSubject();

            if (isSynthetic ^ method.isSynthetic()) {
                String must;

                if (isSynthetic) {
                    must = "must";
                } else {
                    must = "must not";
                }

                this.addException(String.format(
                        "The method %s be synthetic", must));
            }
        }

        return this;
    }
}
