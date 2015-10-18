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

import org.junit.Test

import groovy.transform.TypeChecked;

/**
 * Tests for {@link Validator}.
 */
@TypeChecked
class ValidatorTest {
    @Test(expected = NullPointerException)
    void testConstructorThrowsExceptionIfNull() {
        new Validator<>(null)
    }

    @Test(expected = UnsupportedOperationException)
    void testGetErrorReturnsWithImmutable() {
        new Validator<>('subject').errors.add(null)
    }

    @Test
    void testSubjectIsSavedByReference() {
        String subject = 'subject'
        Validator<String> v = new Validator<>(subject)
        assert v.subject.is(subject)
    }

    @Test
    void testScenarioWithoutAddErrorCalls() {
        Validator<String> v = new Validator<>('subject')

        assert v.errorCount == 0
        assert v.errors.isEmpty()
        assert v.isValid()
        v.validate()
    }

    @Test
    void testScenarioWithDirectlyAddedErrors() {
        Validator<String> v = new Validator<>('subject')
        v.addError('error1')
        v.addError('error2')

        assert v.errorCount == 2
        assert v.errors.size() == 2
        assert v.errors[0].message == 'error1'
        assert v.errors[1].message == 'error2'

        String search = "${getClass().name}.test"
        v.errors.each { ValidationError e ->
            assert e.message && e.stackTrace[0].toString().startsWith("${Validator.class.name}.addError(")
            assert e.message && e.stackTrace.find {
                it.toString().startsWith(search)
            }
        }

        assert !v.isValid()

        // try-catch is needed to verify that exception is coming from this call
        try {
            v.validate()
            assert false : 'Expected exception was not thrown'
        } catch (ValidationException ex) {
            assert ex.message.tokenize('\n').size() == 4
        }
    }

    @Test
    void testScenarionWithConditionalErrors() {
        Validator<String> v = new Validator<>('subject')

        v.addErrorIfTrue('error1', true)
        v.addErrorIfTrue('error2', false)
        v.addErrorIfFalse('error3', true)
        v.addErrorIfFalse('error4', false)
        v.addErrorIfTrue('error5', { String s -> s.startsWith('s') })
        v.addErrorIfTrue('error6', { String s -> s.startsWith('a') })
        v.addErrorIfFalse('error7', { String s -> s.startsWith('s') })
        v.addErrorIfFalse('error8', { String s -> s.startsWith('a') })
        v.addError('error9')
        v.addErrorIfNotEquals('prop', 'a', 'a')
        v.addErrorIfNotEquals('prop', 'a', 'b')

        assert v.errorCount == 6
        assert v.errors.size() == 6
        assert v.errors[0].message == 'error1'
        assert v.errors[1].message == 'error4'
        assert v.errors[2].message == 'error5'
        assert v.errors[3].message == 'error8'
        assert v.errors[4].message == 'error9'
        assert v.errors[5].message == 'prop: expected a instead of b'

        String search = "${getClass().name}.test"
        v.errors.each { ValidationError e ->
            assert e.message && e.stackTrace.find { it.toString().startsWith(search) }
        }
        assert v.errors[0].stackTrace[0].toString().startsWith("${Validator.class.name}.addErrorIfTrue(")
        assert v.errors[1].stackTrace[0].toString().startsWith("${Validator.class.name}.addErrorIfFalse(")
        assert v.errors[2].stackTrace[0].toString().startsWith("${Validator.class.name}.addErrorIfTrue(")
        assert v.errors[3].stackTrace[0].toString().startsWith("${Validator.class.name}.addErrorIfFalse(")
        assert v.errors[4].stackTrace[0].toString().startsWith("${Validator.class.name}.addError(")
        assert v.errors[5].stackTrace[0].toString().startsWith("${Validator.class.name}.addErrorIfNotEquals(")

        assert !v.isValid()

        // try-catch is needed to verify that exception is coming from this call
        try {
            v.validate()
            assert false : 'Expected exception was not thrown'
        } catch (ValidationException ex) {
            assert ex.message.tokenize('\n').size() == 8
        }
    }

    @Test
    void testScenarioWithConditionalButUnsatisfiedErrors() {
        Validator<String> v = new Validator<>('subject')

        v.addErrorIfTrue('error2', { String s -> s.startsWith('a') })
        v.addErrorIfFalse('error3', { String s -> s.startsWith('s') })

        assert v.errorCount == 0
        assert v.errors.isEmpty()
        assert v.isValid()
        v.validate()
    }

    @Test
    void testAddErrorMethodsThrowExceptionIfNull() {
        // one test is shorter and easier to maintain
        Validator<String> v = new Validator<>('subject')

        Closure pred = { String s -> s.startsWith('s') }
        List<Closure> cases = [
            { v.addError(null) },
            { v.addErrorIfTrue(null, true) },
            { v.addErrorIfFalse(null, false) },
            { v.addErrorIfTrue(null, pred) },
            { v.addErrorIfTrue('error', null) },
            { v.addErrorIfFalse(null, pred) },
            { v.addErrorIfFalse('error', null) }
        ]

        cases.eachWithIndex { Closure c, int i ->
            try {
                c()
                assert false : "Expected exception was not thrown for $i"
            } catch (NullPointerException ex) {
                // expected
            }
        }

        assert v.errorCount == 0
        assert v.errors.isEmpty()
        assert v.isValid()
        v.validate()
    }
}
