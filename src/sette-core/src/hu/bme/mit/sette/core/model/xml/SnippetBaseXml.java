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
import org.simpleframework.xml.convert.Convert;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import hu.bme.mit.sette.core.model.runner.ResultType;
import hu.bme.mit.sette.core.model.xml.converter.ResultTypeConverter;
import hu.bme.mit.sette.core.util.xml.XmlElement;
import hu.bme.mit.sette.core.validator.ValidationException;
import hu.bme.mit.sette.core.validator.Validator;
import lombok.Data;

/**
 * Base class for runner project snippet XML files.
 */
@Data
public abstract class SnippetBaseXml implements XmlElement {
    /** The name of the tool. */
    @Element
    private String toolName;

    /** The snippet project element. */
    @Element
    private SnippetProjectElement snippetProjectElement;

    /** The snippet element. */
    @Element
    private SnippetElement snippetElement;

    /** The result type. */
    @Element
    @Convert(ResultTypeConverter.class)
    private ResultType resultType;

    /**
     * Sets the result type.
     *
     * @param resultType
     *            the new result type
     */
    public final void setResultType(ResultType resultType) {
        Preconditions.checkState(this.resultType == null,
                "The result type can be only set once [resultType: %s, newResultType: %s",
                this.resultType, resultType);
        this.resultType = resultType;
    }

    @Override
    public final void validate() throws ValidationException {
        Validator<?> validator = Validator.of(this);

        if (Strings.isNullOrEmpty(toolName)) {
            validator.addError("The tool name must not be empty");
        }

        if (snippetProjectElement == null) {
            validator.addError("The snippet project element must not be null");
        } else {
            try {
                snippetProjectElement.validate();
            } catch (ValidationException ex) {
                validator.addChild(ex.getValidator());
            }
        }

        if (snippetElement == null) {
            validator.addError("The snippet element must not be null");
        } else {
            try {
                snippetElement.validate();
            } catch (ValidationException ex) {
                validator.addChild(ex.getValidator());
            }
        }

        if (resultType == null) {
            validator.addError("The result type must not be null");
        }

        validate2(validator);

        validator.validate();
    }

    /**
     * Validates the object (phase 2). The overriding subclasses should place their validation
     * exceptions in the container instead of throwing an exception.
     *
     * @param validator
     *            a validator
     */
    protected abstract void validate2(Validator<?> validator);
}
