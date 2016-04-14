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
package hu.bme.mit.sette.core.tasks.testsuiterunner;

import org.jacoco.core.analysis.ICounter;

public enum LineStatus {
    EMPTY,
    NOT_COVERED,
    FULLY_COVERED,
    PARTLY_COVERED;
    // TODO for future use to show if a line is PARTLY covered several times 
    // FULLY_OR_PARTLY_COVERED; 

    public static LineStatus fromJaCoCo(int status) {
        switch (status) {
            case ICounter.NOT_COVERED:
                return NOT_COVERED;

            case ICounter.FULLY_COVERED:
                return FULLY_COVERED;

            case ICounter.PARTLY_COVERED:
                return LineStatus.PARTLY_COVERED;

            // FIXME if not known in ICounter, fail?
            // case ICounter.EMPTY:
            default:
                return LineStatus.EMPTY;
        }
    }

    public boolean countsForStatementCoverage() {
        return this != EMPTY && this != NOT_COVERED;
    }
}
