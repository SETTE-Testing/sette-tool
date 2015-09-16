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
package hu.bme.mit.sette.common.snippets

import groovy.transform.TypeChecked

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

/**
 * Tests for {@link SnippetInput}. The {@link TestWithValidData} class contains parameterized tests
 * with valid data for the class.
 */
@TypeChecked
public class SnippetInputTest {
    @Test
    public void testConstructorNullObjectArrayAsParameters() {
        def si = new SnippetInput(null, (Object[]) null)

        assert 0 == si.parameterCount
        assert []== si.parameters
        assert null == si.expected
    }

    @Test(expected = IndexOutOfBoundsException)
    public void testGetParameterThrowsExceptionIfIndexIsTooSmall() {
        def si = new SnippetInput(null, 1, '1', false)
        si.getParameter(-1)
    }

    @Test(expected = IndexOutOfBoundsException)
    public void testGetParameterThrowsExceptionIfIndexIsTooBig() {
        def si = new SnippetInput(null, 1, '1', false)
        si.getParameter(3)
    }

    @RunWith(Parameterized)
    public static class TestWithValidData {
        private Class<? extends Throwable> expected
        private Object[] parameters

        public TestWithValidData(Class<? extends Throwable> expected, Object[] parameters) {
            this.expected = expected
            this.parameters = parameters
        }

        @Test
        public void test() {
            def si = new SnippetInput(expected, parameters)

            assert parameters.length == si.parameterCount
            assert parameters == si.parameters
            assert expected == si.expected

            parameters.eachWithIndex { Object param, int idx ->
                assert param == si.getParameter(idx)
            }
        }

        @Parameters
        public static def data() {
            List<List<?>> values = []
            values << [null, []]
            values << [null, [1]]
            values << [null, [1, '1']]
            values << [null, [1, '1', false]]
            values << [IllegalArgumentException, []]
            values << [IllegalArgumentException, [1]]
            values << [IllegalArgumentException, [1, '1']]
            values << [IllegalArgumentException, [1, '1', true]]

            return values.collect { List<?> it ->
                it[1] = it[1] as Object[]
                return it as Object[]
            }
        }
    }
}
