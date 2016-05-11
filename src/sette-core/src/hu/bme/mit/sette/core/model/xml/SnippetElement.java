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

import org.simpleframework.xml.Element;

import com.google.common.base.Strings;

import hu.bme.mit.sette.core.util.xml.XmlElement;
import hu.bme.mit.sette.core.validator.ValidationException;
import hu.bme.mit.sette.core.validator.Validator;
import lombok.Data;

/**
 * Represents a snippet element.
 */
@Data
public final class SnippetElement implements XmlElement {
    /** The name of the snippet container. */
    @Element(data = true)
    private String containerName;

    /** The name of the code snippet. */
    @Element(data = true)
    private String name;

    /**
     * Instantiates a new snippet element.
     */
    public SnippetElement() {
        // default constructor is required for deserialization
    }

    /**
     * Instantiates a new snippet element.
     *
     * @param containerName
     *            the name of the snippet container
     * @param name
     *            the name of the code snippet
     */
    public SnippetElement(String containerName, String name) {
        this.containerName = containerName;
        this.name = name;
    }

    @Override
    public void validate() throws ValidationException {
        Validator<SnippetElement> v = Validator.of(this);

        if (Strings.isNullOrEmpty(containerName)) {
            v.addError("The container name must not be empty");
        }
        if (Strings.isNullOrEmpty(name)) {
            v.addError("The name must not be empty");
        }

        v.validate();
    }
}
