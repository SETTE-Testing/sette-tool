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

import hu.bme.mit.sette.common.model.runner.ResultType;
import hu.bme.mit.sette.common.validator.GeneralValidator;
import hu.bme.mit.sette.common.validator.exceptions.ValidatorException;

import java.util.ArrayList;
import java.util.List;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

/**
 * Represents an XML file containing the generated inputs for a snippet.
 */
@Root(name = "setteSnippetInputs")
public final class SnippetInputsXml extends SnippetBaseXml {
    /** The generated inputs. */
    @ElementList(name = "generatedInputs", entry = "input", type = InputElement.class,
            required = false)
    private List<InputElement> generatedInputs;

    /**
     * Instantiates a new snippet inputs XML.
     */
    public SnippetInputsXml() {
        super();
        generatedInputs = new ArrayList<InputElement>();
    }

    /**
     * Gets the list of generated inputs.
     *
     * @return the list of generated inputs
     */
    public List<InputElement> getGeneratedInputs() {
        return generatedInputs;
    }

    /**
     * Sets the list of generated inputs.
     *
     * @param generatedInputs
     *            the new list of generated inputs
     */
    public void setGeneratedInputs(List<InputElement> generatedInputs) {
        this.generatedInputs = generatedInputs;
    }

    @Override
    protected void validate2(GeneralValidator validator) {
        if (getResultType() == ResultType.NC || getResultType() == ResultType.C) {
            // TODO enable back?
            // validator.addException("The result type must not be NC or C");
        }

        if (generatedInputs == null) {
            // tag is omitted
        } else {
            for (InputElement input : generatedInputs) {
                try {
                    input.validate();
                } catch (ValidatorException e) {
                    validator.addChild(e.getValidator());
                }
            }
        }
    }
}
