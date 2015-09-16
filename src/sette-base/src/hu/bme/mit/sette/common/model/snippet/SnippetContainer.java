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

import hu.bme.mit.sette.annotations.SetteNotSnippet;
import hu.bme.mit.sette.annotations.SetteSnippetContainer;
import hu.bme.mit.sette.common.snippets.JavaVersion;
import hu.bme.mit.sette.common.util.SetteAnnotationUtils;
import hu.bme.mit.sette.common.util.reflection.AnnotationMap;
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

import javax.lang.model.type.NullType;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

/**
 * Represents a snippet container (which is a Java class).
 */
public final class SnippetContainer implements Comparable<SnippetContainer> {
    /** The Java class of the snippet container. */
    private final Class<?> javaClass;

    /** The category . */
    private final String category;

    /** The goal. */
    private final String goal;

    /** The required Java version. */
    private final JavaVersion requiredJavaVersion;

    /** The code snippets. */
    private final Map<String, Snippet> snippets;

    /** The corresponding snippet input factory container. */
    private final SnippetInputFactoryContainer inputFactoryContainer;

    /**
     * Instantiates a new snippet container.
     *
     * @param javaClass
     *            the Java class
     * @param classLoader
     *            the class loader for loading snippet project classes
     * @throws ValidatorException
     *             if validation has failed
     */
    SnippetContainer(Class<?> javaClass, ClassLoader classLoader) throws ValidatorException {
        Validate.notNull(javaClass, "The Java class must not be null");
        this.javaClass = javaClass;

        // start validation
        GeneralValidator validator = new GeneralValidator(SnippetContainer.class);

        // validate class and get container annotation
        SetteSnippetContainer containerAnnotation = validateClass(validator);
        // validate fields
        validateFields(validator);
        // validate constructor
        validateConstructor(validator);
        // validate methods and get snippet methods
        Map<String, Method> snippetMethods = validateMethods(validator);

        // save annotation data to fields
        category = containerAnnotation.category();
        goal = containerAnnotation.goal();
        requiredJavaVersion = containerAnnotation.requiredJavaVersion();

        // add snippet methods
        snippets = new HashMap<>();

        for (Method method : snippetMethods.values()) {
            if (method != null) {
                // the method is a snippet method
                try {
                    // create and add snippet object
                    Snippet snippet = new Snippet(this, method, classLoader);
                    snippets.put(method.getName(), snippet);
                } catch (ValidatorException e) {
                    validator.addChild(e.getValidator());
                }
            }
        }

        // input factory container
        SnippetInputFactoryContainer inputFactCont = null;
        if (!containerAnnotation.inputFactoryContainer().equals(NullType.class)) {
            // input factory container is present
            try {
                // create input factory container
                inputFactCont = new SnippetInputFactoryContainer(this,
                        containerAnnotation.inputFactoryContainer(), classLoader);
            } catch (ValidatorException e) {
                validator.addChild(e.getValidator());
            }
        }

        // set input factory container
        inputFactoryContainer = inputFactCont;

        validator.validate();
    }

    /**
     * Validates the class and its annotations.
     *
     * @param validator
     *            a validator
     * @return the {@link SetteSnippetContainer} annotation
     */
    private SetteSnippetContainer validateClass(AbstractValidator<?> validator) {
        // check: "public final class", no superclass, interface, declared
        // class, exactly one constructor
        ClassValidator v = new ClassValidator(javaClass);
        v.type(ClassType.REGULAR_CLASS);
        v.withModifiers(Modifier.PUBLIC | Modifier.FINAL);
        v.withoutModifiers(Modifier.ABSTRACT);
        v.synthetic(false);
        v.superclass(Object.class).interfaceCount(0).memberClassCount(0);
        v.declaredConstructorCount(1);

        // check: only @SetteSnippetContainer
        SetteSnippetContainer containerAnn = null;

        AnnotationMap classAnns = SetteAnnotationUtils.getSetteAnnotations(javaClass);

        containerAnn = (SetteSnippetContainer) classAnns.get(SetteSnippetContainer.class);

        if (containerAnn == null) {
            v.addException("The Java class must have the annotation @"
                    + SetteSnippetContainer.class.getSimpleName());
        } else {
            if (classAnns.size() != 1) {
                v.addException("The Java class must not have any SETTE annotations other than @"
                        + SetteSnippetContainer.class.getSimpleName());
            }

            if (StringUtils.isBlank(containerAnn.category())) {
                v.addException("The category in @" + SetteSnippetContainer.class.getSimpleName()
                        + " must not be blank");
            }

            if (StringUtils.isBlank(containerAnn.goal())) {
                v.addException("The goal in @" + SetteSnippetContainer.class.getSimpleName()
                        + " must not be blank");
            }

            if (containerAnn.requiredJavaVersion() == null) {
                v.addException("The reqired Java version in @"
                        + SetteSnippetContainer.class.getSimpleName() + " must not be null");
            }
        }

        validator.addChildIfInvalid(v);

        return containerAnn;
    }

