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
package hu.bme.mit.sette.core.model.snippet;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.google.common.base.Preconditions;

import hu.bme.mit.sette.common.snippets.SnippetInputContainer;
import hu.bme.mit.sette.core.util.reflection.SetteAnnotationUtils;
import hu.bme.mit.sette.core.validator.ClassExecutableValidator;
import hu.bme.mit.sette.core.validator.ValidationException;
import lombok.Getter;
import lombok.NonNull;

/**
 * Represents a snippet input factory (which is a Java method).
 */
public final class SnippetInputFactory {
    /** The snippet input factory container. */
    @Getter
    private final SnippetInputFactoryContainer container;

    /** The method for the snippet input factory. */
    @Getter
    private final Method method;

    /** The corresponding code snippet. */
    @Getter
    private final Snippet snippetMethod;

    /**
     * Instantiates a new snippet input factory.
     *
     * @param container
     *            the container
     * @param method
     *            the method
     * @throws ValidationException
     *             if validation fails
     */
    SnippetInputFactory(@NonNull SnippetInputFactoryContainer container, @NonNull Method method)
            throws ValidationException {
        Preconditions.checkArgument(container.getJavaClass().equals(method.getDeclaringClass()),
                "The method must be declared in the Java class stored by the container\n"
                        + "(container.javaClass: [%s])\n(method.declaringClass: [%s])",
                container.getJavaClass(), method.getDeclaringClass());
        this.container = container;
        this.method = method;

        // start validation
        ClassExecutableValidator v = new ClassExecutableValidator(method);
        // modifiers are checked by the container when parsing the Java class

        // check SETTE annotations
        if (!SetteAnnotationUtils.getSetteAnnotations(method).isEmpty()) {
            v.addError("The method must not have any SETTE annotations");
        }

        // check parameter types
        if (method.getParameterTypes().length != 0) {
            v.addError("The method must not have any parameter types");
        }

        // check return type
        if (!method.getReturnType().equals(SnippetInputContainer.class)) {
            v.addError("The method must have the return type of "
                    + SnippetInputContainer.class.getSimpleName());
        }

        // get snippet method
        snippetMethod = container.getSnippetContainer().getSnippets().get(method.getName());

        if (snippetMethod == null) {
            v.addError("The corresponding snippet method was not found");
        }

        v.validate();
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
