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

import hu.bme.mit.sette.annotations.SetteIncludeCoverage;
import hu.bme.mit.sette.annotations.SetteRequiredStatementCoverage;
import hu.bme.mit.sette.common.util.SetteAnnotationUtils;
import hu.bme.mit.sette.common.util.reflection.AnnotationMap;
import hu.bme.mit.sette.common.validator.exceptions.ValidatorException;
import hu.bme.mit.sette.common.validator.reflection.MethodValidator;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

/**
 * Represents a code snippet (which is a Java method).
 */
public final class Snippet {
    /**
     * Pattern for method strings in @SetteIncludeCoverage annotation. An example matching the
     * pattern: "methodName(int, my.pkg.MyClass)"
     */
    public static final Pattern METHOD_STRING_PATTERN = Pattern.compile("(.+)\\((.*)\\)");

    /** The snippet container. */
    private final SnippetContainer container;

    /** The method for the snippet. */
    private final Method method;

    /** The required statement coverage. */
    private final double requiredStatementCoverage;

    /** The constructors which should be considered when measuring coverage. */
    private final Set<Constructor<?>> includedConstructors;

    /** The methods which should be considered when measuring coverage. */
    private final Set<Method> includedMethods;

    /** The input factory for the snippet. */
    private SnippetInputFactory inputFactory;

    /**
     * Instantiates a new snippet.
     *
     * @param container
     *            the snippet container
     * @param method
     *            the method
     * @param classLoader
     *            the class loader for loading snippet project classes
     * @throws ValidatorException
     *             if validation has failed
     */
    Snippet(SnippetContainer container, Method method, ClassLoader classLoader)
            throws ValidatorException {
        Validate.notNull(container, "The container must not be null");
        Validate.notNull(method, "The method must not be null");
        Validate.isTrue(container.getJavaClass().equals(method.getDeclaringClass()),
                "The method must be declared in the Java class of the container\n"
                        + "(container.javaClass: [%s])\n(method.declaringClass: [%s])",
                container.getJavaClass(), method.getDeclaringClass());

        this.container = container;
        this.method = method;
        inputFactory = null; // should be set later by setter method

        // start validation
        MethodValidator v = new MethodValidator(method);
        // modifiers are checked by the container when parsing the Java class

        // check SETTE annotations
        AnnotationMap methodAnnots = SetteAnnotationUtils.getSetteAnnotations(method);

        SetteRequiredStatementCoverage reqStmtCovAnnot;
        SetteIncludeCoverage inclCovAnnot;

        reqStmtCovAnnot = (SetteRequiredStatementCoverage) methodAnnots
                .get(SetteRequiredStatementCoverage.class);

        inclCovAnnot = (SetteIncludeCoverage) methodAnnots.get(SetteIncludeCoverage.class);

        if (reqStmtCovAnnot == null) {
            v.addException("Method must have the annotation @"
                    + SetteRequiredStatementCoverage.class.getSimpleName());
        }

        if ((inclCovAnnot != null && methodAnnots.size() != 2)
                || (inclCovAnnot == null && methodAnnots.size() != 1)) {
            v.addException("Method must not have any SETTE annotation other than @"
                    + SetteRequiredStatementCoverage.class.getName() + " and @"
                    + SetteIncludeCoverage.class.getSimpleName());
        }

        // check and parse annotation @SetteRequiredStatementCoverage
        if (reqStmtCovAnnot != null) {
            double value = reqStmtCovAnnot.value();

            if (value < SetteRequiredStatementCoverage.MIN
                    || value > SetteRequiredStatementCoverage.MAX) {
                v.addException(String.format(
                        "Required statement coverage must be between %.2f%% and %.2f%%",
                        SetteRequiredStatementCoverage.MIN, SetteRequiredStatementCoverage.MAX));
            }

            requiredStatementCoverage = value;
        } else {
            requiredStatementCoverage = -1;
        }

        // check and parse annotation @SetteIncludeCoverage
        includedConstructors = new HashSet<>();
        includedMethods = new HashSet<>();
        parseIncludedMethods(inclCovAnnot, v, classLoader);

        v.validate();
    }

