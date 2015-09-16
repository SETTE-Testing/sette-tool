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

import hu.bme.mit.sette.common.validator.GeneralValidator;
import hu.bme.mit.sette.common.validator.exceptions.ValidatorException;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.ElementListUnion;

/**
 * Represents an input element.
 */
public final class InputElement implements XmlElement {
    /** The heap. */
    @Element(name = "heap", data = true, required = false)
    private String heap;

    /** The parameters. */
    @ElementListUnion({
            @ElementList(type = ParameterElement.class, entry = "parameter", inline = true,
                    required = false),
            @ElementList(type = ArrayParameterElement.class, entry = "arrayParameter",
                    inline = true, required = false) })
    private List<AbstractParameterElement> parameters;

    /** The name of the expected exception. */
    @Element(name = "expected", data = true, required = false)
    private String expected;

    /**
     * Instantiates a new input element.
     */
    public InputElement() {
        parameters = new ArrayList<>();
    }

    /**
     * Gets the heap.
     *
     * @return the heap
     */
    public String getHeap() {
        return heap;
    }

    /**
     * Sets the heap.
     *
     * @param heap
     *            the new heap
     */
    public void setHeap(String heap) {
        this.heap = StringUtils.trimToNull(heap);
    }

    /**
     * Gets the list of parameters.
     *
     * @return the list of parameters
     */
    public List<AbstractParameterElement> getParameters() {
        return parameters;
    }

    /**
     * Sets the list of parameters.
     *
     * @param parameters
     *            the new list of parameters
     */
    public void setParameters(List<AbstractParameterElement> parameters) {
        this.parameters = parameters;
    }

    /**
     * Gets the name of the expected exception.
     *
     * @return the name of the expected exception
     */
    public String getExpected() {
        return expected;
    }

    /**
     * Sets the name of the expected exception.
     *
     * @param expected
     *            the new name of the expected exception
     */
    public void setExpected(String expected) {
        this.expected = expected;
    }

    @Override
    public void validate() throws ValidatorException {
        GeneralValidator v = new GeneralValidator(this);

        if (parameters == null) {
            v.addException("The list of parameters must not be null");
        } else if (parameters.contains(null)) {
            v.addException("The list of parameters must not contain a null element");
        }

        for (AbstractParameterElement ape : parameters) {
            try {
                ape.validate();
            } catch (ValidatorException e) {
                v.addChild(e.getValidator());
            }
        }

        v.validate();
    }
}
