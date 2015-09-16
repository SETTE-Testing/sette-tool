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

import hu.bme.mit.sette.common.util.SetteAnnotationUtils;
import hu.bme.mit.sette.common.validator.AbstractValidator;
import hu.bme.mit.sette.common.validator.GeneralValidator;
import hu.bme.mit.sette.common.validator.exceptions.ValidatorException;
import hu.bme.mit.sette.common.validator.reflection.ClassType;
import hu.bme.mit.sette.common.validator.reflection.ClassValidator;
import hu.bme.mit.sette.common.validator.reflection.ConstructorValidator;
import hu.bme.mit.sette.common.validator.reflection.FieldValidator;
import hu.bme.mit.sette.common.validator.reflection.MethodValidator;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.Validate;

/**
 * Represents a snippet input factory container (which is a Java class).
 */
public final class SnippetInputFactoryContainer
        implements Comparable<SnippetInputFactoryContainer> {
    /** The Java class of snippet input container. */
    private final Class<?> javaClass;

    /** The snippet input factories. */
    private final Map<String, SnippetInputFactory> inputFactories;

    /** The corresponding snippet container. */
    private final SnippetContainer snippetContainer;

    /**
     * Instantiates a new snippet input factory container.
     *
     * @param snippetContainer
     *            the snippet container
     * @param javaClass
     *            the java class
     * @param classLoader
     *            the class loader for loading snippet project classes
     * @throws ValidatorException
     *             if validation has failed
     */
    SnippetInputFactoryContainer(SnippetContainer snippetContainer, Class<?> javaClass,
            ClassLoader classLoader) throws ValidatorException {
        Validate.notNull(snippetContainer, "The snippet container must not be null");
        Validate.notNull(javaClass, "The Java class must not be null");
        this.snippetContainer = snippetContainer;
        this.javaClass = javaClass;

        // start validation
        GeneralValidator validator = new GeneralValidator(SnippetInputFactoryContainer.class);

        // validate class
        validateClass(validator);
        // validate fields
        validateFields(validator);
        // validate constructor
        validateConstructor(validator);
        // validate methods and get factory methods
        Map<String, Method> factoryMethods = validateMethods(validator);

        // save data to fields
        inputFactories = new HashMap<>();

        // add factory methods
        for (Method method : factoryMethods.values()) {
            try {

                SnippetInputFactory inputFactory = new SnippetInputFactory(this, method,
                        classLoader);
                inputFactories.put(method.getName(), inputFactory);
            } catch (ValidatorException e) {
                validator.addChild(e.getValidator());
            }
        }

        // check that there is no snippet method without factory method
        for (Snippet snippet : snippetContainer.getSnippets().values()) {
            if (snippet.getInputFactory() == null) {
                MethodValidator v = new MethodValidator(snippet.getMethod());
                v.addException("The corresponting input factory was not found");
                validator.addChildIfInvalid(v);
            }
        }

        validator.validate();
    }

    /**
     * Validates the class and its annotations.
     *
     * @param validator
     *            a validator
     */
    private void validateClass(AbstractValidator<?> validator) {
        // check: "public final class", no superclass, interface, declared
        // class, exactly one constructor
        ClassValidator v = new ClassValidator(javaClass);
        v.type(ClassType.REGULAR_CLASS);
        v.withModifiers(Modifier.PUBLIC | Modifier.FINAL);
        v.withoutModifiers(Modifier.ABSTRACT);
        v.synthetic(false);
        v.superclass(Object.class).interfaceCount(0).memberClassCount(0);
        v.declaredConstructorCount(1);

        // check: no SETTE annotations
        if (!SetteAnnotationUtils.getSetteAnnotations(javaClass).isEmpty()) {
            v.addException("The class must not have any SETTE annotations");
        }

        validator.addChildIfInvalid(v);
    }

    /**
     * Validates the fields of the class.
     *
     * @param validator
     *            a validator
     */
    private void validateFields(AbstractValidator<?> validator) {
        // check: no declared fields
        for (Field field : javaClass.getDeclaredFields()) {
            if (field.isSynthetic()) {
                // skip for synthetic fields (e.g. switch for enum generates
                // synthetic methods and fields)
                continue;
            }

            FieldValidator v = new FieldValidator(field);
            v.addException("The class must not declare fields");
            validator.addChildIfInvalid(v);
        }
    }

    /**
     * Validates the constructor of the class.
     *
     * @param validator
     *            a validator
     */
    private void validateConstructor(AbstractValidator<?> validator) {
        if (javaClass.getDeclaredConstructors().length != 1) {
            // constructor count is validated with the class
            return;
        }

        Constructor<?> constructor = javaClass.getDeclaredConstructors()[0];
        ConstructorValidator v = new ConstructorValidator(constructor);
        v.withModifiers(Modifier.PRIVATE).parameterCount(0);
        v.synthetic(false);

        // check: constructor throws new
        // UnsupportedOperationException("Static class")

        Throwable exception = null;
        try {
            // call the private ctor
            constructor.setAccessible(true);
            constructor.newInstance();
        } catch (Exception e) {
            exception = e.getCause();
        } finally {
            // restore visibility
            constructor.setAccessible(false);
        }

        if (exception == null || !exception.getClass().equals(UnsupportedOperationException.class)
                || !exception.getMessage().equals("Static class")) {
            v.addException("The constructor must throw an "
                    + "UnsupportedOperationException with the message \"Static class\"");
        }

        validator.addChildIfInvalid(v);
    }

    /**
     * Validates methods of the class.
     *
     * @param validator
     *            a validator
     * @return a map containing the input factory methods by their name
     */
    private Map<String, Method> validateMethods(AbstractValidator<?> validator) {
        // check: only "public static" or synthetic methods
        Map<String, Method> factoryMethods = new HashMap<String, Method>();

        for (Method method : javaClass.getDeclaredMethods()) {
            if (method.isSynthetic()) {
                // skip synthetic methods
                continue;
            }

            MethodValidator v = new MethodValidator(method);

            if (factoryMethods.get(method.getName()) != null) {
                v.addException("The method must have a unique name");
            }

            v.withModifiers(Modifier.PUBLIC | Modifier.STATIC);
            v.withoutModifiers(
                    Modifier.ABSTRACT | Modifier.FINAL | Modifier.NATIVE | Modifier.SYNCHRONIZED);

            factoryMethods.put(method.getName(), method);

            validator.addChildIfInvalid(v);
        }

        return factoryMethods;
    }

    /**
     * Gets the Java class of snippet input container.
     *
     * @return the Java class of snippet input container
     */
    public Class<?> getJavaClass() {
        return javaClass;
    }

    /**
     * Gets the snippet input factories.
     *
     * @return the snippet input factories
     */
    public Map<String, SnippetInputFactory> getInputFactories() {
        return Collections.unmodifiableMap(inputFactories);
    }

    /**
     * Gets the corresponding snippet container.
     *
     * @return the corresponding snippet container
     */
    public SnippetContainer getSnippetContainer() {
        return snippetContainer;
    }

    /** The {@link Comparator} for the class. */
    public static final Comparator<SnippetInputFactoryContainer> COMPARATOR;

    static {
        COMPARATOR = new Comparator<SnippetInputFactoryContainer>() {
            @Override
            public int compare(SnippetInputFactoryContainer o1,
                    final SnippetInputFactoryContainer o2) {
                if (o1 == null) {
                    return -1;
                } else if (o2 == null) {
                    return 1;
                } else {
                    return o1.javaClass.getName().compareTo(o2.javaClass.getName());
                }
            }
        };
    }

    @Override
    public int compareTo(SnippetInputFactoryContainer o) {
        return SnippetInputFactoryContainer.COMPARATOR.compare(this, o);
    }
}
