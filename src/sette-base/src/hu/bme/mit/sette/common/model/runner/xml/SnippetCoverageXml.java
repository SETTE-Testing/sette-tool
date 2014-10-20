/*
 * SETTE - Symbolic Execution based Test Tool Evaluator
 *
 * SETTE is a tool to help the evaluation and comparison of symbolic execution
 * based test input generator tools.
 *
 * Budapest University of Technology and Economics (BME)
 *
 * Authors: Lajos Cseppentő <lajos.cseppento@inf.mit.bme.hu>, Zoltán Micskei
 * <micskeiz@mit.bme.hu>
 *
 * Copyright 2014
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package hu.bme.mit.sette.common.model.runner.xml;

import hu.bme.mit.sette.common.validator.GeneralValidator;
import hu.bme.mit.sette.common.validator.exceptions.ValidatorException;

import java.util.ArrayList;
import java.util.List;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

/**
 * Represents an XML file containing the coverage information for a snippet.
 */
@Root(name = "setteSnipeptCoverage")
public final class SnippetCoverageXml extends SnippetBaseXml {
    /** The coverage. */
    @ElementList(name = "coverage", entry = "file",
            type = FileCoverageElement.class)
    private List<FileCoverageElement> coverage;

    /**
     * Instantiates a new snippet coverage XML.
     */
    public SnippetCoverageXml() {
        super();
        coverage = new ArrayList<>();
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

    /*
     * (non-Javadoc)
     *
     * @see
     * hu.bme.mit.sette.common.model.runner.xml.SnippetBaseXml#validate2(hu.
     * bme.mit.sette.common.validator.GeneralValidator)
     */
    @Override
    protected void validate2(GeneralValidator validator) {
        if (coverage == null) {
            validator
            .addException("The coverage " + "must not be null");
        } else {
            for (FileCoverageElement coverageElement : coverage) {
                try {
                    coverageElement.validate();
                } catch (ValidatorException e) {
                    validator.addChild(e.getValidator());
                }
            }
        }
    }
}
