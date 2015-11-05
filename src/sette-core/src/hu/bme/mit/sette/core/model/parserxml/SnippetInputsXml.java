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
package hu.bme.mit.sette.core.model.parserxml;

import java.util.List;

import org.apache.commons.lang3.Validate;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import hu.bme.mit.sette.core.model.runner.ResultType;
import hu.bme.mit.sette.core.validator.ValidationException;
import hu.bme.mit.sette.core.validator.Validator;

/**
 * Represents an XML file containing the generated inputs for a snippet.
 */
@Root(name = "setteSnippetInputs")
public final class SnippetInputsXml extends SnippetBaseXml {
    /** The generated inputs. */
    @ElementList(name = "generatedInputs", entry = "input", type = InputElement.class,
            required = false)
    private List<InputElement> generatedInputs = null;

    /** The number of the generated inputs. */
    @Element(name = "generatedInputCount", required = false)
    private Integer generatedInputCount = null;

    /**
     * Instantiates a new snippet inputs XML.
     */
    public SnippetInputsXml() {
        super();
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
        Validate.isTrue(generatedInputCount == null,
                "The generated input count property must be set to null before setting this variable");
        this.generatedInputs = generatedInputs;
    }

    /**
     * Gets the generated input count.
     *
     * @return the generated input count
     */
    public Integer getGeneratedInputCount() {
        if (generatedInputs != null) {
            return generatedInputs.size();
        } else {
            return generatedInputCount;
        }
    }

    /**
     * Sets the generated input count if the list of generated inputs is <code>null</code>.
     *
     * @param generatedInputCount
     *            the new generated input count
     */
    public void setGeneratedInputCount(Integer generatedInputCount) {
        Validate.isTrue(generatedInputs == null,
                "The list of generated inputs must be set to null before setting this variable");
        this.generatedInputCount = generatedInputCount;
    }

    @Override
    protected void validate2(Validator validator) {
        if (getResultType() == ResultType.NC || getResultType() == ResultType.C) {
            // TODO enable back?
            // validator.addError("The result type must not be NC or C");
        }

        if (getResultType() == null) {
            validator.addError("The result type must not be null");
        }

        if (generatedInputCount != null && generatedInputs != null) {
            validator.addError(
                    "Only one of the generatedInputs and generatedInputCount properties can be specified");
        }

        if (generatedInputs == null) {
            // tag is omitted
        } else {
            for (InputElement input : generatedInputs) {
                try {
                    input.validate();
                } catch (ValidationException ex) {
                    // FIXME
                    // v.addChild(ex.getValidator());
                    // throw ex;
                    validator.addError(ex.getMessage());
                }
            }
        }

        switch (getResultType()) {
            case NA:
            case EX:
            case TM:
                if (getGeneratedInputCount() != null && getGeneratedInputCount() != 0) {
                    validator.addError(
                            "N/A, EX and T/M results must not have any generated inputs");
                }
                break;

            case S:
            case NC:
            case C:
                if (getGeneratedInputCount() == null && getGeneratedInputCount() < 1) {
                    validator.addError("S, NC and C results must have at least one input");
                }
                break;

            default:
                // NOTE
                throw new RuntimeException("UNKNOWN RESULT TYPE");
        }
    }
}
