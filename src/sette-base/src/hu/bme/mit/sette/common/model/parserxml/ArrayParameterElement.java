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

import hu.bme.mit.sette.common.model.runner.ParameterType;
import hu.bme.mit.sette.common.validator.GeneralValidator;

import java.util.ArrayList;
import java.util.List;

import org.simpleframework.xml.ElementList;

/**
 * Represents an array parameter element.
 */
public final class ArrayParameterElement extends AbstractParameterElement {
    /** The elements of the array parameter. */
    @ElementList(inline = true, entry = "element", data = true, type = String.class)
    private List<String> elements;

    /**
     * Instantiates a new array parameter element.
     */
    public ArrayParameterElement() {
        super();
        elements = new ArrayList<>();
    }

    /**
     * Instantiates a new array parameter element.
     *
     * @param type
     *            the type of the parameter
     */
    public ArrayParameterElement(ParameterType type) {
        super(type);
        elements = new ArrayList<>();
    }

    /**
     * Gets the list of elements of the array parameter.
     *
     * @return the list of elements of the array parameter
     */
    public List<String> getElements() {
        return elements;
    }

    /**
     * Sets the list of elements.
     *
     * @param elements
     *            the new list of elements
     */
    public void setElements(List<String> elements) {
        this.elements = elements;
    }

    @Override
    void validateValue(GeneralValidator validator) {
        if (getType() != null) {
            // TODO validate elements for type
        }
    }
}
