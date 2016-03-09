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
package hu.bme.mit.sette

import groovy.transform.TypeChecked
import hu.bme.mit.sette.application.SetteApplication

import java.nio.file.Paths

import org.junit.Before
import org.junit.Test

@TypeChecked
class CheckerTest {
    SetteApplication app

    @Before
    void setUp() {
        app = new SetteApplication(new BufferedReader(new InputStreamReader(System.in)),
                System.out, System.err, Paths.get('sette.config.json'))
    }
    
    @Test
    public final void test() {
        List<String> tasks = [
            'generator',
            'runner',
            'parser',
            'test-generator',
            'test-runner',
            'export-csv'
        ]

        tasks.each { String task ->
            app.execute('--snippet-project-dir', 'sette-snippets/sette-snippets', '--backup', 'skip',
                    '--runner-project-tag', 'test', '--task', task, '--tool', 'SnippetInputChecker')
        }
    }

    @Test
    public final void testPerformanceTime() {
        List<String> tasks = [
            'generator',
            'runner',
            'parser',
            'test-generator',
            'test-runner',
            'export-csv'
        ]

        tasks.each { String task ->
            app.execute('--snippet-project-dir', 'sette-snippets/sette-snippets-performance-time', 
                    '--backup', 'skip',
                    '--runner-project-tag', 'test', '--task', task, '--tool', 'SnippetInputChecker')
        }
    }
}
