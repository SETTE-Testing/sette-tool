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

import hu.bme.mit.sette.common.validator.GeneralValidator;
import hu.bme.mit.sette.common.validator.exceptions.ValidatorException;

import org.apache.commons.lang3.StringUtils;
import org.simpleframework.xml.Element;

/**
 * Represents a snippet project element.
 */
public final class SnippetProjectElement implements XmlElement {
    /** The base directory path of the snippet project. */
    @Element(name = "baseDirectory", data = true)
    private String baseDirectoryPath;

    /**
     * Instantiates a new snippet project element.
     */
    public SnippetProjectElement() {
    }

    /**
     * Instantiates a new snippet project element.
     *
     * @param pBaseDirectoryPath
     *            the base directory path of the snippet project
     */
    public SnippetProjectElement(final String pBaseDirectoryPath) {
        baseDirectoryPath = pBaseDirectoryPath;
    }

    /**
     * Gets the base directory path of the snippet project.
     *
     * @return the base directory path of the snippet project
     */
    public String getBaseDirectoryPath() {
        return baseDirectoryPath;
    }

    /**
     * Sets the base directory path of the snippet project.
     *
     * @param pBaseDirectoryPath
     *            the new base directory path of the snippet project
     */
    public void setBaseDirectoryPath(final String pBaseDirectoryPath) {
        baseDirectoryPath = pBaseDirectoryPath;
    }

    /*
     * (non-Javadoc)
     *
     * @see hu.bme.mit.sette.common.model.runner.xml.XmlElement#validate()
     */
    @Override
    public void validate() throws ValidatorException {
        GeneralValidator v = new GeneralValidator(this);

        if (StringUtils.isBlank(baseDirectoryPath)) {
            v.addException("The base directory path must not be blank");
        }

        v.validate();
    }
}
