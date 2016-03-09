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
package hu.bme.mit.sette.application

import groovy.transform.TypeChecked
import groovy.transform.TypeChecked.*
import groovy.util.logging.Slf4j
import hu.bme.mit.sette.TestPrintStream
import hu.bme.mit.sette.core.configuration.SetteConfiguration

import java.io.ByteArrayOutputStream.*

import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.Before.*

/**
 * Tests for {@link SetteArgumentParser}.
 */
@Slf4j
@TypeChecked
class ArgumentParserTest {
    static SetteConfiguration config
    ArgumentParser argParser
    TestPrintStream errorOutput

    @BeforeClass
    static void setUpClass() {
        config = SetteConfiguration.parse('''
{
  "baseDirs":["~/sette", "D:/SETTE"],
  "outputDir":"sette-results",
  "runnerTimeoutInMs":30000,
  "snippetProjectDirs":["sette-tool/src/sette-sample-snippets"],
  "tools":[
    {
      "className":"hu.bme.mit.sette.catg.CatgTool",
      "name":"CATG",
      "toolDir":"sette-tool/test-generator-tools/catg"
    },
    {
      "className":"hu.bme.mit.sette.evosuite.EvoSuiteTool",
      "name":"EvoSuite",
      "toolDir":"sette-tool/test-generator-tools/evosuite"
    },
    {
      "className":"hu.bme.mit.sette.jpet.JPetTool",
      "name":"jPET",
      "toolDir":"sette-tool/test-generator-tools/jpet"
    },
    {
      "className":"hu.bme.mit.sette.randoop.RandoopTool",
      "name":"Randoop",
      "toolDir":"sette-tool/test-generator-tools/randoop"
    },
    {
      "className":"hu.bme.mit.sette.spf.SpfTool",
      "name":"SPF",
      "toolDir":"sette-tool/test-generator-tools/spf"
    }
  ]
}
''')
    }

    @Before
    void setUp() {
        errorOutput = new TestPrintStream()
        argParser = new ArgumentParser(config, errorOutput)
    }

    @Test
    void testParseNoArgs() {
        argParser.with {
            assert parse() : errorOutput.lines

            assert errorOutput.lines.isEmpty()

            assert backupPolicy == BackupPolicy.ASK
            assert runnerProjectTag == null
            assert runnerTimeoutInMs == 30000
            assert snippetProjectDir == null
            assert applicationTask == null
            assert toolConfiguration == null
        }
    }

    @Test
    void testParseHelp() {
        argParser.with {
            assert !parse('--help') : errorOutput.lines
            assert errorOutput.lines[0].startsWith('Usage:')

            // only verify message here
            List<String> actualLines = errorOutput.lines*.trim()
            List<String> expectedLines = '''
Usage:
 --backup [ASK | CREATE | SKIP]         : Set the backup policy for runner
                                          projects (used when the runner
                                          project already exists before
                                          generation) (default: ASK)
 --runner-project-tag [TAG]             : The tag of the desired runner project
 --runner-timeout [ 30000ms | 30s ]     : Timeout for execution of a tool on
                                          one snippet - if missing, then the
                                          value specified in the configuration
                                          will be used (default: 30000)
 --snippet-project-dir [PROJECT_NAME]   : The path to the snippet-project
                                          (relative to the base-directory) to
                                          use - if missing, then the user will
                                          be asked to select one from the
                                          projects specified in the
                                          configuration
 --snippet-selector [PATTERN]           : Regular expression to filter a subset
                                          of the snippets (the pattern will be
                                          matched against snippet IDs and it
                                          will only be used by the runner task)
 --task [exit | generator | runner |    : The task to execute
 parser | test-generator | test-runner
 | snippet-browser | export-csv |
 export-csv-batch]
 --tool [CATG | EvoSuite | Randoop |    : The tool to use
 SPF | jPET]'''.trim().replace('\r\n', '\n').split('\n')*.trim()

            if (actualLines != expectedLines) {
                println '== ACTUAL HELP BEGIN'
                println errorOutput.lines.join('\n')
                println '== ACTUAL HELP END'
                
                assert actualLines == expectedLines
            } 
        }
    }

    @Test
    void testParseAllArgs() {
        argParser.with{
            assert parse('--backup', 'skip', '--runner-project-tag', 'my tag',
            '--runner-timeout', '5000ms', '--snippet-project-dir', '../snippet-project',
            '--task', 'test-runner', '--tool', 'spf',
            '--snippet-selector', 'pat{2}ern') : errorOutput.lines

            assert backupPolicy == BackupPolicy.SKIP
            assert runnerProjectTag == 'my tag'
            assert runnerTimeoutInMs == 5000
            assert snippetProjectDir == '../snippet-project'
            assert applicationTask == ApplicationTask.TEST_RUNNER
            assert toolConfiguration.name == 'SPF'
            assert snippetSelector.matcher('pattern').matches()
        }
    }

