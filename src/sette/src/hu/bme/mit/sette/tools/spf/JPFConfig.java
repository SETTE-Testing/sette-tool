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
// NOTE revise this file
package hu.bme.mit.sette.tools.spf;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public final class JPFConfig {
    public static final String SYMBOLIC_LISTENER = "gov.nasa.jpf.symbc.SymbolicListener";
    public static final String SYMBOLIC_SEQUENCE_LISTENER = "gov.nasa.jpf.symbc.sequences.SymbolicSequenceListener";
    public static final String DP_CHOCO = "choco";
    public static final String DP_CORAL = "coral";

    public String target;
    public List<String> symbolicMethod = new ArrayList<>();
    public String classpath;
    public String listener;
    public String symbolicDebug;
    public String decisionProcedure;
    public String searchMultipleErrors;

    public StringBuilder generate() {
        StringBuilder sb = new StringBuilder();

        if (StringUtils.isNotBlank(target)) {
            sb.append("target=").append(target).append('\n');
        }

        if (symbolicMethod != null && symbolicMethod.size() > 0) {
            sb.append("symbolic.method=");
            sb.append(StringUtils.join(symbolicMethod, ','));
            sb.append('\n');
        }

        if (StringUtils.isNotBlank(classpath)) {
            sb.append("classpath=").append(classpath).append('\n');
        }

        if (StringUtils.isNotBlank(listener)) {
            sb.append("listener=").append(listener).append('\n');
        }

        if (StringUtils.isNotBlank(symbolicDebug)) {
            sb.append("symbolic.debug=").append(symbolicDebug).append('\n');
        }

        if (StringUtils.isNotBlank(searchMultipleErrors)) {
            sb.append("search.multiple_errors=").append(searchMultipleErrors).append('\n');
        }

        if (StringUtils.isNotBlank(decisionProcedure)) {
            sb.append("symbolic.dp=").append(decisionProcedure).append('\n');
        }

        return sb;
    }
}
