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

import java.lang.reflect.Method;

import hu.bme.mit.sette.common.annotations.SetteDependency;
import hu.bme.mit.sette.core.util.reflection.ClassComparator;
import hu.bme.mit.sette.core.util.reflection.SetteAnnotationUtils;
import hu.bme.mit.sette.core.validator.ClassExecutableValidator;
import hu.bme.mit.sette.core.validator.ClassValidator;
import hu.bme.mit.sette.core.validator.ValidationContext;
import hu.bme.mit.sette.core.validator.ValidationException;
import lombok.Getter;
import lombok.NonNull;
import lombok.val;

/**
 * Represents a snippet dependency (which is a Java class).
 */
public final class SnippetDependency implements Comparable<SnippetDependency> {
    /** The snippet project to which the instance belongs to. */
    @Getter
    private final SnippetProject snippetProject;

    /** The Java {@link Class} of the snippet dependency. */
    @Getter
    private final Class<?> javaClass;

    /**
     * Instantiates a new snippet dependency.
     *
     * @param snippetProject
     *            the snippet project to which the instance belongs to
     * @param javaClass
     *            the Java {@link Class}
     * @throws ValidationException
     *             if validation fails
     */
    SnippetDependency(@NonNull SnippetProject snippetProject, @NonNull Class<?> javaClass)
            throws ValidationException {
        this.snippetProject = snippetProject;
        this.javaClass = javaClass;

        // Validation: no @SetteXxx annotations except @SnippetDependency
        ValidationContext vc = new ValidationContext("SnippetDependency: " + javaClass.getName());

        // validate class
        // check: only @SetteDependency SETTE annotation
        val classAnns = SetteAnnotationUtils.getSetteAnnotations(javaClass);

        if (classAnns.size() != 1 || classAnns.get(SetteDependency.class) == null) {
            ClassValidator v = new ClassValidator(javaClass);
            v.addError("The class must not only have the @" + SetteDependency.class.getSimpleName()
                    + " SETTE annotation");
            vc.addValidator(v);
        }

        // validate methods
        // check: no SETTE annotations
        for (Method method : javaClass.getDeclaredMethods()) {
            if (method.isSynthetic()) {
                // skip synthetic methods
                continue;
            }

            if (!SetteAnnotationUtils.getSetteAnnotations(method).isEmpty()) {
                ClassExecutableValidator v = new ClassExecutableValidator(method);
                v.addError("The method must not have any SETTE annotations");
                vc.addValidator(v);
            }
        }

        vc.validate();
    }

    @Override
    public int compareTo(@NonNull SnippetDependency o) {
        return ClassComparator.INSTANCE.compare(javaClass, o.javaClass);
    }
}