    @Test
    void testParseIgnoreCaseEnum() {
        argParser.with{
            assert parse('--backup', 'CrEaTe') : errorOutput.lines

            assert backupPolicy == BackupPolicy.CREATE
            assert runnerProjectTag == null
            assert runnerTimeoutInMs == 30000
            assert snippetProjectDir == null
            assert applicationTask == null
            assert toolConfiguration == null
            assert snippetSelector == null
        }
    }

    @Test
    void testParseUnderscoreForEnum() {
        argParser.with{
            assert parse('--task', 'TEST_RUNNER') : errorOutput.lines

            assert backupPolicy == BackupPolicy.ASK
            assert runnerProjectTag == null
            assert runnerTimeoutInMs == 30000
            assert snippetProjectDir == null
            assert applicationTask == ApplicationTask.TEST_RUNNER
            assert toolConfiguration == null
            assert snippetSelector == null
        }
    }

    @Test
    void testParseDashForEnum() {
        argParser.with{
            assert parse('--task', 'test-runner') : errorOutput.lines

            assert backupPolicy == BackupPolicy.ASK
            assert runnerProjectTag == null
            assert runnerTimeoutInMs == 30000
            assert snippetProjectDir == null
            assert applicationTask == ApplicationTask.TEST_RUNNER
            assert toolConfiguration == null
            assert snippetSelector == null
        }
    }

    @Test
    void testParseFailsIfInvalidEnumValue() {
        argParser.with{
            assert !parse('--backup', 'differential') : errorOutput.lines
            assert errorOutput.lines[0].contains('"differential" is not a valid value for "--backup"')
            assert errorOutput.lines[1].startsWith('Usage:')
        }
    }

    @Test
    void testParseForTool() {
        argParser.with{
            // case-insensitive
            assert parse('--tool', 'SpF') : errorOutput.lines

            assert backupPolicy == BackupPolicy.ASK
            assert runnerProjectTag == null
            assert runnerTimeoutInMs == 30000
            assert snippetProjectDir == null
            assert applicationTask == null
            assert toolConfiguration.name == 'SPF'
            assert snippetSelector == null
        }
    }

    @Test
    void testParseFailsIfInvalidTool() {
        argParser.with{
            assert !parse('--tool', 'MyTool') : errorOutput.lines
            assert errorOutput.lines[0].contains('"MyTool" is not a valid value for "--tool"')
            assert errorOutput.lines[1].startsWith('Usage:')
        }
    }

    @Test
    void testParseTimeIfSec() {
        argParser.with{
            assert parse('--runner-timeout', '5s') : errorOutput.lines

            assert backupPolicy == BackupPolicy.ASK
            assert runnerProjectTag == null
            assert runnerTimeoutInMs == 5000
            assert snippetProjectDir == null
            assert applicationTask == null
            assert toolConfiguration == null
            assert snippetSelector == null
        }
    }

    @Test
    void testParseTimeIfMs() {
        argParser.with{
            assert parse('--runner-timeout', '5000ms') : errorOutput.lines

            assert backupPolicy == BackupPolicy.ASK
            assert runnerProjectTag == null
            assert runnerTimeoutInMs == 5000
            assert snippetProjectDir == null
            assert applicationTask == null
            assert toolConfiguration == null
            assert snippetSelector == null
        }
    }

    @Test
    void testParsePattern() {
        argParser.with{
            assert parse('--snippet-selector', 'pat{2}ern') : errorOutput.lines

            assert backupPolicy == BackupPolicy.ASK
            assert runnerProjectTag == null
            assert runnerTimeoutInMs == 30000
            assert snippetProjectDir == null
            assert applicationTask == null
            assert toolConfiguration == null
            assert snippetSelector.matcher('pattern').matches()
        }
    }

    @Test
    void testParseFailsIfNotANumber() {
        argParser.with{
            assert !parse('--runner-timeout', 'five seconds') : errorOutput.lines

            assert errorOutput.lines[0].contains('"five seconds" is not a valid value for "--runner-timeout"')
            assert errorOutput.lines[1].startsWith('Usage:')
        }
    }

    @Test
    void testParseFailsIfTimeUnitIsMissing() {
        argParser.with{
            assert !parse('--runner-timeout', '5000') : errorOutput.lines

            assert errorOutput.lines[0].contains('"5000" is not a valid value for "--runner-timeout"')
            assert errorOutput.lines[1].startsWith('Usage:')
        }
    }

    @Test
    void testParseFailsIfExtraArgs() {
        argParser.with{
            assert !parse('--runner-project-tag', 'TAG', 'extra1', 'extra2') : errorOutput.lines

            assert errorOutput.lines[0].contains('No argument is allowed: extra1')
            assert errorOutput.lines[1].startsWith('Usage:')
        }
    }

    @Test
    void testMain() throws Exception {
        assert argParser.parse('--runner-timeout', '30s') : errorOutput.lines
    }
}
