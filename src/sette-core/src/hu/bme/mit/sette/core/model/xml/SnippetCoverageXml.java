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
package hu.bme.mit.sette.core.model.xml;

import java.util.ArrayList;
import java.util.List;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.convert.Convert;

import hu.bme.mit.sette.core.model.runner.ResultType;
import hu.bme.mit.sette.core.model.xml.converter.DoublePercentConverter;
import hu.bme.mit.sette.core.util.ListUtils;
import hu.bme.mit.sette.core.validator.ValidationException;
import hu.bme.mit.sette.core.validator.Validator;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents an XML file containing the coverage information for a snippet.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Root(name = "setteSnippetCoverage")
public final class SnippetCoverageXml extends SnippetBaseXml {
    /** The achieved coverage. */
    @Element
    @Convert(DoublePercentConverter.class)
    private Double achievedCoverage;

    /** The coverage. */
    @ElementList(entry = "file")
    private ArrayList<FileCoverageElement> coverage = new ArrayList<>();

    /**
     * Sets the coverage.
     *
     * @param coverage
     *            the new coverage
     */
    public void setCoverage(List<FileCoverageElement> coverage) {
        this.coverage = ListUtils.asArrayList(coverage);
    }

    @Override
    protected void validate2(Validator<?> validator) {
        if (coverage == null) {
            validator.addError("The coverage must not be null");
        } else {
            if (getResultType() == ResultType.S) {
                validator.addError("The result of an execution must not be S at this point!");
            }

            if (achievedCoverage == null) {
                validator.addError("The achieved coverage of an execution must be set!");
            }

            // if (statementCoverage < SetteRequiredStatementCoverage.MIN
            // || statementCoverage > SetteRequiredStatementCoverage.MAX) {
            // v.addError(String.format("The statement coverage must be between %.2f%% and
            // %.2f%%",
            // SetteRequiredStatementCoverage.MIN, SetteRequiredStatementCoverage.MAX));

            for (FileCoverageElement coverageElement : coverage) {
                try {
                    coverageElement.validate();
                } catch (ValidationException ex) {
                    validator.addChild(ex.getValidator());
                }
            }
        }
    }
}
