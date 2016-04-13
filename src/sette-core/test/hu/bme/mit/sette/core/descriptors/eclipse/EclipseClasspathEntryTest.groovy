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
package hu.bme.mit.sette.core.descriptors.eclipse

import groovy.transform.TypeChecked

import org.junit.Test

/**
 * Tests for {@link EclipseClasspathEntry}.
 */
@TypeChecked
class EclipseClasspathEntryTest {
    @Test
    void testCtor() {
        def entry = new EclipseClasspathEntry(EclipseClasspathEntryKind.LIBRARY, 'lib/mylib.jar')

        assert entry.kind == EclipseClasspathEntryKind.LIBRARY
        assert entry.path == 'lib/mylib.jar'
    }

    @Test(expected = NullPointerException)
    void testCtorThrowsExceptionIfKindIsNull() {
        new EclipseClasspathEntry(null, 'lib/mylib.jar')
    }

    @Test(expected = NullPointerException)
    void testCtorThrowsExceptionIfPathIsNull() {
        new EclipseClasspathEntry(EclipseClasspathEntryKind.LIBRARY, null)
    }

    @Test
    void testJreEntry() {
        assert EclipseClasspathEntry.JRE_CONTAINER.kind == EclipseClasspathEntryKind.CONTAINER
        assert EclipseClasspathEntry.JRE_CONTAINER.path == 'org.eclipse.jdt.launching.JRE_CONTAINER'
    }
}
