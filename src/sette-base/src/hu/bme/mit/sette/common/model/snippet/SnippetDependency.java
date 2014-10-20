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

import hu.bme.mit.sette.annotations.SetteDependency;
import hu.bme.mit.sette.common.util.SetteAnnotationUtils;
import hu.bme.mit.sette.common.util.reflection.AnnotationMap;
import hu.bme.mit.sette.common.validator.GeneralValidator;
import hu.bme.mit.sette.common.validator.exceptions.ValidatorException;
import hu.bme.mit.sette.common.validator.reflection.ClassValidator;
import hu.bme.mit.sette.common.validator.reflection.MethodValidator;

import java.lang.reflect.Method;
import java.util.Comparator;

import org.apache.commons.lang3.Validate;

/**
 * Represents a snippet dependency (which is a Java class).
 */
public final class SnippetDependency implements
Comparable<SnippetDependency> {
    /** The Java class. */
    private final Class<?> javaClass;

    /**
     * Instantiates a new snippet dependency.
     *
     * @param pJavaClass
     *            the Java class
     * @param classLoader
     *            the class loader for loading snippet project classes
     * @throws ValidatorException
     *             if validation has failed
     */
    SnippetDependency(final Class<?> pJavaClass,
            final ClassLoader classLoader) throws ValidatorException {
        Validate.notNull(pJavaClass, "The Java class must not be null");
        javaClass = pJavaClass;

        // start validaton
        GeneralValidator validator = new GeneralValidator(
                SnippetDependency.class);

        // validate class
        // check: only @SetteDependency SETTE annotation
        AnnotationMap classAnns = SetteAnnotationUtils
                .getSetteAnnotations(pJavaClass);

        if (classAnns.size() != 1
                || classAnns.get(SetteDependency.class) == null) {
            ClassValidator v = new ClassValidator(pJavaClass);
            v.addException("The class must not only have the @"
                    + SetteDependency.class.getSimpleName()
                    + " SETTE annotation");
            validator.addChildIfInvalid(v);
        }

        // validate methods
        // check: no SETTE annotations
        for (Method method : pJavaClass.getDeclaredMethods()) {
            if (method.isSynthetic()) {
                // skip synthetic methods
                continue;
            }

            if (!SetteAnnotationUtils.getSetteAnnotations(method)
                    .isEmpty()) {
                MethodValidator v = new MethodValidator(method);
                v.addException("The method must not have "
                        + "any SETTE annotations");
                validator.addChildIfInvalid(v);
            }
        }

        validator.validate();
    }

    /**
     * Gets the Java class.
     *
     * @return the Java class
     */
    public Class<?> getJavaClass() {
        return javaClass;
    }

    /** The comparator for the class. */
    public static final Comparator<SnippetDependency> COMPARATOR;

    static {
        COMPARATOR = new Comparator<SnippetDependency>() {
            @Override
            public int compare(final SnippetDependency o1,
                    final SnippetDependency o2) {
                if (o1 == null) {
                    return -1;
                } else if (o2 == null) {
                    return 1;
                } else {
                    return o1.javaClass.getName().compareTo(
                            o2.javaClass.getName());
                }
            }
        };
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(final SnippetDependency o) {
        return SnippetDependency.COMPARATOR.compare(this, o);
    }
}
