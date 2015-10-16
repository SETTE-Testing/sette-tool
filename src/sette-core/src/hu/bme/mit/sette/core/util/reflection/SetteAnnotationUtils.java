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
package hu.bme.mit.sette.core.util.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;

import hu.bme.mit.sette.common.annotations.SetteAnnotation;
import lombok.NonNull;

/**
 * Contains static helper methods for SETTE annotation manipulation.
 */
public final class SetteAnnotationUtils {
    /** Static class. */
    private SetteAnnotationUtils() {
        throw new UnsupportedOperationException("Static class");
    }

    /**
     * Gets the map of SETTE annotations.
     *
     * @param element
     *            an annotated element
     * @return a map of SETTE annotations
     */
    public static ClassToInstanceMap<Annotation> getSetteAnnotations(
            @NonNull AnnotatedElement element) {
        ClassToInstanceMap<Annotation> setteAnnots = MutableClassToInstanceMap.create();

        Arrays.stream(element.getAnnotations())
                .filter(SetteAnnotationUtils::isSetteAnnotation)
                .forEach(a -> setteAnnots.put(a.annotationType(), a));

        return setteAnnots;
    }

    /**
     * Decides whether the {@link Annotation} is a valid SETTE annotation (has the
     * {@link SetteAnnotation} on it).
     * 
     * @param annotation
     *            an annotation
     * @return <code>true</code> if the parameter is a SETTE annotation, otherwise
     *         <code>false</code>
     */
    public static boolean isSetteAnnotation(@NonNull Annotation annotation) {
        return annotation.annotationType().getAnnotation(SetteAnnotation.class) != null;
    }
}
