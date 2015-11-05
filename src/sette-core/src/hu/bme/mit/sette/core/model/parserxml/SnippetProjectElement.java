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

import org.simpleframework.xml.Element;

import com.google.common.base.Strings;

import hu.bme.mit.sette.core.validator.ValidationException;
import hu.bme.mit.sette.core.validator.Validator;
import lombok.Data;

/**
 * Represents a snippet project element.
 */
@Data
public final class SnippetProjectElement implements XmlElement {
    /** The base directory path of the snippet project. */
    @Element(name = "baseDir", data = true)
    private String baseDirPath;

    /**
     * Instantiates a new snippet project element.
     */
    public SnippetProjectElement() {
    }

    /**
     * Instantiates a new snippet project element.
     *
     * @param baseDirPath
     *            the base directory path of the snippet project
     */
    public SnippetProjectElement(String baseDirPath) {
        this.baseDirPath = baseDirPath;
    }

    @Override
    public void validate() throws ValidationException {
        Validator v = new Validator(this);

        if (Strings.isNullOrEmpty(baseDirPath)) {
            v.addError("The base directory path must not be empty");
        }

        v.validate();
    }
}
