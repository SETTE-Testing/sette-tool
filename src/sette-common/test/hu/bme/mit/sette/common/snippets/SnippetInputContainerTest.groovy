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

import org.junit.Test

import groovy.transform.TypeChecked

/**
 * Tests for {@link SnippetInputContainer}. The {@link TestWithValidData} class contains
 * parameterized tests with valid data for the class.
 */
@TypeChecked
class SnippetInputContainerTest {
    @Test
    void testConstructor() {
        def sic = new SnippetInputContainer(2)
        assert 2 == sic.parameterCount
        assert 0 == sic.size()
        assert []== sic.toArray()
    }

    @Test(expected = IllegalArgumentException)
    void testConstructorThrowsExceptionIfParameterCountNegative() {
        new SnippetInputContainer(-1)
    }

    @Test
    void testGetAndAdd() {
        def sic = new SnippetInputContainer(3)

        sic.add(new SnippetInput(null, 1, 2, 'a'))
        sic.addByParameters(3, 4, 'b')
        sic.addByExpectedAndParameters(IllegalArgumentException, 5, 6, 'c')
        sic.addByExpectedAndParameters(null, 7, 8, 'd')

        assert 3 == sic.parameterCount
        assert 4 == sic.size()
        assert [sic.get(0), sic.get(1), sic.get(2), sic.get(3)]== sic.toArray()

        sic.get(0).with { assert [1, 2, 'a']== parameters && null == expected }
        sic.get(1).with { assert [3, 4, 'b']== parameters && null == expected }
        sic.get(2).with { assert [5, 6, 'c']== parameters && IllegalArgumentException == expected }
        sic.get(3).with { assert [7, 8, 'd']== parameters && null == expected }
    }

    @Test(expected = IndexOutOfBoundsException)
    void testGetThrowsExceptionIfIndexIsTooSmall() {
        def sic = new SnippetInputContainer(2)
        sic.addByParameters(1, 2).addByParameters(3, 4)
        sic.get(-1)
    }

    @Test(expected = IndexOutOfBoundsException)
    void testGetThrowsExceptionIfIndexIsTooBig() {
        def sic = new SnippetInputContainer(2)
        sic.addByParameters(1, 2).addByParameters(3, 4)
        sic.get(3)
    }

    @Test(expected = IllegalArgumentException)
    void testAddThrowsExceptionIfInputIsNull() {
        new SnippetInputContainer(1).add(null)
    }

    @Test(expected = IllegalArgumentException)
    void testAddThrowsExceptionIfInputParameterCountIsLess() {
        new SnippetInputContainer(1).addByParameters()
    }

    @Test(expected = IllegalArgumentException)
    void testAddThrowsExceptionIfInputParameterCountIsMore() {
        new SnippetInputContainer(1).addByParameters(1, 2)
    }
}
