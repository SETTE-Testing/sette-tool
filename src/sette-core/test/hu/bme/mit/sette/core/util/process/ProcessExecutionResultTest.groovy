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
package hu.bme.mit.sette.core.util.process

import org.junit.Test

import groovy.transform.TypeChecked

/**
 * Tests for {@link ProcessExecutionResult}.
 */
@TypeChecked
class ProcessExecutionResultTest {
    @Test
    void test() {
        def per1 = new ProcessExecutionResult(0, false,  100)
        def per2 = new ProcessExecutionResult(0, false,  100)
        def per3 = new ProcessExecutionResult(1, true,  5000)
        def per4 = new ProcessExecutionResult(0, true,  5000)
        def per5 = new ProcessExecutionResult(0, false,  5000)

        per1.with { assert exitValue == 0 && !destroyed && elapsedTimeInMs == 100 }
        per3.with { assert exitValue == 1 && destroyed && elapsedTimeInMs == 5000 }

        assert !per1.equals(null)
        assert !per1.equals(0)
        assert per1.equals(per1)
        assert per1 == per2
        assert per1 != per3
        assert per2 != per3
        assert per1 != per4
        assert per1 != per5
        
        Set<String> set = [per1, per2, per3] as Set<String>
        assert set.size() == 2
        
        assert per1.toString() == 'ProcessExecutionResult [exitValue=0, destroyed=false, elapsedTimeInMs=100]'
    }
}
