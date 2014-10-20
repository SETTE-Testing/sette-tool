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
import hu.bme.mit.sette.common.validator.exceptions.ValidatorException;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.convert.Convert;

/**
 * Base class for different parameter elements.
 */
public abstract class AbstractParameterElement implements XmlElement {
    /** The type of the parameter. */
    @Element(name = "type")
    @Convert(ParameterTypeConverter.class)
    private ParameterType type;

    /**
     * Instantiates a new abstract parameter element.
     */
    public AbstractParameterElement() {
    }

    /**
     * Instantiates a new abstract parameter element.
     *
     * @param pType
     *            the type of the parameter
     */
    public AbstractParameterElement(final ParameterType pType) {
        type = pType;
    }

    /**
     * Gets the type of the parameter.
     *
     * @return the type of the parameter
     */
    public final ParameterType getType() {
        return type;
    }

    /**
     * Sets the type of the parameter.
     *
     * @param pType
     *            the new type of the parameter
     */
    public final void setType(final ParameterType pType) {
        type = pType;
    }

    /*
     * (non-Javadoc)
     *
     * @see hu.bme.mit.sette.common.model.runner.xml.XmlElement#validate()
     */
    @Override
    public final void validate() throws ValidatorException {
        GeneralValidator validator = new GeneralValidator(this);

        if (type == null) {
            validator.addException("The type must not be null");
        }

        validate2(validator);

        validator.validate();
    }

    /**
     * Validates the object (phase 2). The overriding subclasses should place
     * their validation exceptions in the container instead of throwing an
     * exception.
     *
     * @param validator
     *            a validator
     */
    protected abstract void validate2(GeneralValidator validator);
}
