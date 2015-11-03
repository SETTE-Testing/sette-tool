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
package hu.bme.mit.sette.core.descriptors.eclipse

import groovy.transform.TypeChecked

import org.junit.Test
import org.w3c.dom.Document

/**
 * Tests for {@link EclipseClasspathDescriptor}.
 */
@TypeChecked
class EclipseClasspathDescriptorTest {
    @Test(expected = NullPointerException)
    void testAddEntryThrowsExceptionIfNull() {
        new EclipseClasspathDescriptor().addEntry(null)
    }

    @Test
    void testCreateXmlDocument() {
        def cd = new EclipseClasspathDescriptor()
        // unique and insertion order
        cd.addEntry(EclipseClasspathEntry.JRE_CONTAINER)
        cd.addEntry(new EclipseClasspathEntry(EclipseClasspathEntryKind.LIBRARY, 'lib/mylib.jar'))

        Document xml = cd.createXmlDocument()

        List<String> docLines = (xml.documentElement as String).tokenize('\n')*.trim()
        List<String> expectedLines = '''
<?xml version="1.0" encoding="UTF-8"?><classpath>
  <classpathentry kind="con" path="org.eclipse.jdt.launching.JRE_CONTAINER"/>
  <classpathentry kind="lib" path="lib/mylib.jar"/>
</classpath>
'''.tokenize('\n')*.trim()

        assert docLines == expectedLines
    }
}
