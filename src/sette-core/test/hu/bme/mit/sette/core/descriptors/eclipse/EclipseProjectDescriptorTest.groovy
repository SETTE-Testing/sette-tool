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
import org.w3c.dom.Document

/**
 * Test for {@link EclipseProjectDescriptor}.
 */
@TypeChecked
class EclipseProjectDescriptorTest {
    @Test
    void testCreateXmlDocument() {
        def pd = new EclipseProjectDescriptor('test', null)

        Document xml = pd.createXmlDocument()

        List<String> docLines = (xml.documentElement as String).tokenize('\n')*.trim()
        List<String> expectedLines = '''
<?xml version="1.0" encoding="UTF-8"?><projectDescription>
  <name>test</name>
  <comment/>
  <projects/>
  <buildSpec>
    <buildCommand>
      <name>org.eclipse.jdt.core.javabuilder</name>
      <arguments/>
    </buildCommand>
  </buildSpec>
  <natures>
    <nature>org.eclipse.jdt.core.javanature</nature>
  </natures>
</projectDescription>
'''.tokenize('\n')*.trim()

        assert docLines == expectedLines
    }

    @Test
    void testCreateXmlDocumentWithComment() {
        def pd = new EclipseProjectDescriptor('test', 'comment')

        Document xml = pd.createXmlDocument()

        List<String> docLines = (xml.documentElement as String).tokenize('\n')*.trim()
        List<String> expectedLines = '''
<?xml version="1.0" encoding="UTF-8"?><projectDescription>
  <name>test</name>
  <comment>comment</comment>
  <projects/>
  <buildSpec>
    <buildCommand>
      <name>org.eclipse.jdt.core.javabuilder</name>
      <arguments/>
    </buildCommand>
  </buildSpec>
  <natures>
    <nature>org.eclipse.jdt.core.javanature</nature>
  </natures>
</projectDescription>
'''.tokenize('\n')*.trim()

        assert docLines == expectedLines
    }
}