    /**
     * Validates the fields of the class.
     *
     * @param validator
     *            a validator
     */
    private void validateFields(AbstractValidator<?> validator) {
        // check: only constant ("public static final") or synthetic fields
        for (Field field : javaClass.getDeclaredFields()) {
            if (field.isSynthetic()) {
                // skip for synthetic fields (e.g. switch for enum generates
                // synthetic methods and fields)
                continue;
            }

            FieldValidator v = new FieldValidator(field);
            v.withModifiers(Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL);
            v.withoutModifiers(Modifier.SYNCHRONIZED | Modifier.TRANSIENT | Modifier.VOLATILE);

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
            v.addException("The constructor must throw an UnsupportedOperationException with "
                    + "the message \"Static class\"");
        }

        validator.addChildIfInvalid(v);
    }

    /**
     * Validates methods of the class.
     *
     * @param validator
     *            a validator
     * @return a map containing the snippet methods by their name
     */
    private Map<String, Method> validateMethods(AbstractValidator<?> validator) {
        // check: only "[public|private] static" or synthetic methods
        Map<String, Method> snippetMethods = new HashMap<String, Method>();

        for (Method method : javaClass.getDeclaredMethods()) {
            if (method.isSynthetic()) {
                // skip synthetic methods
                continue;
            }

            MethodValidator v = new MethodValidator(method);

            if (snippetMethods.get(method.getName()) != null) {
                v.addException("The method must have a unique name");
            }

            int methodModifiers = method.getModifiers();

            if (!Modifier.isPublic(methodModifiers) && !Modifier.isPrivate(methodModifiers)) {
                v.addException("The method must be public or private");
            }

            v.withModifiers(Modifier.STATIC);
            v.withoutModifiers(
                    Modifier.ABSTRACT | Modifier.FINAL | Modifier.NATIVE | Modifier.SYNCHRONIZED);

            AnnotationMap methodAnns = SetteAnnotationUtils.getSetteAnnotations(method);

            if (Modifier.isPublic(methodModifiers)) {
                if (methodAnns.get(SetteNotSnippet.class) == null) {
                    // should be snippet, validated by Snippet class and added
                    // later
                    snippetMethods.put(method.getName(), method);
                } else {
                    // not snippet
                    snippetMethods.put(method.getName(), null);

                    if (methodAnns.size() != 1) {
                        v.addException("The method must not have any other SETTE annotations "
                                + "if it is not a snippet.");
                    }
                }
            } else {
                // method is private
                if (methodAnns.size() != 0) {
                    v.addException("The method must not have any SETTE annotations");
                }
            }

            validator.addChildIfInvalid(v);
        }

        return snippetMethods;
    }

    /**
     * Gets the Java class of the snippet container.
     *
     * @return the Java class of the snippet container
     */
    public Class<?> getJavaClass() {
        return javaClass;
    }

    /**
     * Gets the category .
     *
     * @return the category
     */
    public String getCategory() {
        return category;
    }

    /**
     * Gets the goal.
     *
     * @return the goal
     */
    public String getGoal() {
        return goal;
    }

    /**
     * Gets the required Java version.
     *
     * @return the required Java version
     */
    public JavaVersion getRequiredJavaVersion() {
        return requiredJavaVersion;
    }

    /**
     * Gets the code snippets.
     *
     * @return the code snippets
     */
    public Map<String, Snippet> getSnippets() {
        return Collections.unmodifiableMap(snippets);
    }

    /**
     * Gets the corresponding snippet input factory container.
     *
     * @return the corresponding snippet input factory container
     */
    public SnippetInputFactoryContainer getInputFactoryContainer() {
        return inputFactoryContainer;
    }

    /** The {@link Comparator} for the class. */
    public static final Comparator<SnippetContainer> COMPARATOR;

    static {
        COMPARATOR = new Comparator<SnippetContainer>() {
            @Override
            public int compare(SnippetContainer o1, SnippetContainer o2) {
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
    public int compareTo(SnippetContainer o) {
        return SnippetContainer.COMPARATOR.compare(this, o);
    }
}
