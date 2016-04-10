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
package hu.bme.mit.sette.core.validator

import groovy.transform.CompileStatic

import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.lang.reflect.Modifier

import org.junit.Test

/**
 * Tests for {@link ClassExecutableValidator}.
 */
@CompileStatic
class ClassExecutableValidatorTest {
    Constructor<?> constructor = Number.class.constructors[0]
    Method method = Integer.class.declaredMethods.find { it.name.contains('toUnsignedString0') }

    @Test(expected = NullPointerException)
    void testConstructorThrowsExceptionIfNull() {
        new ClassExecutableValidator(null)
    }

    @Test
    void testModifiers() {
        assert new ClassExecutableValidator(constructor).withModifiers(Modifier.PUBLIC).isValid()
        assert !new ClassExecutableValidator(constructor).withModifiers(Modifier.PRIVATE).isValid()
        assert !new ClassExecutableValidator(constructor).withoutModifiers(Modifier.PUBLIC).isValid()
        assert new ClassExecutableValidator(constructor).withoutModifiers(Modifier.PRIVATE).isValid()

        assert !new ClassExecutableValidator(method).withModifiers(Modifier.PUBLIC).isValid()
        assert new ClassExecutableValidator(method).withModifiers(Modifier.PRIVATE).isValid()
        assert new ClassExecutableValidator(method).withoutModifiers(Modifier.PUBLIC).isValid()
        assert !new ClassExecutableValidator(method).withoutModifiers(Modifier.PRIVATE).isValid()
    }

    @Test
    void testParameterCount() {
        assert new ClassExecutableValidator(constructor).parameterCount(0).isValid()
        assert !new ClassExecutableValidator(constructor).parameterCount(1).isValid()

        assert !new ClassExecutableValidator(method).parameterCount(0).isValid()
        assert !new ClassExecutableValidator(method).parameterCount(1).isValid()
        assert new ClassExecutableValidator(method).parameterCount(2).isValid()
    }

    @Test(expected = IllegalArgumentException)
    void testParameterCountThrowsExceptionIfNegative() {
        new ClassExecutableValidator(Number.class.constructors[0]).parameterCount(-1)
    }

    @Test
    void testReturnValues() {
        ClassExecutableValidator v = new ClassExecutableValidator(constructor)

        assert v.withModifiers(Modifier.PUBLIC).is(v)
        assert v.withModifiers(Modifier.PRIVATE).is(v)
        assert v.withoutModifiers(Modifier.PUBLIC).is(v)
        assert v.withoutModifiers(Modifier.PRIVATE).is(v)
        assert v.parameterCount(0).is(v)
        assert v.parameterCount(1).is(v)
    }
}
