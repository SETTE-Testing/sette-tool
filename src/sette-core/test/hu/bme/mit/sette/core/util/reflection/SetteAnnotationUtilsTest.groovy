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
package hu.bme.mit.sette.core.util.reflection

import groovy.transform.CompileStatic
import hu.bme.mit.sette.common.annotations.SetteIncludeCoverage
import hu.bme.mit.sette.common.annotations.SetteRequiredStatementCoverage
import hu.bme.mit.sette.common.annotations.SetteSnippetContainer

import java.lang.annotation.Annotation
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.reflect.AnnotatedElement

import org.junit.Test

import com.google.common.collect.ClassToInstanceMap

/**
 * Tests for {@link SetteAnnotationUtils}.
 */
@CompileStatic
class SetteAnnotationUtilsTest {
    @Test(expected = UnsupportedOperationException)
    void testStaticClass() {
        SetteAnnotationUtils.class.newInstance()
    }

    @Test(expected = NullPointerException)
    void testGetSetteAnnotations_throwsExceptionIfNull() {
        SetteAnnotationUtils.getSetteAnnotations(null)
    }

    @Test
    void testGetSetteAnnotations_findsAnnotationForClass() {
        ClassToInstanceMap<Annotation> annots = SetteAnnotationUtils.getSetteAnnotations(Container)

        assert annots.size() == 1
        assert annots.values()[0] == Container.annotations[1]
    }

    @Test
    void testGetSetteAnnotations_findsAnnotationsforMethod() {
        AnnotatedElement elem = Container.getMethod('snippet')
        ClassToInstanceMap<Annotation> annots = SetteAnnotationUtils.getSetteAnnotations(elem)
        List<Annotation> elemAnnots = elem.annotations as List
        elemAnnots.sort { Annotation annot -> annot.annotationType().simpleName }

        assert annots.values()[0] == elem.annotations[1] // @SetteIncludeCoverage
        assert annots.values()[1] == elem.annotations[2] // @SetteRequiredStatementCoverage
    }

    @Test
    void testGetSetteAnnotations_findsNoAnnotationforMethodWithoutSetteAnnotations() {
        AnnotatedElement elem = Container.getMethod('notSnippet')
        ClassToInstanceMap<Annotation> annots = SetteAnnotationUtils.getSetteAnnotations(elem)

        assert annots.isEmpty()
    }

    @Test
    void testIsSetteAnnotation() {
        // @OtherAnnotation
        assert !SetteAnnotationUtils.isSetteAnnotation(Container.annotations[0])

        // @SetteSnippetContainer
        assert SetteAnnotationUtils.isSetteAnnotation(Container.annotations[1])
    }

    @Test(expected = NullPointerException)
    void testIsSetteAnnotation_throwsExceptionIfNull() {
        SetteAnnotationUtils.isSetteAnnotation(null)
    }

    /**
     * Class with annotations to serve as test data for the test cases.
     */
    @OtherAnnotation
    @SetteSnippetContainer(category = "C", goal = "G", inputFactoryContainer = Void.class)
    private static final class Container {
        @OtherAnnotation
        @SetteRequiredStatementCoverage(value = 50d)
        @SetteIncludeCoverage(classes = [], methods = [])
        public void snippet() { }

        @OtherAnnotation
        public void notSnippet() { }
    }
}

@Retention(RetentionPolicy.RUNTIME)
@interface OtherAnnotation { }
