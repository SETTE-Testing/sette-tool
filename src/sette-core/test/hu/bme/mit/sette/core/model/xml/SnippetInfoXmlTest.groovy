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
package hu.bme.mit.sette.core.model.xml

import groovy.transform.CompileStatic
import hu.bme.mit.sette.core.model.xml.SnippetInfoXml;
import hu.bme.mit.sette.core.util.io.PathUtils
import hu.bme.mit.sette.core.util.xml.XmlException;
import hu.bme.mit.sette.core.util.xml.XmlUtils
import hu.bme.mit.sette.core.validator.ValidationException

import java.nio.file.Path
import java.nio.file.Paths

import org.hamcrest.CoreMatchers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.rules.TemporaryFolder

/**
 * Tests for {@link SnippetInfoXml}.
 */
@CompileStatic
class SnippetInfoXmlTest {
    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder()

    @Rule
    public ExpectedException thrown = ExpectedException.none()

    SnippetInfoXml sampleInfo
    List<String> sampleXmlLines
    Path file

    private static List<String> toLines(String string) {
        return string.trim().split('\n')*.trim()
    }

    @Before
    void setUp() {
        sampleInfo = new SnippetInfoXml()
        sampleInfo.workingDirectory = Paths.get('testDir')
        sampleInfo.command = ['test', 'cmd']
        sampleInfo.exitValue = 1
        sampleInfo.destroyed = true
        sampleInfo.elapsedTimeInMs = 1000

        sampleXmlLines = toLines('''
<?xml version="1.0" encoding= "UTF-8" ?>
<setteSnippetInfo>
   <workingDirectory>testDir</workingDirectory>
   <command>
      <c>test</c>
      <c>cmd</c>
   </command>
   <exitValue>1</exitValue>
   <destroyed>true</destroyed>
   <elapsedTimeInMs>1000</elapsedTimeInMs>
</setteSnippetInfo>''')

        file = tmpDir.newFile().toPath()
    }

    @Test
    void testSerializeToXml() {
        XmlUtils.serializeToXml(sampleInfo, file)
        List<String> actualLines = PathUtils.readAllLines(file)*.trim()
        assert actualLines == sampleXmlLines
    }

    @Test
    void testSerializeInvalidToXmlFails() {
        thrown.expect(XmlException)
        thrown.expectCause(CoreMatchers.isA(ValidationException))

        sampleInfo.elapsedTimeInMs = -1

        XmlUtils.serializeToXml(sampleInfo, file)
    }

    @Test
    void testDeserializeFromXml() {
        PathUtils.write(file, sampleXmlLines)
        SnippetInfoXml info = XmlUtils.deserializeFromXml(SnippetInfoXml, file)
        assert info == sampleInfo
    }

    @Test
    void testDeserializeInvalidFromXmlFails() {
        thrown.expect(XmlException)
        thrown.expectCause(CoreMatchers.isA(ValidationException))

        int idx = sampleXmlLines.findIndexOf { String xmlLine ->
            xmlLine.contains('<elapsedTimeInMs>')
        }
        assert idx >= 0 : 'Cannot manipulate XML'

        sampleXmlLines[idx] = sampleXmlLines[idx].replaceFirst('\\<elapsedTimeInMs\\>\\d+', '<elapsedTimeInMs>-1')

        PathUtils.write(file, sampleXmlLines)
        XmlUtils.deserializeFromXml(SnippetInfoXml, file)
    }
}
