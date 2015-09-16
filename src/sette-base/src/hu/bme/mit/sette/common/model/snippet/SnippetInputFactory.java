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
// TODO z revise this file
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
     * @param container
     *            the container
     * @param method
     *            the method
     * @param classLoader
     *            the class loader for loading snippet project classes
     * @throws ValidatorException
     *             if validation has failed
     */
    SnippetInputFactory(SnippetInputFactoryContainer container, Method method,
            ClassLoader classLoader) throws ValidatorException {
        Validate.notNull(container, "The container must not be null");
        Validate.notNull(method, "The method must not be null");
        Validate.isTrue(container.getJavaClass().equals(method.getDeclaringClass()),
                "The method must be declared in the Java class stored by the container\n"
                        + "(container.javaClass: [%s])\n(method.declaringClass: [%s])",
                container.getJavaClass(), method.getDeclaringClass());
        this.container = container;
        this.method = method;

        // start validation
        MethodValidator v = new MethodValidator(method);
        // modifiers are checked by the container when parsing the Java class

        // check SETTE annotations
        if (!SetteAnnotationUtils.getSetteAnnotations(method).isEmpty()) {
            v.addException("The method must not have any SETTE annotations");
        }

        // check parameter types
        if (method.getParameterTypes().length != 0) {
            v.addException("The method must not have any parameter types");
        }

        // check return type
        if (!method.getReturnType().equals(SnippetInputContainer.class)) {
            v.addException("The method must have the return type of "
                    + SnippetInputContainer.class.getSimpleName());
        }

        // get snippet method
        snippetMethod = container.getSnippetContainer().getSnippets().get(method.getName());

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
     *             if the underlying method is inaccessible because of Java language access control
     * @throws InvocationTargetException
     *             if the underlying method throws an exception
     */
    public SnippetInputContainer getInputs()
            throws IllegalAccessException, InvocationTargetException {
        return (SnippetInputContainer) method.invoke(null);
    }
}
