/*
 * SETTE - Symbolic Execution based Test Tool Evaluator
 *
 * SETTE is a tool to help the evaluation and comparison of symbolic execution
 * based test input generator tools.
 *
 * Budapest University of Technology and Economics (BME)
 *
 * Authors: Lajos Cseppentő <lajos.cseppento@inf.mit.bme.hu>, Zoltán Micskei
 * <micskeiz@mit.bme.hu>
 *
 * Copyright 2014
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package hu.bme.mit.sette.common.model.runner.xml;

import hu.bme.mit.sette.common.model.runner.ParameterType;
import hu.bme.mit.sette.common.validator.GeneralValidator;

import org.apache.commons.lang3.StringUtils;
import org.simpleframework.xml.Element;

/**
 * Represents a simple parameter element.
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
     * @param pType
     *            the type of the parameter
     * @param pValue
     *            the value of the parameter
     */
    public ParameterElement(final ParameterType pType,
            final String pValue) {
        super(pType);
        value = pValue;
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
     * Sets the value of the parameter. Uses {@link String#valueOf(Object)}.
     *
     * @param pValue
     *            the new value of the parameter
     */
    public void setValue(final Object pValue) {
        value = String.valueOf(pValue);
    }

    /*
     * (non-Javadoc)
     *
     * @see hu.bme.mit.sette.common.model.runner.xml.AbstractParameterElement#
     * validateSubclass(hu.bme.mit.sette.common.validator.GeneralValidator)
     */
    @Override
    protected void validate2(final GeneralValidator validator) {
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
            throw new RuntimeException("Unhandled parameter type: "
                    + getType());
        }
    }
}
