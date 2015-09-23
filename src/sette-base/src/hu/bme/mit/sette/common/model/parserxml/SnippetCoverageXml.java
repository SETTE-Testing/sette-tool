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
// NOTE revise this file
package hu.bme.mit.sette.common.model.parserxml;

import java.util.ArrayList;
import java.util.List;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import hu.bme.mit.sette.common.model.runner.ResultType;
import hu.bme.mit.sette.common.validator.GeneralValidator;
import hu.bme.mit.sette.common.validator.exceptions.ValidatorException;

/**
 * Represents an XML file containing the coverage information for a snippet.
 */
@Root(name = "setteSnippetCoverage")
public final class SnippetCoverageXml extends SnippetBaseXml {
    /** The achieved coverage. */
    @Element(name = "achievedCoverage")
    private String achievedCoverage;

    /** The coverage. */
    @ElementList(name = "coverage", entry = "file", type = FileCoverageElement.class)
    private List<FileCoverageElement> coverage;

    /**
     * Instantiates a new snippet coverage XML.
     */
    public SnippetCoverageXml() {
        super();
        coverage = new ArrayList<>();
    }

    public String getAchievedCoverage() {
        return achievedCoverage;
    }

    public void setAchievedCoverage(String achievedCoverage) {
        this.achievedCoverage = achievedCoverage;
    }

    /**
     * E.g.: 50.623453 -> 50.62%
     */
    public void setAchievedCoverage(double achievedCoverage) {
        this.achievedCoverage = String.format("%.2f%%", achievedCoverage);
    }

    /**
     * Gets the coverage.
     *
     * @return the coverage
     */
    public List<FileCoverageElement> getCoverage() {
        return coverage;
    }

    /**
     * Sets the coverage.
     *
     * @param coverage
     *            the new coverage
     */
    public void setCoverage(List<FileCoverageElement> coverage) {
        this.coverage = coverage;
    }

    @Override
    protected void validate2(GeneralValidator validator) {
        if (coverage == null) {
            validator.addException("The coverage must not be null");
        } else {
            if (getResultType() == ResultType.S) {
                validator.addException("The result of an execution must not be S at this point!");
            }

            if (achievedCoverage == null) {
                validator.addException("The achieved coverage of an execution must be set!");
            }

            // if (statementCoverage < SetteRequiredStatementCoverage.MIN
            // || statementCoverage > SetteRequiredStatementCoverage.MAX) {
            // v.addException(String.format("The statement coverage must be between %.2f%% and
            // %.2f%%",
            // SetteRequiredStatementCoverage.MIN, SetteRequiredStatementCoverage.MAX));

            for (FileCoverageElement coverageElement : coverage) {
                try {
                    coverageElement.validate();
                } catch (ValidatorException ex) {
                    validator.addChild(ex.getValidator());
                }
            }
        }
    }
}
