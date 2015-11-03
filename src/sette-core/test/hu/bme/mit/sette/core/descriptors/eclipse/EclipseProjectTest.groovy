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

import java.nio.file.Files
import java.nio.file.Path

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Tests for {@link EclipseProjectTest}.
 */
@TypeChecked
class EclipseProjectTest {
    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder()

    private static final List<String> expectedProjectLines = '''
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

    private static final List<String> expectedClasspathLines = '''
<?xml version="1.0" encoding="UTF-8"?><classpath>
  <classpathentry kind="con" path="org.eclipse.jdt.launching.JRE_CONTAINER"/>
</classpath>
'''.tokenize('\n')*.trim()

    @Test
    void testSaveWithoutClasspath() {
        def pd = new EclipseProjectDescriptor('test', null)
        def p = new EclipseProject(pd, null)

        Path dir = tmpDir.newFolder().toPath()
        p.save(dir)

        List<Path> files = Files.newDirectoryStream(dir).collect() as List
        assert files*.fileName*.toString() == ['.project']

        List<String> projectLines = Files.readAllLines(dir.resolve('.project')) *.trim()
        assert projectLines == expectedProjectLines
    }

    @Test
    void testSaveWithClasspath() {
        def pd = new EclipseProjectDescriptor('test', null)
        def cd = new EclipseClasspathDescriptor()
        cd.addEntry(EclipseClasspathEntry.JRE_CONTAINER)
        def p = new EclipseProject(pd, cd)

        Path dir = tmpDir.newFolder().toPath()
        p.save(dir)

        List<Path> files = Files.newDirectoryStream(dir).collect() as List
        assert files*.fileName*.toString() as Set == ['.project', '.classpath'] as Set

        List<String> projectLines = Files.readAllLines(dir.resolve('.project')) *.trim()
        List<String> classpathLines = Files.readAllLines(dir.resolve('.classpath')) *.trim()

        assert projectLines == expectedProjectLines
        assert classpathLines == expectedClasspathLines
    }
}
