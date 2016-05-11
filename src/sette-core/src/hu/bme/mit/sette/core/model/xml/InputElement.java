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
import org.simpleframework.xml.ElementListUnion;

import hu.bme.mit.sette.core.util.ListUtils;
import hu.bme.mit.sette.core.util.xml.XmlElement;
import hu.bme.mit.sette.core.validator.ValidationException;
import hu.bme.mit.sette.core.validator.Validator;
import lombok.Data;

/**
 * Represents an input element.
 */
@Data
public final class InputElement implements XmlElement {
    /** The heap. */
    @Element(data = true, required = false)
    private String heap;

    /** The parameters. */
    @ElementListUnion({
            @ElementList(type = ParameterElement.class, entry = "parameter", inline = true,
                    required = false),
            @ElementList(type = ArrayParameterElement.class, entry = "arrayParameter",
                    inline = true, required = false) })
    private ArrayList<AbstractParameterElement> parameters = new ArrayList<>();

    /** The name of the expected exception. */
    @Element(data = true, required = false)
    private String expected;

    /**
     * Sets the list of parameters.
     *
     * @param parameters
     *            the new list of parameters
     */
    public void setParameters(List<AbstractParameterElement> parameters) {
        this.parameters = ListUtils.asArrayList(parameters);
    }

    @Override
    public void validate() throws ValidationException {
        Validator<InputElement> v = Validator.of(this);

        if (parameters == null) {
            v.addError("The list of parameters must not be null");
        } else if (parameters.contains(null)) {
            v.addError("The list of parameters must not contain a null element");
        }

        for (AbstractParameterElement ape : parameters) {
            try {
                ape.validate();
            } catch (ValidationException ex) {
                v.addChild(ex.getValidator());
            }
        }

        v.validate();
    }
}
