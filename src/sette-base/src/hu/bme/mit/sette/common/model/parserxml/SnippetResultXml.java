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
package hu.bme.mit.sette.common.model.parserxml;

import hu.bme.mit.sette.annotations.SetteRequiredStatementCoverage;
import hu.bme.mit.sette.common.model.runner.ResultType;
import hu.bme.mit.sette.common.validator.GeneralValidator;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/**
 * Represents an XML file containing the result of the generation for a snippet.
 */
@Root(name = "setteSnippetResult")
public final class SnippetResultXml extends SnippetBaseXml {
    /** The statement coverage. */
    @Element(name = "statementCoverage")
    private double statementCoverage;

    /**
     * Instantiates a new snippet result XML.
     */
    public SnippetResultXml() {
    }

    /**
     * Gets the statement coverage.
     *
     * @return the statement coverage
     */
    public double getStatementCoverage() {
        return statementCoverage;
    }

    /**
     * Sets the statement coverage.
     *
     * @param statementCoverage
     *            the new statement coverage
     */
    public void setStatementCoverage(double statementCoverage) {
        this.statementCoverage = statementCoverage;
    }

    @Override
    protected void validate2(GeneralValidator v) {
        if (getResultType() == ResultType.S) {
            v.addException("The result type must not be S");
        }

        if (statementCoverage < SetteRequiredStatementCoverage.MIN
                || statementCoverage > SetteRequiredStatementCoverage.MAX) {
            v.addException(String.format("The statement coverage must be between %.2f%% and %.2f%%",
                    SetteRequiredStatementCoverage.MIN, SetteRequiredStatementCoverage.MAX));
        }
    }
}
