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

import java.lang.reflect.Field
import java.lang.reflect.Modifier

import org.junit.Test

/**
 * Tests for {@link ClassFieldValidator}.
 */
@TypeChecked
class ClassFieldValidatorTest {
    Field field = Integer.class.declaredFields.find { it.name.contains('value') }

    @Test(expected = NullPointerException)
    void testConstructorThrowsExceptionIfNull() {
        new ClassFieldValidator(null)
    }

    @Test
    void testModifiers() {
        assert !new ClassFieldValidator(field).withModifiers(Modifier.PUBLIC).isValid()
        assert new ClassFieldValidator(field).withModifiers(Modifier.PRIVATE).isValid()
        assert new ClassFieldValidator(field).withoutModifiers(Modifier.PUBLIC).isValid()
        assert !new ClassFieldValidator(field).withoutModifiers(Modifier.PRIVATE).isValid()
    }

    @Test
    void testReturnValues() {
        ClassFieldValidator v = new ClassFieldValidator(field)

        assert v.withModifiers(Modifier.PUBLIC).is(v)
        assert v.withModifiers(Modifier.PRIVATE).is(v)
        assert v.withoutModifiers(Modifier.PUBLIC).is(v)
        assert v.withoutModifiers(Modifier.PRIVATE).is(v)
    }
}
