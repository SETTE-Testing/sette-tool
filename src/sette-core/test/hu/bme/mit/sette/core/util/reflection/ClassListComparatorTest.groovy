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

import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters

import groovy.transform.CompileStatic

/**
 * Tests for {@link ClassListComparator}.
 */
@CompileStatic
@RunWith(Parameterized)
class ClassListComparatorTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none()

    @Parameter(0)
    public List<Class> o1

    @Parameter(1)
    public List<Class> o2

    @Parameter(2)
    public int expectedSign

    @Test
    void testCompare() {
        if (o1 == null || o2 == null) {
            thrown.expect(NullPointerException)
        } else if (o1.contains(null) || o2.contains(null)) {
            thrown.expect(IllegalArgumentException)
        }

        assert Math.signum(ClassListComparator.INSTANCE.compare(o1, o2)) == expectedSign
    }

    @Test
    void testCompareReversed() {
        if (o1 == null || o2 == null) {
            thrown.expect(NullPointerException)
        } else if (o1.contains(null) || o2.contains(null)) {
            thrown.expect(IllegalArgumentException)
        }

        assert Math.signum(ClassListComparator.INSTANCE.compare(o2, o1)) == -expectedSign
    }

    @Parameters(name = "{index}: {0} <=> {1} = {2}")
    static Collection<Object[]> data() {
        List<List> values = []

        // null or same
        values << [null, null, 0]
        values << [[null], [String], 0]
        values << [[String], [String], 0]
        values << [[String, int, Object], [String, int, Object], 0]

        // difference before end
        values << [[Object, int, Object], [String, int, Object], -1]
        values << [[java.lang.String, int, Object], [java.util.List, int, Object], -1]
        values << [[Object, double, Object], [String, int, Object], -1]
        values << [[String, int, Object], [String, int[], Object], -1]
        values << [[Object, int, Object, int], [String, int, Object], -1]
        
        // no difference in common part
        values << [[Object, int, Object], [String, int, Object, Void], -1]

        return values.collect { it as Object[] }
    }
}
