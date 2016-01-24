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
package hu.bme.mit.sette.core.util

import groovy.transform.TypeChecked

import org.junit.Before
import org.junit.Test
import org.junit.rules.ExpectedException

@TypeChecked
class LazyImmutableTest {
    LazyImmutable<Integer> var1, var2

    @Before
    void setUp() {
        var1 = LazyImmutable.of()
        var2 = LazyImmutable.of()
    }

    @Test
    void testBasic() {
        assert !var1.isSet()
        assert var1.get() == null
        
        var1.set(1)
        assert var1.isSet()
        assert var1.get() == 1
    }

    @Test(expected = IllegalStateException)
    void testThrowsExceptionIfSetTwice() {
        var1.set(1)
        var1.set(2)
    }

    @Test
    void testEqualsHashCode() {
        assert var1 == var2
        assert var1.hashCode() == var2.hashCode()

        var1.set(1)
        assert var1 != var2
        // hash code is not necessary different

        var2.set(1)
        assert var1 == var2
        assert var1.hashCode() == var2.hashCode()
    }

    @Test
    void testToString() {
        var1.set(1)
        assert var1.toString() == 'LazyImmutable [1]'
        assert var2.toString() == 'LazyImmutable [null]'
    }
}
