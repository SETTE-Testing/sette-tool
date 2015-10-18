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
package hu.bme.mit.sette.core.validator

import groovy.transform.TypeChecked

import java.lang.reflect.Modifier

import org.junit.Test

/**
 * Tests for {@link ClassValidator}.
 */
@TypeChecked
class ClassValidatorTest {
    @Test(expected = NullPointerException)
    void testConstructorThrowsExceptionIfNull() {
        new ClassValidator(null)
    }

    @Test
    void testIsRegular() {
        assert new ClassValidator(getClass()).isRegular().isValid()
        assert !new ClassValidator(int).isRegular().isValid() // primitive
        assert !new ClassValidator(int[]).isRegular().isValid() // array
        assert !new ClassValidator(Thread.State).isRegular().isValid() // enum
        assert !new ClassValidator(Test).isRegular().isValid() // annotation
        assert !new ClassValidator(Serializable).isRegular().isValid() // interface
    }

    @Test
    void testModifiers() {
        // Number: public abstract

        assert new ClassValidator(getClass()).withModifiers(Modifier.PUBLIC).isValid()
        assert !new ClassValidator(getClass()).withModifiers(Modifier.PRIVATE).isValid()
        assert new ClassValidator(Number).withModifiers(Modifier.PUBLIC | Modifier.ABSTRACT).isValid()
        assert !new ClassValidator(Number).withModifiers(Modifier.FINAL | Modifier.ABSTRACT).isValid()
        assert !new ClassValidator(Number).withModifiers(Modifier.FINAL | Modifier.PRIVATE).isValid()

        assert !new ClassValidator(getClass()).withoutModifiers(Modifier.PUBLIC).isValid()
        assert new ClassValidator(getClass()).withoutModifiers(Modifier.PRIVATE).isValid()
        assert !new ClassValidator(Number).withoutModifiers(Modifier.PUBLIC | Modifier.ABSTRACT).isValid()
        assert !new ClassValidator(Number).withoutModifiers(Modifier.FINAL | Modifier.ABSTRACT).isValid()
        assert new ClassValidator(Number).withoutModifiers(Modifier.FINAL | Modifier.PRIVATE).isValid()
    }

    @Test
    void testSuperclass() {
        assert new ClassValidator(getClass()).superclass(Object).isValid()
        assert !new ClassValidator(getClass()).superclass(Number).isValid()

        assert !new ClassValidator(Integer).superclass(getClass()).isValid()
        assert !new ClassValidator(Integer).superclass(Object).isValid()
        assert new ClassValidator(Integer).superclass(Number).isValid()
    }

    @Test(expected = NullPointerException)
    void testSuperclassThrowsExceptionIfNull() {
        new ClassValidator(getClass()).superclass(null)
    }

    @Test
    void testInterfaceCount() {
        assert new ClassValidator(ClassValidator).interfaceCount(0).isValid()
        assert !new ClassValidator(ClassValidator).interfaceCount(1).isValid()

        assert !new ClassValidator(Number).interfaceCount(0).isValid()
        assert new ClassValidator(Number).interfaceCount(1).isValid()

        assert !new ClassValidator(ArrayList).interfaceCount(0).isValid()
        assert !new ClassValidator(ArrayList).interfaceCount(1).isValid()
        assert new ClassValidator(ArrayList).interfaceCount(4).isValid()
    }

    @Test(expected = IllegalArgumentException)
    void testInterfaceCountThrowsExceptionIfNegative() {
        new ClassValidator(getClass()).interfaceCount(-1)
    }

    @Test
    void testDeclaredConstructorCount() {
        assert !new ClassValidator(ClassValidator).declaredConstructorCount(0).isValid()
        assert new ClassValidator(ClassValidator).declaredConstructorCount(1).isValid()

        assert !new ClassValidator(ArrayList).declaredConstructorCount(0).isValid()
        assert !new ClassValidator(ArrayList).declaredConstructorCount(1).isValid()
        assert new ClassValidator(ArrayList).declaredConstructorCount(3).isValid()
    }

    @Test(expected = IllegalArgumentException)
    void testDeclaredConstructorCountThrowsExceptionIfNegative() {
        new ClassValidator(getClass()).declaredConstructorCount(-1)
    }

    @Test
    void testReturnValues() {
        ClassValidator v = new ClassValidator(getClass())

        assert v.isRegular().is(v)
        assert v.withModifiers(Modifier.PUBLIC).is(v)
        assert v.withModifiers(Modifier.PRIVATE).is(v)
        assert v.withoutModifiers(Modifier.PUBLIC).is(v)
        assert v.withoutModifiers(Modifier.PRIVATE).is(v)
        assert v.superclass(Object).is(v)
        assert v.superclass(Number).is(v)
        assert v.interfaceCount(0).is(v)
        assert v.interfaceCount(1).is(v)
        assert v.declaredConstructorCount(0).is(v)
        assert v.declaredConstructorCount(1).is(v)
    }
}
