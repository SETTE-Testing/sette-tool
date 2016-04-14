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

import com.google.common.base.Preconditions;

public final class LineStatuses {
    private final int beginLine;
    private final int endLine;
    private final LineStatus[] lineStatuses;

    public LineStatuses(int beginLine, int endLine) {
        this.beginLine = beginLine;
        this.endLine = endLine;

        lineStatuses = new LineStatus[endLine - beginLine + 1];
        for (int i = 0; i < lineStatuses.length; i++) {
            lineStatuses[i] = LineStatus.EMPTY;
        }
    }

    public LineStatus getStatus(int lineNumber) {
        Preconditions.checkArgument(lineNumber >= beginLine, "%s < %s", lineNumber, beginLine);
        Preconditions.checkArgument(lineNumber <= endLine, "%s > %s", lineNumber, endLine);

        return lineStatuses[lineNumber - beginLine];
    }

    public void setStatus(int lineNumber, LineStatus status) {
        Preconditions.checkArgument(lineNumber >= beginLine, "%s < %s", lineNumber, beginLine);
        Preconditions.checkArgument(lineNumber <= endLine, "%s > %s", lineNumber, endLine);

        lineStatuses[lineNumber - beginLine] = status;
    }

    public void setStatus(int[] lineNumbers, LineStatus status) {
        for (int lineNumber : lineNumbers) {
            setStatus(lineNumber, status);
        }
    }
}
