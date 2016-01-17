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
package hu.bme.mit.sette.core.model.snippet;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import com.google.common.collect.ImmutableSortedMap;

import hu.bme.mit.sette.core.util.reflection.SetteAnnotationUtils;
import hu.bme.mit.sette.core.validator.ClassExecutableValidator;
import hu.bme.mit.sette.core.validator.ClassFieldValidator;
import hu.bme.mit.sette.core.validator.ClassValidator;
import hu.bme.mit.sette.core.validator.ValidationContext;
import hu.bme.mit.sette.core.validator.ValidationException;
import lombok.Getter;
import lombok.NonNull;

/**
 * Represents a snippet input factory container (which is a Java class).
 */
public final class SnippetInputFactoryContainer
        implements Comparable<SnippetInputFactoryContainer> {
    /** The Java class of snippet input container. */
    @Getter
    private final Class<?> javaClass;

    /** The snippet input factories. */
    @Getter
    private final ImmutableSortedMap<String, SnippetInputFactory> inputFactories;

    /** The corresponding snippet container. */
    @Getter
    private final SnippetContainer snippetContainer;

    /**
     * Instantiates a new snippet input factory container.
     *
     * @param snippetContainer
     *            the snippet container
     * @param javaClass
     *            the java class
     * @throws ValidationException
     *             if validation fails
     */
    SnippetInputFactoryContainer(SnippetContainer snippetContainer, Class<?> javaClass)
            throws ValidationException {
        Validate.notNull(snippetContainer, "The snippet container must not be null");
        Validate.notNull(javaClass, "The Java class must not be null");
        this.snippetContainer = snippetContainer;
        this.javaClass = javaClass;

        // start validation
        ValidationContext vc = new ValidationContext(SnippetInputFactoryContainer.class);

        // validate class
        validateClass(vc);
        // validate fields
        validateFields(vc);
        // validate constructor
        validateConstructor(vc);
        // validate methods and get factory methods
        Map<String, Method> factoryMethods = validateMethods(vc);

        // save data to fields
        Map<String, SnippetInputFactory> tmpInputFactories = new HashMap<>();

        // add factory methods
        for (Method method : factoryMethods.values()) {
            try {

                SnippetInputFactory inputFactory = new SnippetInputFactory(this, method);
                tmpInputFactories.put(method.getName(), inputFactory);
            } catch (ValidationException ex) {
                // FIXME
                throw ex;
            }
        }

        vc.validate();
        inputFactories = ImmutableSortedMap.copyOf(tmpInputFactories);
    }

    /**
     * Validates the class and its annotations.
     *
     * @param validationContext
     *            a validation context
     */
    private void validateClass(ValidationContext validationContext) {
        // check: "public final class", no superclass, interface, declared
        // class, exactly one constructor
        ClassValidator v = new ClassValidator(javaClass);
        v.isRegular();
        v.withModifiers(Modifier.PUBLIC | Modifier.FINAL);
        v.withoutModifiers(Modifier.ABSTRACT);
        v.superclass(Object.class).interfaceCount(0);
        v.declaredConstructorCount(1);

        // check: no SETTE annotations
        if (!SetteAnnotationUtils.getSetteAnnotations(javaClass).isEmpty()) {
            v.addError("The class must not have any SETTE annotations");
        }

        validationContext.addValidator(v);
    }

    /**
     * Validates the fields of the class.
     *
     * @param validationContext
     *            a validation context
     */
    private void validateFields(ValidationContext validationContext) {
        // check: no declared fields
        for (Field field : javaClass.getDeclaredFields()) {
            if (field.isSynthetic()) {
                // skip for synthetic fields (e.g. switch for enum generates
                // synthetic methods and fields)
                continue;
            }

            ClassFieldValidator v = new ClassFieldValidator(field);
            v.addError("The class must not declare fields");
            validationContext.addValidator(v);
        }
    }

    /**
     * Validates the constructor of the class.
     *
     * @param validationContext
     *            a validation context
     */
    private void validateConstructor(ValidationContext validationContext) {
        if (javaClass.getDeclaredConstructors().length != 1) {
            // constructor count is validated with the class
            return;
        }

        Constructor<?> constructor = javaClass.getDeclaredConstructors()[0];
        ClassExecutableValidator v = new ClassExecutableValidator(constructor);
        v.withModifiers(Modifier.PRIVATE).parameterCount(0);

        // check: constructor throws new
        // UnsupportedOperationException("Static class")

        Throwable exception = null;
        try {
            // call the private ctor
            constructor.setAccessible(true);
            constructor.newInstance();
        } catch (Exception ex) {
            exception = ex.getCause();
        } finally {
            // restore visibility
            constructor.setAccessible(false);
        }

        if (exception == null || !exception.getClass().equals(UnsupportedOperationException.class)
                || !exception.getMessage().equals("Static class")) {
            v.addError("The constructor must throw an "
                    + "UnsupportedOperationException with the message \"Static class\"");
        }

        validationContext.addValidator(v);
    }

    /**
     * Validates methods of the class.
     *
     * @param validationContext
     *            a validation context
     * @return a map containing the input factory methods by their name
     */
    private Map<String, Method> validateMethods(ValidationContext validationContext) {
        // check: only "public static" or synthetic methods
        Map<String, Method> factoryMethods = new HashMap<String, Method>();

        for (Method method : javaClass.getDeclaredMethods()) {
            if (method.isSynthetic()) {
                // skip synthetic methods
                continue;
            }

            ClassExecutableValidator v = new ClassExecutableValidator(method);

            if (factoryMethods.get(method.getName()) != null) {
                v.addError("The method must have a unique name");
            }

            v.withModifiers(Modifier.PUBLIC | Modifier.STATIC);
            v.withoutModifiers(Modifier.ABSTRACT | Modifier.FINAL | Modifier.NATIVE);

            factoryMethods.put(method.getName(), method);

            validationContext.addValidator(v);
        }

        return factoryMethods;
    }

    @Override
    public int compareTo(@NonNull SnippetInputFactoryContainer o) {
        return javaClass.getName().compareTo(o.javaClass.getName());
    }
}
