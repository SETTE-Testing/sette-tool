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
package hu.bme.mit.sette.test

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import hu.bme.mit.sette.core.configuration.SetteConfiguration

import java.nio.file.Path
import java.nio.file.Paths

@CompileStatic
public class TestConfiguration {
    public static SetteConfiguration create() {
        Path jsonFile = Paths.get('sette.config.default.json')
        Map json = (Map) new JsonSlurper().parse(jsonFile.toFile())
        json['outputDir'] = 'sette-results-test'

        String jsonString =  JsonOutput.with { prettyPrint(toJson(json)) }
        return SetteConfiguration.parse(jsonString)
    }
}
