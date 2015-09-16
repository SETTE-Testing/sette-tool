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
// TODO z revise this file
package hu.bme.mit.sette.tools.jpet.xmlparser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class TestCase {
    private final List<DataOrRef> argsIn;
    private final Map<String, HeapElement> heapIn;
    private final Map<String, HeapElement> heapOut;
    private String exceptionFlag = null;

    public TestCase() {
        argsIn = new ArrayList<>();
        heapIn = new HashMap<>();
        heapOut = new HashMap<>();
    }

    public List<DataOrRef> argsIn() {
        return argsIn;
    }

    public Map<String, HeapElement> heapIn() {
        return heapIn;
    }

    public Map<String, HeapElement> heapOut() {
        return heapOut;
    }

    public String getExceptionFlag() {
        return exceptionFlag;
    }

    public void setExceptionFlag(String exceptionFlag) {
        this.exceptionFlag = exceptionFlag;
    }
}
