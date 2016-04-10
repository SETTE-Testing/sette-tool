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

import org.junit.Before
import org.junit.Test

/**
 * Tests for {@link Validator}.
 */
@CompileStatic
class ValidatorTest {
    @Test(expected = NullPointerException)
    void testConstructorThrowsExceptionIfNull() {
        Validator.of(null)
    }

    @Test
    void testSubjectIsSavedByReference() {
        String subject = 'subject'
        Validator<String> v = Validator.of(subject)
        assert v.subject.is(subject)
    }

    @Test
    void testScenarioWithoutAddErrorCalls() {
        Validator<String> v = new Validator<>('subject')

        assert v.totalErrorCount == 0
        assert v.errors.collect().isEmpty()
        assert v.isValid()
        assert v.toString() == '[V] subject: 0 error(s)'
        v.validate()
    }

    @Test
    void testScenarioWithDirectlyAddedErrors() {
        Validator<String> v = new Validator<>('subject')
        v.addError('error1')
        v.addError('error2')

        List<ValidationError> errors = v.errors.collect().toList()
        assert v.totalErrorCount == 2
        assert errors.size() == 2
        assert errors[0].message == 'error1'
        assert errors[1].message == 'error2'

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
            // 2 lines for the validator, 2 errors, 1 (msg) + 5 (stacktrace) lines per error
            assert ex.message.tokenize('\n').size() == 2 + 2*6
        }
    }

    @Test
    void testScenarioWithConditionalErrors() {
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

        List<ValidationError> errors = v.errors.collect().toList()
        assert v.totalErrorCount == 6
        assert errors.size() == 6

        assert errors[0].message == 'error1'
        assert errors[1].message == 'error4'
        assert errors[2].message == 'error5'
        assert errors[3].message == 'error8'
        assert errors[4].message == 'error9'
        assert errors[5].message == 'prop: expected a instead of b'

        String search = "${getClass().name}.test"
        errors.each { ValidationError e ->
            assert e.message && e.stackTrace.find { it.toString().startsWith(search) }
        }

        assert errors[0].stackTrace[0].toString().startsWith("${Validator.class.name}.addErrorIfTrue(")
        assert errors[1].stackTrace[0].toString().startsWith("${Validator.class.name}.addErrorIfFalse(")
        assert errors[2].stackTrace[0].toString().startsWith("${Validator.class.name}.addErrorIfTrue(")
        assert errors[3].stackTrace[0].toString().startsWith("${Validator.class.name}.addErrorIfFalse(")
        assert errors[4].stackTrace[0].toString().startsWith("${Validator.class.name}.addError(")
        assert errors[5].stackTrace[0].toString().startsWith("${Validator.class.name}.addErrorIfNotEquals(")

        assert !v.isValid()

        // try-catch is needed to verify that exception is coming from this call
        try {
            v.validate()
            assert false : 'Expected exception was not thrown'
        } catch (ValidationException ex) {
            // 2 lines for the validator, 6 errors, 1 (msg) + 5 (stacktrace) lines per error
            assert ex.message.tokenize('\n').size() == 2 + 6*6
        }
    }

    @Test
    void testScenarioWithConditionalButUnsatisfiedErrors() {
        Validator<String> v = new Validator<>('subject')

        v.addErrorIfTrue('error2', { String s -> s.startsWith('a') })
        v.addErrorIfFalse('error3', { String s -> s.startsWith('s') })

        assert v.totalErrorCount == 0
        assert v.errors.collect().isEmpty()
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

        assert v.totalErrorCount == 0
        assert v.errors.collect().isEmpty()
        assert v.isValid()
        v.validate()
    }

    static class HierarchyTests {
        Validator<String> root, v1, v11, v12, v2, v21, v211, v212, v22

        @Before
        void setUp() {
            root = createValidator('root', 'error1', 'error2')

            v1 = createValidator('child-1', 'error1')
            v11 = createValidator('child-1.1', 'error1', 'error2')
            v12 = createValidator('child-1-1', 'error1')

            v2 = createValidator('child-2', 'error1')
            v21 = createValidator('child-2-1', 'error1', 'error2')
            v211 = createValidator('child-2-1-1', 'error1', 'error2')
            v212 = createValidator('child-2-1-2', 'error1')
            v22 = createValidator('child-2-2', 'error1')
        }

        private static Validator<String> createValidator(String subject, String... errors) {
            Validator<String> v = Validator.of(subject)
            errors?.each { String e -> v.addError(e) }
            return v
        }

        /**
         * Cretes the following hierarchy:
         * <pre>
         * <code>
         * root:
         *   v1: v11, v12
         *   v2:
         *     v21: v211, v211
         *     v22
         * </code>
         * </pre>
         */
        private createFullHierarchy() {
            root.addChild(v1)
            v1.addChild(v11)
            v1.addChild(v12)
            root.addChild(v2)
            v2.addChild(v21)
            v21.addChild(v211)
            v21.addChild(v212)
            v2.addChild(v22)
        }

        private void checkValidator(Validator<?> validator, int totalErrorCount,Validator<?> parent,
                Collection<Validator<?>> children, Collection<Validator<?>> allChildren) {
            assert validator.totalErrorCount == totalErrorCount
            assert validator.parent == parent
            assert validator.root == root

            assert validator.children.collect().toSet() == children.toSet()
            assert validator.allChildren.collect().toSet() == allChildren.toSet()
        }

        @Test
        void testOnlyRoot() {
            checkValidator(root, 2, null, [], [])

            assert root.toString() == '''
[V] root: 2 error(s)
    [E] error1
    [E] error2'''.trim()
        }

        @Test
        void testTwoLevelHierarchy() {
            root.addChild(v1)
            root.addChild(v2)

            Validator<String> v3 = Validator.of('child3 - no errors')
            root.addChild(v3)

            checkValidator(root, 4, null, [v1, v2, v3], [v1, v2, v3])
            checkValidator(v1, 1, root, [], [])
            checkValidator(v2, 1, root, [], [])
            checkValidator(v3, 0, root, [], [])

            assert root.toString() == '''
[V] root: 4 error(s)
    [E] error1
    [E] error2
    [V] child-1: 1 error(s)
        [E] error1
    [V] child-2: 1 error(s)
        [E] error1'''.trim()
        }

        @Test
        void testMultiLevelHierarchy() {
            createFullHierarchy()

            checkValidator(root, 13, null, [v1, v2], [v1, v11, v12, v2, v21, v211, v212, v22])

            checkValidator(v1, 4, root, [v11, v12], [v11, v12])
            checkValidator(v11, 2, v1, [], [])
            checkValidator(v12, 1, v1, [], [])

            checkValidator(v2, 7, root, [v21, v22], [v21, v211, v212, v22])
            checkValidator(v21, 5, v2, [v211, v212], [v211, v212])
            checkValidator(v211, 2, v21, [], [])
            checkValidator(v212, 1, v21, [], [])
            checkValidator(v22, 1, v2, [], [])

            assert v2.toString() == '''
[V] child-2: 7 error(s)
    [E] error1
    [V] child-2-1: 5 error(s)
        [E] error1
        [E] error2
        [V] child-2-1-1: 2 error(s)
            [E] error1
            [E] error2
        [V] child-2-1-2: 1 error(s)
            [E] error1
    [V] child-2-2: 1 error(s)
        [E] error1
    [V] child-2-1-1: 2 error(s)
        [E] error1
        [E] error2
    [V] child-2-1-2: 1 error(s)
        [E] error1
'''.trim()

            assert root.toString() == '''
[V] root: 13 error(s)
    [E] error1
    [E] error2
    [V] child-1: 4 error(s)
        [E] error1
        [V] child-1.1: 2 error(s)
            [E] error1
            [E] error2
        [V] child-1-1: 1 error(s)
            [E] error1
    [V] child-2: 7 error(s)
        [E] error1
        [V] child-2-1: 5 error(s)
            [E] error1
            [E] error2
            [V] child-2-1-1: 2 error(s)
                [E] error1
                [E] error2
            [V] child-2-1-2: 1 error(s)
                [E] error1
        [V] child-2-2: 1 error(s)
            [E] error1
        [V] child-2-1-1: 2 error(s)
            [E] error1
            [E] error2
        [V] child-2-1-2: 1 error(s)
            [E] error1
    [V] child-1.1: 2 error(s)
        [E] error1
        [E] error2
    [V] child-1-1: 1 error(s)
        [E] error1
    [V] child-2-1: 5 error(s)
        [E] error1
        [E] error2
        [V] child-2-1-1: 2 error(s)
            [E] error1
            [E] error2
        [V] child-2-1-2: 1 error(s)
            [E] error1
    [V] child-2-2: 1 error(s)
        [E] error1
    [V] child-2-1-1: 2 error(s)
        [E] error1
        [E] error2
    [V] child-2-1-2: 1 error(s)
        [E] error1
'''.trim()
        }

        @Test
        void testAddChildThrowsExceptionIfSameValidator() {
            createFullHierarchy()

            try {
                root.addChild(root)
                assert false : 'no exception was thrown'
            } catch (IllegalArgumentException ex) {
                // expected
            }

            root.toString() // trigger tree walk
        }

        @Test
        void testAddChildThrowsExceptionIfAlreadyAdded() {
            createFullHierarchy()

            try {
                root.addChild(v1)
                assert false : 'no exception was thrown'
            } catch (IllegalArgumentException ex) {
                // expected
            }

            root.toString() // trigger tree walk
        }


        @Test
        void testAddChildThrowsExceptionIfTransitiveChild() {
            createFullHierarchy()

            try {
                root.addChild(v11)
                assert false : 'no exception was thrown'
            } catch (IllegalArgumentException ex) {
                // expected
            }

            root.toString() // trigger tree walk
        }

        @Test
        void testAddChildThrowsExceptionIfTransitiveSibling() {
            createFullHierarchy()

            try {
                v11.addChild(v22)
                assert false : 'no exception was thrown'
            } catch (IllegalArgumentException ex) {
                // expected
            }

            root.toString() // trigger tree walk
        }

        @Test
        void testAddChildThrowsExceptionIfRoot() {
            createFullHierarchy()

            try {
                v212.addChild(root)
                assert false : 'no exception was thrown'
            } catch (IllegalArgumentException ex) {
                // expected
            }

            root.toString() // trigger tree walk
        }

        @Test
        void testAddChildThrowsExceptionIfTransitiveParent() {
            createFullHierarchy()

            try {
                v212.addChild(v2)
                assert false : 'no exception was thrown'
            } catch (IllegalArgumentException ex) {
                // expected
            }

            root.toString() // trigger tree walk
        }

        @Test
        void testOnlyChildHasError() {
            root.@errors.clear()
            root.addChild(v1)

            checkValidator(root, 1, null, [v1], [v1])

            assert !root.isValid()
            assert root.toString() == '''
[V] root: 1 error(s)
    [V] child-1: 1 error(s)
        [E] error1
'''.trim()
        }
    }
}
