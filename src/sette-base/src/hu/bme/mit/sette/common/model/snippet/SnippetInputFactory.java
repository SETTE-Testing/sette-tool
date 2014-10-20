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
package hu.bme.mit.sette.common.model.snippet;

import hu.bme.mit.sette.common.snippets.SnippetInputContainer;
import hu.bme.mit.sette.common.util.SetteAnnotationUtils;
import hu.bme.mit.sette.common.validator.exceptions.ValidatorException;
import hu.bme.mit.sette.common.validator.reflection.MethodValidator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.lang3.Validate;

/**
 * Represents a snippet input factory (which is a Java method).
 */
public final class SnippetInputFactory {
    /** The snippet input factory container. */
    private final SnippetInputFactoryContainer container;

    /** The method for the snippet input factory. */
    private final Method method;

    /** The corresponding code snippet. */
    private final Snippet snippetMethod;

    /**
     * Instantiates a new snippet input factory.
     *
     * @param pContainer
     *            the container
     * @param pMethod
     *            the method
     * @param classLoader
     *            the class loader for loading snippet project classes
     * @throws ValidatorException
     *             if validation has failed
     */
    SnippetInputFactory(final SnippetInputFactoryContainer pContainer,
            final Method pMethod, final ClassLoader classLoader)
                    throws ValidatorException {
        Validate.notNull(pContainer, "The container must not be null");
        Validate.notNull(pMethod, "The method must not be null");
        Validate.isTrue(
                pContainer.getJavaClass().equals(
                        pMethod.getDeclaringClass()),
                        "The method must be declared in the Java class "
                                + "stored by the container\n"
                                + "(container.javaClass: [%s])\n"
                                + "(method.declaringClass: [%s])",
                                pContainer.getJavaClass(), pMethod.getDeclaringClass());
        container = pContainer;
        method = pMethod;

        // start validation
        MethodValidator v = new MethodValidator(pMethod);
        // modifiers are checked by the container when parsing the Java class

        // check SETTE annotations
        if (!SetteAnnotationUtils.getSetteAnnotations(pMethod)
                .isEmpty()) {
            v.addException("The method must not have any SETTE annotations");
        }

        // check parameter types
        if (pMethod.getParameterTypes().length != 0) {
            v.addException("The method must not have any parameter types");
        }

        // check return type
        if (!pMethod.getReturnType()
                .equals(SnippetInputContainer.class)) {
            v.addException("The method must have the return type of "
                    + SnippetInputContainer.class.getSimpleName());
        }

        // get snippet method
        snippetMethod = pContainer.getSnippetContainer().getSnippets()
                .get(pMethod.getName());

        if (snippetMethod == null) {
            v.addException("The corresponding snippet method was not found");
        }

        v.validate();

        snippetMethod.setInputFactory(this);
    }

    /**
     * Gets the snippet input factory container.
     *
     * @return the snippet input factory container
     */
    public SnippetInputFactoryContainer getContainer() {
        return container;
    }

    /**
     * Gets the method for the snippet input factory.
     *
     * @return the method for the snippet input factory
     */
    public Method getMethod() {
        return method;
    }

    /**
     * Gets the corresponding code snippet.
     *
     * @return the corresponding code snippet
     */
    public Snippet getSnippetMethod() {
        return snippetMethod;
    }

    /**
     * Gets the inputs for the code snippet.
     *
     * @return a snippet input container with the inputs
     * @throws IllegalAccessException
     *             if the underlying method is inaccessible because of Java
     *             language access control
     * @throws InvocationTargetException
     *             if the underlying method throws an exception
     */
    public SnippetInputContainer getInputs()
            throws IllegalAccessException, InvocationTargetException {
        return (SnippetInputContainer) method.invoke(null);
    }
}
