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

import org.apache.commons.lang3.StringUtils;
import org.simpleframework.xml.Element;

import hu.bme.mit.sette.common.model.runner.ParameterType;
import hu.bme.mit.sette.common.validator.GeneralValidator;

/**
 * Represents an XML <code>&lt;parameter></code> which represents a parameter for an input. The
 * element has a type and a value and it is embedded in the <code>&lt;input></code> element (see
 * {@link InputElement}). The class is able to validate the value of the parameter. Example for the
 * corresponding XML:
 * 
 * <pre>
 * <code>
 * &lt;input>
 *   &lt;parameter>
 *     &lt;type>int&lt;/type>
 *     &lt;value>5&lt;/value>
 *   &lt;/parameter>
 *   &lt;parameter>
 *     &lt;type>expression&lt;/type>
 *     &lt;value>&lt;![CDATA[null]]>&lt;/value>
 *   &lt;/parameter>
 * &lt;/input>
 * </code>
 * </pre>
 */
public final class ParameterElement extends AbstractParameterElement {
    /** The value of the parameter. */
    @Element(name = "value", data = true)
    private String value;

    /**
     * Instantiates a new simple parameter element.
     */
    public ParameterElement() {
        super();
    }

    /**
     * Instantiates a new simple parameter element.
     *
     * @param type
     *            the type of the parameter
     * @param value
     *            the value of the parameter
     */
    public ParameterElement(ParameterType type, String value) {
        super(type);
        setValue(value);
    }

    /**
     * Gets the value of the parameter.
     *
     * @return the value of the parameter
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value of the parameter. Uses {@link String#valueOf(Object)} and preserves
     * <code>null</code>.
     *
     * @param value
     *            the new value of the parameter
     */
    public void setValue(Object value) {
        this.value = (value == null ? null : String.valueOf(value));
    }

    @Override
    void validateValue(GeneralValidator validator) {
        switch (getType()) {
            case BYTE:
                // TODO validator exception
                Byte.parseByte(value);
                break;

            case SHORT:
                // TODO validator exception
                Short.parseShort(value);
                break;

            case INT:
                // TODO validator exception
                Integer.parseInt(value);
                break;

            case LONG:
                // TODO validator exception
                Long.parseLong(value);
                break;

            case FLOAT:
                // TODO validator exception
                Float.parseFloat(value);
                break;

            case DOUBLE:
                // TODO validator exception
                Double.parseDouble(value);
                break;

            case BOOLEAN:
                if (!value.equals("true") && !value.equals("false")) {
                    // TODO error handling
                    throw new RuntimeException("Bad boolean: " + value);
                }
                break;

            case CHAR:
                if (value.length() != 1) {
                    // TODO error handling
                    throw new RuntimeException("Bad char: " + value);
                }
                break;

            case EXPRESSION:
                if (StringUtils.isBlank(value)) {
                    // TODO error handling
                    throw new RuntimeException("Blank expression");
                }
                break;

            default:
                // TODO error handling
                throw new RuntimeException("Unhandled parameter type: " + getType());
        }
    }
}
