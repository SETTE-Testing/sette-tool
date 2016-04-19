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
package hu.bme.mit.sette.test.system

import groovy.transform.CompileStatic
import hu.bme.mit.sette.application.SetteApplication
import hu.bme.mit.sette.core.configuration.SetteConfiguration
import hu.bme.mit.sette.core.configuration.SetteToolConfiguration
import hu.bme.mit.sette.core.model.csv.ResultCsvEntry
import hu.bme.mit.sette.core.util.io.PathUtils
import hu.bme.mit.sette.test.TestBufferedReader
import hu.bme.mit.sette.test.TestConfiguration
import hu.bme.mit.sette.test.TestPrintStream

import java.nio.file.Path

import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.MappingIterator
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvSchema

@CompileStatic
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(Parameterized)
class EvaluationRegressionTest {
    private static SetteConfiguration configuration = TestConfiguration.create()

    TestBufferedReader input
    TestPrintStream output, errorOutput
    SetteApplication app

    @Parameter(0)
    public String toolName
    @Parameter(1)
    public String tag
    @Parameter(2)
    public List<String> tasks

    @Before
    void setUp() {
        input = new TestBufferedReader()
        output = new TestPrintStream()
        errorOutput = new TestPrintStream()
        app = new SetteApplication(input, output, errorOutput, configuration)
    }

    @Test
    void test_1_Evaluation() {
        tasks.each { String task ->
            output.reset()
            errorOutput.reset()

            // expect no exception
            try {
                app.execute( '--snippet-project-dir', 'sette-snippets/java/sette-snippets',
                        '--backup', 'skip',
                        '--tool', toolName, '--runner-project-tag', tag, '--task', task)
            } catch (Exception ex) {
                println '='.multiply(80)
                output.lines.each { println it }
                println '='.multiply(80)
                errorOutput.lines.each { println it }

                assert false : 'Failure, check output'
            }
        }

    }

    // check CSV in separate test to make it runnable as a separate test
    @Test
    void test_2_checkCsv() {
        String runnerProjectName = "sette-snippets___${toolName.toLowerCase()}___${tag}"
        Path expectedCsvFile = configuration.getOutputDir().resolve("expected_${runnerProjectName}.csv")
        Path actualCsvFile =  configuration.getOutputDir().resolve("$runnerProjectName/sette-evaluation.csv")

        assert PathUtils.exists(actualCsvFile) : 'The CSV file does not exists'

        if (PathUtils.exists(expectedCsvFile)) {
            CsvMapper mapper = new CsvMapper()
            CsvSchema schema = ResultCsvEntry.createSchema()

            List<ResultCsvEntry> expectedData = mapper.readerFor(ResultCsvEntry)
                    .with(schema).readValues(expectedCsvFile.toFile()) as List
            List<ResultCsvEntry> actualData = mapper.readerFor(ResultCsvEntry)
                    .with(schema).readValues(actualCsvFile.toFile()) as List

            assert expectedData.size() == 300
            assert actualData.size() == 300

            actualData.each { ResultCsvEntry entry ->
                entry.with {
                    assert it && snippet.startsWith(category)
                    assert it && tool == toolName
                    assert it && Double.parseDouble(coverage ?: '0') >= 0
                    assert it && status in ['N/A', 'EX', 'T/M', 'NC', 'C']
                    assert it && Integer.parseInt(size ?: '0') >= 0
                    assert it && run == tag
                    assert it && Integer.parseInt(duration ?: '1') > 0

                }
            }

            assert expectedData == actualData
        } else {
            PathUtils.copy(actualCsvFile, expectedCsvFile)
            assert false : 'No expected CSV was found, the actual CSV has been copied and ' +
            'please re-run the test to verify that the result is the same if run again'
        }
    }

    @Parameters(name = '{index} : {0} {1} {2}')
    static Collection<Object[]> data() {
        assert configuration.toolConfigurations.size() == 6 // make sure all tools are used

        List<List> data = []

        configuration.toolConfigurations.each { SetteToolConfiguration tc ->
            String tag = 'run-01-30sec'
            List<String> tasks = []

            if (tc.getClassName().endsWith('SnippetInputCheckerTool')) {
                tasks += ['generator', 'runner']
            }

            tasks += ['parser', 'test-generator', 'test-runner', 'export-csv']

            data << [tc.name, tag, tasks]
        }

        return data.collect { it as Object[] }
    }
}
