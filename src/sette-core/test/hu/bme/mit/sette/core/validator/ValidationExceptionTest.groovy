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
 * Tests for {@link ValidationException}.
 */
@TypeChecked
class ValidationExceptionTest {
    @Test(expected = NullPointerException)
    void testThrowsExceptionifNullValidator() {
        new ValidationException((Validator) null)
    }

    @Test(expected = NullPointerException)
    void testThrowsExceptionifNullValidationContext() {
        new ValidationException((ValidationContext) null)
    }

    @Test(expected = IllegalArgumentException)
    void testThrowsExceptionifValidatorHasNoErrors() {
        new ValidationException(new Validator<>('subject'))
    }

    @Test(expected = IllegalArgumentException)
    void testThrowsExceptionifValidationContextHasNoErrors() {
        new ValidationException(new ValidationContext('context'))
    }

    @Test
    void testExceptionMessageForValidator() {
        Validator<String> v = new Validator<>('subject')
        v.addError('error1')
        v.addError('error2')

        ValidationException ex = new ValidationException(v)
        List<String> lines = ex.message.tokenize('\n')

        assert lines.size() == 4
        assert lines[0] == '2 errors occurred during validation'
        assert lines[1].startsWith('    Validator') && lines[1].contains('subject=subject')
        assert lines[2].startsWith('    ValidationError') && lines[2].contains('message=error1')
        assert lines[3].startsWith('    ValidationError') && lines[3].contains('message=error2')
    }

    @Test
    void testExceptionMessageForValidationContext() {
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

        ValidationException ex = new ValidationException(vc)
        List<String> lines = ex.message.tokenize('\n')

        assert lines.size() == 7
        assert lines[0] == '3 errors occurred during validation'
        assert lines[1].startsWith('    ValidationContext') && lines[1].contains('context=context')
        assert lines[2].startsWith('    Validator') && lines[2].contains('subject=a')
        assert lines[3].startsWith('        ValidationError') && lines[3].contains('message=error1')
        assert lines[4].startsWith('        ValidationError') && lines[4].contains('message=error2')
        assert lines[5].startsWith('    Validator') && lines[5].contains('subject=c')
        assert lines[6].startsWith('        ValidationError') && lines[6].contains('message=error3')
    }
}
