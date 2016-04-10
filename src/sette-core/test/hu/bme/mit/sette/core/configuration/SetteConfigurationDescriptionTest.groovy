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
package hu.bme.mit.sette.core.configuration

import org.junit.Test

import groovy.transform.CompileStatic

/**
 * Tests for {@link SetteConfiguration}.
 */
@CompileStatic
class SetteConfigurationDescriptionTest {
    @Test
    void testParse() {
        String json = '''
{
  "baseDirs":["/home/sette/sette", "~/sette", "D:/SETTE"],
  "outputDir":"sette-results",
  "runnerTimeoutInMs":30000,
  "snippetProjectDirs":["sette-snippets/sette-snippets"],
  "tools":[
    {
      "className":"hu.bme.mit.sette.tools.catg.CatgTool",
      "name":"CATG",
      "toolDir":"sette-tool/test-generator-tools/catg"
    },
    {
      "className":"hu.bme.mit.sette.tools.evosuite.EvoSuiteTool",
      "name":"EvoSuite",
      "toolDir":"sette-tool/test-generator-tools/evosuite"
    },
    {
      "className":"hu.bme.mit.sette.tools.jpet.JPetTool",
      "name":"jPET",
      "toolDir":"sette-tool/test-generator-tools/jpet"
    },
    {
      "className":"hu.bme.mit.sette.tools.randoop.RandoopTool",
      "name":"Randoop",
      "toolDir":"sette-tool/test-generator-tools/randoop"
    },
    {
      "className":"hu.bme.mit.sette.tools.spf.SpfTool",
      "name":"SPF",
      "toolDir":"sette-tool/test-generator-tools/spf"
    }
  ]
}'''

        SetteConfigurationDescription.parse(json).with {
            assert baseDirPaths == ['/home/sette/sette', '~/sette', 'D:/SETTE']
            assert outputDirPath == 'sette-results'
            assert runnerTimeoutInMs == 30000
            assert snippetProjectDirPaths== ['sette-snippets/sette-snippets']
            assert toolConfigurations.size() == 5

            toolConfigurations[0].with {
                assert className == 'hu.bme.mit.sette.tools.catg.CatgTool'
                assert name == 'CATG'
                assert toolDirPath == 'sette-tool/test-generator-tools/catg'
            }
        }
    }

    @Test(expected = SetteConfigurationException)
    void testParseThrowsExceptionIfTimeoutIsString() {
        String json = '''
{
  "baseDirs":["~/sette"],
  "outputDir":"sette-results",
  "runnerTimeoutInMs":"30000",
  "snippetProjectDirs":["snippets"],
  "tools":[
    {
      "className":"com.example.MyTool",
      "name":"My Tool",
      "toolDir":"my-tool"
    }
  ]
}'''
        SetteConfigurationDescription.parse(json)
    }

    @Test(expected = SetteConfigurationException)
    void testParseThrowsExceptionIfTimeoutIsMissing() {
        String json = '''
{
  "baseDirs":["~/sette"],
  "outputDir":"sette-results",
  "snippetProjectDirs":["snippets"],
  "tools":[
    {
      "className":"com.example.MyTool",
      "name":"My Tool",
      "toolDir":"my-tool"
    }
  ]
}'''
        SetteConfigurationDescription.parse(json)
    }

    @Test(expected = SetteConfigurationException)
    void testParseThrowsExceptionIfContainsNonAsciiChar() {
        String json = '''
{
  "baseDirs":["~/sette-\u0151"],
  "outputDir":"sette-results",
  "runnerTimeoutInMs":30000,
  "snippetProjectDirs":["snippets"],
  "tools":[
    {
      "className":"com.example.MyTool",
      "name":"My Tool",
      "toolDir":"my-tool"
    }
  ]
}'''

        SetteConfigurationDescription.parse(json)
    }

    @Test(expected = SetteConfigurationException)
    void testParseThrowsExceptionIfBaseDirArrayIsString() {
        String json = '''
{
  "baseDirs": "this should be an array",
  "outputDir":"sette-results",
  "runnerTimeoutInMs":30000,
  "snippetProjectDirs":["snippets"],
  "tools":[
    {
      "className":"com.example.MyTool",
      "name":"My Tool",
      "toolDir":"my-tool"
    }
  ]
}'''

        SetteConfigurationDescription.parse(json)
    }

    @Test(expected = SetteConfigurationException)
    void testParseThrowsExceptionIfToolsAreStrings() {
        String json = '''
{
  "baseDirs": 0
  "outputDir":"sette-results",
  "runnerTimeoutInMs":30000,
  "snippetProjectDirs":["snippets"],
  "tools":["it should be an", "object array", "not a string array"]
}'''

        SetteConfigurationDescription.parse(json)
    }

    @Test(expected = SetteConfigurationException)
    void testParshrowsExceptionIfJsonIsAnArray() {
        SetteConfigurationDescription.parse('["~/sette", 30000]')
    }

    @Test(expected = SetteConfigurationException)
    void testParseThrowsExceptionIfJsonIsInvalid() {
        SetteConfigurationDescription.parse('{123: ["~/sette" ["~sette/sette"] []}')
    }
}
