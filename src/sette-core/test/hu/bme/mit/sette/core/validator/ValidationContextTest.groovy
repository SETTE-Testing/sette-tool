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

import org.junit.Test

import groovy.transform.TypeChecked;

/**
 * Tests for {@link ValidationContext}.
 */
@TypeChecked
class ValidationContextTest {
    @Test(expected = NullPointerException)
    void testConstructorThrowsExceptionIfNull() {
        new ValidationContext(null)
    }

    @Test
    void testContextIsSavedByReference() {
        String context = 'context'
        ValidationContext vc = new ValidationContext(context)
        assert vc.context.is(context)
    }

    @Test
    void testNoValidators() {
        String context = 'context'
        ValidationContext vc = new ValidationContext(context)

        assert vc.validators.isEmpty()
        assert vc.errorCount == 0
        assert vc.isValid()
        vc.validate()
    }

    @Test
    void testValidatorsWithoutError() {
        String context = 'context'
        ValidationContext vc = new ValidationContext(context)

        vc.addValidator(new Validator<String>('a'))
        vc.addValidator(new Validator<String>('b'))
        vc.addValidator(new Validator<String>('c'))

        assert vc.validators.size() == 3
        assert vc.validators*.subject == ['a', 'b', 'c']
        assert vc.errorCount == 0
        assert vc.isValid()
        vc.validate()
    }

    @Test
    void testValidatorsWithErrors() {
        String context = 'context'
        ValidationContext vc = new ValidationContext(context)

        Validator<String> v1 = new Validator<>('a')
        Validator<String> v2 = new Validator<>('b')
        Validator<String> v3 = new Validator<>('c')

        vc.addValidator(v1)
        vc.addValidator(v2)
        vc.addValidator(v3)

        v1.addError('error1')
        v1.addError('error2')
        v3.addError('error3')

        assert vc.validators == [v1, v2, v3]
        assert vc.errorCount == 3
        assert !vc.isValid()

        // try-catch is needed to verify that exception is coming from this call
        try {
            vc.validate()
            assert false : 'Expected exception was not thrown'
        } catch (ValidationException ex) {
            assert ex.message.tokenize('\n').size() == 7
        }
    }

    @Test
    void testAddValidatorPreventsDuplicates() {
        String context = 'context'
        ValidationContext vc = new ValidationContext(context)

        // same subject can be in several validators
        String subject = 'a'
        Validator<String> v1 = new Validator<>(subject)
        Validator<String> v2 = new Validator<>(subject)

        vc.addValidator(v1)
        vc.addValidator(v2)
        vc.addValidator(v1)

        assert vc.validators == [v1, v2]
        assert vc.errorCount == 0
        assert vc.isValid()
        vc.validate()
    }
}
