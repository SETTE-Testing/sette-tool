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

import java.lang.reflect.Constructor
import java.lang.reflect.Executable
import java.lang.reflect.Method

import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters

/**
 * Tests for {@link ExecutableComparator}.
 */
@CompileStatic
@RunWith(Parameterized)
class ExecutableComparatorTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none()

    @Parameter(0)
    public Executable e1

    @Parameter(1)
    public Executable o2

    @Parameter(2)
    public int expectedSign

    @Test
    void testCompare() {
        if (e1 == null || o2 == null) {
            thrown.expect(NullPointerException)
        }

        assert Math.signum(ExecutableComparator.INSTANCE.compare(e1, o2)) == expectedSign
    }

    @Test
    void testCompareReversed() {
        if (e1 == null || o2 == null) {
            thrown.expect(NullPointerException)
        }

        assert Math.signum(ExecutableComparator.INSTANCE.compare(o2, e1)) == -expectedSign
    }

    @Parameters(name = "{index}: {0} <=> {1} = {2}")
    static Collection<Object[]> data() {
        Method objectHashCode = m(Object, 'hashCode')

        Constructor stringCtorDefault = c(String)
        Constructor stringCtorCopy = c(String, String)
        Constructor stringCtorCharArr = c(String, char[])

        Method stringEquals = m(String, 'equals', Object)
        Method stringHashCode = m(String, 'hashCode')
        Method stringIndexOfInt = m(String, 'indexOf', int)
        Method stringIndexOfIntInt = m(String, 'indexOf', int, int)
        Method stringValueOfChar = m(String, 'valueOf', char)
        Method stringValueOfInt = m(String, 'valueOf', int)

        Method arrayListAdd = m(ArrayList, 'add', Object)
        Method arrayListAddWithIdx = m(ArrayList, 'add', int, Object)

        List<List> values = []

        // null or same
        values << [null, null, 0]
        values << [null, objectHashCode, 0]
        values << [objectHashCode, objectHashCode, 0]

        // different class
        values << [objectHashCode, stringHashCode, -1]
        values << [objectHashCode, arrayListAdd, -1]
        values << [stringHashCode, arrayListAdd, -1]

        // same class, constructor vs method
        values << [stringCtorDefault, stringEquals, -1]
        values << [stringCtorDefault, stringHashCode, -1]
        values << [stringCtorCopy, stringEquals, -1]
        values << [stringCtorCopy, stringHashCode, -1]

        // same class constructors
        values << [stringCtorDefault, stringCtorCopy, -1]
        values << [stringCtorCharArr, stringCtorCopy, -1]
        values << [stringCtorDefault, stringCtorCharArr, -1]

        // same class methods
        values << [stringEquals, stringHashCode, -1]
        values << [stringEquals, stringValueOfChar, -1]
        values << [stringHashCode, stringValueOfChar, -1]
        values << [stringValueOfChar, stringValueOfInt, -1]
        values << [arrayListAddWithIdx, arrayListAdd, -1]
        values << [stringIndexOfInt, stringIndexOfIntInt, -1]

        return values.collect { it as Object[] }
    }

    /**
     * Gets a {@link Constructor} from a {@link Class} with the parameter type list.
     *
     * @param cls the class
     * @param parameterTypes the parameter type list
     * @return a {@link Constructor} object
     */
    private static Constructor c(Class cls, Class... parameterTypes) {
        try {
            return cls.getConstructor(parameterTypes)
        } catch (NoSuchMethodException ex) {
            // go on
        }

        try {
            return cls.getDeclaredConstructor(parameterTypes)
        } catch (NoSuchMethodException ex) {
            // go on
        }

        throw new NoSuchMethodException("Class: ${cls.name}, constructor: " +
        "<init>(${parameterTypes*.toString().join(', ')})")
    }
    /**
     * Gets a {@link Method} from a {@link Class} with the desired name and parameter type list.
     * 
     * @param cls the class
     * @param methodName the method name
     * @param parameterTypes the parameter type list
     * @return a {@link Method} object
     */
    private static Method m(Class cls, String methodName, Class... parameterTypes) {
        try {
            return cls.getMethod(methodName, parameterTypes)
        } catch (NoSuchMethodException ex) {
            // go on
        }

        try {
            return cls.getDeclaredMethod(methodName, parameterTypes)
        } catch (NoSuchMethodException ex) {
            // go on
        }

        throw new NoSuchMethodException("Class: ${cls.name}, method: " +
        "$methodName(${parameterTypes*.toString().join(', ')})")
    }
}
