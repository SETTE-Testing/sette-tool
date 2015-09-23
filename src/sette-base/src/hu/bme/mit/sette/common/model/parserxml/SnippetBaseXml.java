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
package hu.bme.mit.sette.common.model.parserxml;

import hu.bme.mit.sette.common.model.runner.ResultType;
import hu.bme.mit.sette.common.validator.GeneralValidator;
import hu.bme.mit.sette.common.validator.exceptions.ValidatorException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.convert.Convert;

/**
 * Base class for runner project snippet XML files.
 */
public abstract class SnippetBaseXml implements XmlElement {
    /** The name of the tool. */
    @Element(name = "tool")
    private String toolName;

    /** The snippet project element. */
    @Element(name = "snippetProject")
    private SnippetProjectElement snippetProjectElement;

    /** The snippet element. */
    @Element(name = "snippet")
    private SnippetElement snippetElement;

    /** The result type. */
    @Element(name = "result")
    @Convert(ResultTypeConverter.class)
    private ResultType resultType;

    /**
     * Instantiates a new snippet base xml.
     */
    public SnippetBaseXml() {
    }

    /**
     * Gets the name of the tool.
     *
     * @return the name of the tool
     */
    public final String getToolName() {
        return toolName;
    }

    /**
     * Sets the name of the tool.
     *
     * @param toolName
     *            the new name of the tool
     */
    public final void setToolName(String toolName) {
        this.toolName = toolName;
    }

    /**
     * Gets the snippet project element.
     *
     * @return the snippet project element
     */
    public final SnippetProjectElement getSnippetProjectElement() {
        return snippetProjectElement;
    }

    /**
     * Sets the snippet project element.
     *
     * @param snippetProjectElement
     *            the new snippet project element
     */
    public final void setSnippetProjectElement(SnippetProjectElement snippetProjectElement) {
        this.snippetProjectElement = snippetProjectElement;
    }

    /**
     * Gets the snippet element.
     *
     * @return the snippet element
     */
    public final SnippetElement getSnippetElement() {
        return snippetElement;
    }

    /**
     * Sets the snippet element.
     *
     * @param snippetElement
     *            the new snippet element
     */
    public final void setSnippetElement(SnippetElement snippetElement) {
        this.snippetElement = snippetElement;
    }

    /**
     * Gets the result type.
     *
     * @return the result type
     */
    public final ResultType getResultType() {
        return resultType;
    }

    /**
     * Sets the result type.
     *
     * @param resultType
     *            the new result type
     */
    public final void setResultType(ResultType resultType) {
        Validate.isTrue(this.resultType == null,
                "The result type can be only set once [resultType: %s, newResultType: %s",
                this.resultType, resultType);
        this.resultType = resultType;
    }

    @Override
    public final void validate() throws ValidatorException {
        GeneralValidator validator = new GeneralValidator(this);

        if (StringUtils.isBlank(toolName)) {
            validator.addException("The tool name must not be blank");
        }

        if (snippetProjectElement == null) {
            validator.addException("The snippet project element must not be null");
        } else {
            try {
                snippetProjectElement.validate();
            } catch (ValidatorException ex) {
                validator.addChild(ex.getValidator());
            }
        }

        if (snippetElement == null) {
            validator.addException("The snippet element must not be null");
        } else {
            try {
                snippetElement.validate();
            } catch (ValidatorException ex) {
                validator.addChild(ex.getValidator());
            }
        }

        if (resultType == null) {
            validator.addException("The result type must not be null");
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
    protected abstract void validate2(GeneralValidator validator);
}
