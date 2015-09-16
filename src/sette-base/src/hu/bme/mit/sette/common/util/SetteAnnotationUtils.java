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
package hu.bme.mit.sette.common.util;

import hu.bme.mit.sette.annotations.SetteAnnotation;
import hu.bme.mit.sette.common.util.reflection.AnnotationMap;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

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
     * @param annotatedElement
     *            an annotated element
     * @return the map of SETTE annotations
     */
    public static AnnotationMap getSetteAnnotations(AnnotatedElement annotatedElement) {
        if (annotatedElement == null) {
            return new AnnotationMap();
        }

        return SetteAnnotationUtils.getSetteAnnotations(annotatedElement.getAnnotations());
    }

    /**
     * Gets the map of SETTE annotations.
     *
     * @param annotations
     *            an array of annotations
     * @return the map of SETTE annotations
     */
    public static AnnotationMap getSetteAnnotations(Annotation[] annotations) {
        if (annotations == null) {
            return new AnnotationMap();
        }

        AnnotationMap ret = new AnnotationMap();

        for (int i = 0; i < annotations.length; i++) {
            Annotation annotation = annotations[i];

            if (annotation != null
                    && annotation.annotationType().getAnnotation(SetteAnnotation.class) != null) {
                ret.put(annotation.annotationType(), annotation);
            }

        }

        return ret;
    }
}
