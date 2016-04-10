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
package hu.bme.mit.sette.tools.randoop

import groovy.transform.CompileStatic

import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters

@CompileStatic
class RandoopParserTest {
    @RunWith(Parameterized)
    static class GetGeneratedInputCountFromOutputLinesTests {
        @Rule
        public ExpectedException thrown = ExpectedException.none()

        @Parameter(0)
        public Object expectedResult
        @Parameter(1)
        public List<String> outLines

        @Test
        void test() {
            if (expectedResult instanceof Class) {
                thrown.expect(expectedResult)
            }

            RandoopParser.with {
                assert expectedResult == getGeneratedInputCountFromOutputLines(outLines)
            }
        }

        @Parameters(name = '{index}: {0} {1}')
        public static Collection<Object[]> data() {
            List<List> data = []

            // no "Writing tests" line
            data << [-1, '']
            data << [-1, 'nothing', 'special']
            data << [-1, 'this does not count Writing 1 junit tests']

            // one "Writing tests" line
            data << [0, 'x', 'Writing 0 junit tests', 'y']
            data << [1, 'Writing 1 junit tests']
            data << [1, 'x', 'Writing 1 junit tests', 'y']
            data << [123, 'x', 'Writing 123 junit tests', 'y']

            // several "Writing tests" lines
            data << [1, 'x', 'Writing 0 junit tests', 'Writing 1 junit tests', 'y']
            data << [1, 'x', 'Writing 0 junit tests', 'z', 'Writing 1 junit tests', 'y']
            data << [4, 'Writing 1 junit tests', 'Writing 3 junit tests']
            data << [4, 'Writing 1 junit tests', 'Writing 3 junit tests', 'Writing 0 junit tests']
            data << [14, 'Writing 1 junit tests', 'Writing 3 junit tests', 'Writing 10 junit tests']

            // negative numbers in output are not allowed
            data << [RuntimeException, 'x', 'Writing -1 junit tests', 'y']

            // convert for JUnit
            return data.collect { List it -> [it[0], it[1..-1]] as Object[] }
        }
    }

    @Test
    void testDetermineGeneratedTestCount_noInput() {
        RandoopParser.with {
            assert getGeneratedInputCountFromOutputLines(['nothing', 'special']) == 0
        }
    }
}
