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

import org.simpleframework.xml.ElementList;

import hu.bme.mit.sette.core.model.runner.ParameterType;
import hu.bme.mit.sette.core.util.ListUtils;
import hu.bme.mit.sette.core.validator.Validator;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents an array parameter element.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public final class ArrayParameterElement extends AbstractParameterElement {
    /** The elements of the array parameter. */
    @ElementList(inline = true, data = true)
    private ArrayList<String> elements = new ArrayList<>();

    /**
     * Instantiates a new array parameter element.
     */
    public ArrayParameterElement() {
        // default constructor is required for deserialization
    }

    /**
     * Instantiates a new array parameter element.
     *
     * @param type
     *            the type of the parameter
     */
    public ArrayParameterElement(ParameterType type) {
        super(type);
    }

    /**
     * Sets the list of elements.
     *
     * @param elements
     *            the new list of elements
     */
    public void setElements(List<String> elements) {
        this.elements = ListUtils.asArrayList(elements);
    }

    @Override
    void validateValue(Validator<?> validator) {
        if (getType() != null) {
            // TODO validate elements for type
        }
    }

    @Override
    public Object getValueAsObject() {
        // TODO implement
        throw new RuntimeException("NOT SUPPORTED");
    }
}
