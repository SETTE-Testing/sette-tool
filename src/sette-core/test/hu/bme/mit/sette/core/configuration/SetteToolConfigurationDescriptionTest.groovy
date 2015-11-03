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
package hu.bme.mit.sette.core.configuration

import org.junit.Test

class SetteToolConfigurationDescriptionTest {
    @Test
    void test() {
        new SetteToolConfigurationDescription('com.example.MyTool', 'My Tool', 'my-tool').with {
            assert className == 'com.example.MyTool'
            assert name == 'My Tool'
            assert toolDirPath == 'my-tool'
            assert it.toString() == 'SetteToolConfigurationDescription [className=com.example.MyTool, name=My Tool, toolDirPath=my-tool]'
        }
    }
    
    @Test
    void testTrimming() {
        // only name is trimmed
        new SetteToolConfigurationDescription(' com.example.MyTool ', ' My Tool ', ' my-tool ').with {
            assert className == ' com.example.MyTool '
            assert name == 'My Tool' 
            assert toolDirPath == ' my-tool '
            assert it.toString() == 'SetteToolConfigurationDescription [className= com.example.MyTool , name=My Tool, toolDirPath= my-tool ]'
        }
    }
    
    @Test(expected = NullPointerException)
    void testThrowsExceptionIfClassNameIsNull() {
        new SetteToolConfigurationDescription(null, 'My Tool', 'my-tool')
    }
    
    @Test(expected = NullPointerException)
    void testThrowsExceptionIfNameIsNull() {
        new SetteToolConfigurationDescription('com.example.MyTool', null, 'my-tool')
    }
    
    @Test(expected = NullPointerException)
    void testThrowsExceptionIfToolDirPathIsNull() {
        new SetteToolConfigurationDescription('com.example.MyTool', 'My Tool', null)
    }
}