    /**
     * Parses the methods which should be considered in coverage.
     *
     * @param annotation
     *            the {@link SetteIncludeCoverage} annotation
     * @param v
     *            a {@link MethodValidator}
     * @param classLoader
     *            the class loader for loading snippet project classes
     */
    private void parseIncludedMethods(SetteIncludeCoverage annotation, MethodValidator v,
            ClassLoader classLoader) {
        if (annotation == null) {
            return;
        }

        Class<?>[] includedClasses = annotation.classes();
        String[] includedMethodStrings = annotation.methods();
        boolean shouldParse = true; // only parse if no validation error

        // check the arrays: not empty, no null element, same lengths
        if (ArrayUtils.isEmpty(includedClasses)) {
            v.addException("The included class list must not be empty");
            shouldParse = false;
        }

        if (ArrayUtils.contains(includedClasses, null)) {
            v.addException("The included class list must not contain null elements");
            shouldParse = false;
        }

        if (ArrayUtils.isEmpty(includedMethodStrings)) {
            v.addException("The included method list must not be empty");
            shouldParse = false;
        }

        if (ArrayUtils.contains(includedMethodStrings, null)) {
            v.addException("The included method list must not contain null elements");
            shouldParse = false;
        }

        if (!ArrayUtils.isSameLength(includedClasses, includedMethodStrings)) {
            v.addException("The included class list and method list must have the same length");
            shouldParse = false;
        }

        if (shouldParse) {
            // check and add methods
            for (int i = 0; i < includedClasses.length; i++) {
                Class<?> includedClass = includedClasses[i];
                String includedMethodString = includedMethodStrings[i].trim();

                if (includedMethodString.equals("*")) {
                    // add all non-synthetic constructors
                    for (Constructor<?> c : includedClass.getDeclaredConstructors()) {
                        if (!c.isSynthetic()) {
                            addIncludedConstructor(c, v);
                        }
                    }
                    // add all non-synthetic methods
                    for (Method m : includedClass.getDeclaredMethods()) {
                        if (!m.isSynthetic()) {
                            addIncludedMethod(m, v);
                        }
                    }
                } else {
                    parseIncludedMethod(includedClass, includedMethodString, v, classLoader);
                }
            }
        }
    }

    /**
     * Parses a method which should be considered in coverage.
     *
     * @param includedClass
     *            the Java class of the method
     * @param includedMethodString
     *            the string representing the included method
     * @param v
     *            a {@link MethodValidator}
     * @param classLoader
     *            the class loader for loading snippet project classes
     */
    private void parseIncludedMethod(Class<?> includedClass, String includedMethodString,
            MethodValidator v, ClassLoader classLoader) {
        Matcher matcher = Snippet.METHOD_STRING_PATTERN.matcher(includedMethodString);

        if (!matcher.matches() || matcher.groupCount() != 2) {
            // invalid method string
            String message = String.format(
                    "The included method string must match "
                            + "the required format.\n(includedMethodString: [%s])",
                    includedMethodString);
            v.addException(message);
        } else {
            // valid method string
            String includedMethodName = matcher.group(1).trim();
            String[] paramTypeStrings = StringUtils.split(matcher.group(2), ',');
            Class<?>[] paramTypes = new Class<?>[paramTypeStrings.length];
            boolean isConstructor = includedMethodName.equals(includedClass.getSimpleName());
            boolean shouldAdd = true; // only add if there was no problem with
            // the parameters

            // check parameter types
            for (int i = 0; i < paramTypes.length; i++) {
                String parameterTypeString = paramTypeStrings[i].trim();

                if (StringUtils.isBlank(parameterTypeString)) {
                    // blank parameter type string
                    String message = String.format(
                            "The included method string has a blank parameter type.\n"
                                    + "(includedMethodString: [%s])\n(index: [%d])",
                            includedMethodString, i);
                    v.addException(message);
                    shouldAdd = false;
                } else {
                    try {
                        paramTypes[i] = ClassUtils.getClass(classLoader, parameterTypeString);
                    } catch (ClassNotFoundException e) {
                        // parameter type was not found
                        String format = "The parameter type in the included method string "
                                + "could not have been loaded.\n(includedMethodString: [%s])\n"
                                + "(index: [%d])";
                        String message = String.format(format, includedMethodString, i);
                        v.addException(message);
                        shouldAdd = false;
                    }
                }
            }

            if (shouldAdd) {
                // get included method object
                if (isConstructor) {
                    try {
                        // only search declared constructors
                        Constructor<?> found = includedClass.getDeclaredConstructor(paramTypes);
                        addIncludedConstructor(found, v);
                    } catch (NoSuchMethodException e) {
                        String format = "Included constructor cannot be found "
                                + "(it must be declared in the class)\n(includedClass: [%s])\n"
                                + "(includedMethodString: [%s])";
                        String message = String.format(format, includedClass, includedMethodString);
                        v.addException(message);
                    }
                } else {
                    try {
                        // only search declared methods
                        Method found = includedClass.getDeclaredMethod(includedMethodName,
                                paramTypes);
                        addIncludedMethod(found, v);
                    } catch (NoSuchMethodException e) {
                        String format = "Included method cannot be found "
                                + "(it must be declared in the class)\n(includedClass: [%s])\n"
                                + "(includedMethodString: [%s])";
                        String message = String.format(format, includedClass, includedMethodString);
                        v.addException(message, e);
                    }
                }
            }
        }
    }

