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
package hu.bme.mit.sette.core.util.reflection

import groovy.transform.CompileStatic

import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters

/**
 * Tests for {@link ClassComparator}.
 */
@CompileStatic
@RunWith(Parameterized)
class ClassComparatorTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none()

    @Parameter(0)
    public Class o1

    @Parameter(1)
    public Class o2

    @Parameter(2)
    public int expectedSign

    @Test
    void testCompare() {
        if (o1 == null || o2 == null) {
            thrown.expect(NullPointerException)
        }

        assert Math.signum(ClassComparator.INSTANCE.compare(o1, o2)) == expectedSign
    }

    @Test
    void testCompareReversed() {
        if (o1 == null || o2 == null) {
            thrown.expect(NullPointerException)
        }

        assert Math.signum(ClassComparator.INSTANCE.compare(o2, o1)) == -expectedSign
    }

    @Parameters(name = "{index}: {0} <=> {1} = {2}")
    static Collection<Object[]> data() {
        List<List> values = []

        // null or same
        values << [null, null, 0]
        values << [null, String, 0]
        values << [String, String, 0]

        // same package
        values << [Integer, String, -1]
        values << [Integer, Double, 1]
        values << [Double, String, -1]

        // different package
        values << [List, String, 1]
        values << [Integer, List, -1]
        values << [String, Map, -1]

        // primitives
        values << [int, int, 0]
        values << [int, Integer, -1]
        values << [int, Double, -1]

        // arrays
        values << [int[], int[], 0]
        values << [int, int[], -1]
        values << [Object, Object[], -1]
        values << [int[], String, -1]
        values << [int[][], String, -1]
        values << [int[], int[][], -1]

        return values.collect { it as Object[] }
    }
}
