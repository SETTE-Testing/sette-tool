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
package hu.bme.mit.sette.core.util.reflection

import groovy.transform.TypeChecked

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters

/**
 * Tests for {@link ClassComparator}.
 */
@RunWith(Parameterized)
@TypeChecked
class ClassComparatorTest {
    @Parameter(0)
    public Class<?> o1

    @Parameter(1)
    public Class<?> o2

    @Parameter(2)
    public int expectedSign

    @Test
    void testCompare() {
        // tests in both orders
        ClassComparator cmp =  ClassComparator.INSTANCE

        assert Math.signum(cmp.compare(o1, o2)) == expectedSign
        assert Math.signum(cmp.compare(o2, o1)) == -expectedSign
    }

    @Parameters
    static Collection<Object[]> data() {
        List<List<?>> values = []

        // null or same
        values << [null, null, 0]
        values << [null, String, -1]
        values << [String, String, 0]

        // same package
        values << [Integer, String, -1]
        values << [Integer, Double, 1]
        values << [Double, String, -1]

        // different package
        values << [List, String, 1]
        values << [Integer, List, -1]
        values << [String, Map, -1]

        return values.collect { it as Object[] }
    }
}