    /**
     * Adds a constructor (which should be considered in coverage) to the object's internal
     * collection.
     *
     * @param c
     *            the constructor
     * @param v
     *            a {@link MethodValidator}
     */
    private void addIncludedConstructor(Constructor<?> c, MethodValidator v) {
        if (includedConstructors.contains(c)) {
            // duplicate
            String message = String.format("The constructor has been already added "
                    + "for included coverage (includedMethod: [%s])", c);
            v.addException(message);
        } else {
            // add method to the list
            includedConstructors.add(c);
        }
    }

    /**
     * Adds a method (which should be considered in coverage) to the object's internal collection.
     *
     * @param m
     *            the method
     * @param v
     *            a {@link MethodValidator}
     */
    private void addIncludedMethod(Method m, MethodValidator v) {
        if (includedMethods.contains(m)) {
            // duplicate
            String message = String.format("The method has been already added "
                    + "for included coverage(includedMethod: [%s])", m);
            v.addException(message);
        } else {
            // add method to the list
            includedMethods.add(m);
        }
    }

    /**
     * Gets the snippet container.
     *
     * @return the snippet container
     */
    public SnippetContainer getContainer() {
        return container;
    }

    /**
     * Gets the method for the snippet.
     *
     * @return the method for the snippet
     */
    public Method getMethod() {
        return method;
    }

    /**
     * Gets the required statement coverage.
     *
     * @return the required statement coverage
     */
    public double getRequiredStatementCoverage() {
        return requiredStatementCoverage;
    }

    /**
     * Gets the constructors which should be considered when measuring coverage.
     *
     * @return the constructors which should be considered when measuring coverage
     */
    public Set<Constructor<?>> getIncludedConstructors() {
        return includedConstructors;
    }

    /**
     * Gets the methods which should be considered when measuring coverage.
     *
     * @return the methods which should be considered when measuring coverage
     */
    public Set<Method> getIncludedMethods() {
        return includedMethods;
    }

    /**
     * Gets the input factory for the snippet.
     *
     * @return the input factory for the snippet
     */
    public SnippetInputFactory getInputFactory() {
        return inputFactory;
    }

    /**
     * Sets the input factory for the snippet. This method should only be called from other snippet
     * model classes.
     *
     * @param inputFactory
     *            the new input factory for the snippet
     */
    void setInputFactory(SnippetInputFactory inputFactory) {
        Validate.notNull(inputFactory, "Input factory must not be null (method: [%s])", method);
        Validate.isTrue(this.inputFactory == null,
                "Input factory has been already set (method: [%s])", method);

        this.inputFactory = inputFactory;
    }
}
