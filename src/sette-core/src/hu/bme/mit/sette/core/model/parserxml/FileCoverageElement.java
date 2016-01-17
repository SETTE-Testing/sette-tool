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
package hu.bme.mit.sette.core.model.parserxml;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.simpleframework.xml.Element;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;

import hu.bme.mit.sette.core.validator.ValidationException;
import hu.bme.mit.sette.core.validator.Validator;

/**
 * Represents a file coverage element.
 */
public class FileCoverageElement implements XmlElement {
    /** The name. */
    @Element(name = "name", data = true)
    private String name;

    /** The fully covered lines. */
    @Element(name = "fullyCoveredLines", data = true)
    private String fullyCoveredLines;

    /** The partially covered lines. */
    @Element(name = "partiallyCoveredLines", data = true)
    private String partiallyCoveredLines;

    /** The not covered lines. */
    @Element(name = "notCoveredLines", data = true)
    private String notCoveredLines;

    /**
     * Instantiates a file coverage element.
     */
    public FileCoverageElement() {
    }

    /**
     * Gets the name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name.
     *
     * @param name
     *            the new name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the fully covered lines.
     *
     * @return the fully covered lines
     */
    public String getFullyCoveredLines() {
        return fullyCoveredLines;
    }

    /**
     * Sets the fully covered lines.
     *
     * @param fullyCoveredLines
     *            the new fully covered lines
     */
    public void setFullyCoveredLines(String fullyCoveredLines) {
        this.fullyCoveredLines = fullyCoveredLines;
    }

    /**
     * Gets the partially covered lines.
     *
     * @return the partially covered lines
     */
    public String getPartiallyCoveredLines() {
        return partiallyCoveredLines;
    }

    /**
     * Sets the partially covered lines.
     *
     * @param partiallyCoveredLines
     *            the new partially covered lines
     */
    public void setPartiallyCoveredLines(String partiallyCoveredLines) {
        this.partiallyCoveredLines = partiallyCoveredLines;
    }

    /**
     * Gets the not covered lines.
     *
     * @return the not covered lines
     */
    public String getNotCoveredLines() {
        return notCoveredLines;
    }

    /**
     * Sets the not covered lines.
     *
     * @param notCoveredLines
     *            the new not covered lines
     */
    public void setNotCoveredLines(String notCoveredLines) {
        this.notCoveredLines = notCoveredLines;
    }

    @Override
    public void validate() throws ValidationException {
        Validator v = new Validator(this);

        // TODO validate notnull
        // TODO lines can be blank respectively

        Set<Integer> setA = validateAndGetLineNumbers(fullyCoveredLines, v);
        Set<Integer> setB = validateAndGetLineNumbers(partiallyCoveredLines, v);
        Set<Integer> setC = validateAndGetLineNumbers(notCoveredLines, v);

        Collection<Integer> intersection1 = Sets.intersection(setA, setB);
        Collection<Integer> intersection2 = Sets.intersection(setA, setC);
        Collection<Integer> intersection3 = Sets.intersection(setB, setC);

        TreeSet<Integer> union = new TreeSet<>();
        union.addAll(intersection1);
        union.addAll(intersection2);
        union.addAll(intersection3);

        if (!union.isEmpty()) {
            // TODO enhance message
            v.addError("No common elements are allowed ("
                    + Joiner.on(", ").useForNull("null").join(union) + ")");
        }

        v.validate();
    }

    /**
     * Validates and gets the line numbers.
     *
     * @param lines
     *            the lines
     * @param validator
     *            a validator
     * @return the line numbers
     */
    private static Set<Integer> validateAndGetLineNumbers(String lines, Validator<?> validator) {
        List<String> parts = Splitter.onPattern("\\s+").omitEmptyStrings().splitToList(lines);
        Set<Integer> lineNumbers = new HashSet<>();

        // TODO enhance validator messages
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }

            try {
                int lineNumber = Integer.parseInt(part);

                if (lineNumber >= 1) {
                    if (!lineNumbers.contains(lineNumber)) {
                        lineNumbers.add(lineNumber);
                    } else {
                        validator.addError("Duplicates are not allowed");
                    }
                } else {
                    validator.addError("Only positive integers are allowed");
                }
            } catch (NumberFormatException ex) {
                validator.addError("Only positive integers are allowed (invalid: " + part + ")");
            }
        }

        return lineNumbers;
    }
}
