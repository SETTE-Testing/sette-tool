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
package hu.bme.mit.sette.tools.jpet.xmlparser;

import java.util.Arrays;

import org.apache.commons.lang3.Validate;

enum XmlTagType {
    /** The &lt;pet&gt; (root) tag. */
    PET("pet", (XmlTagType) null),

    /** The &lt;test_case&gt; tag (child of &lt;pet&gt;). */
    TEST_CASE("test_case", XmlTagType.PET),

    /** The &lt;method&gt; tag (child of &lt;test_case&gt;). */
    METHOD("method", XmlTagType.TEST_CASE),
    /** The &lt;args_in&gt; tag (child of &lt;test_case&gt;). */
    ARGS_IN("args_in", XmlTagType.TEST_CASE),
    /** The &lt;heap_in&gt; tag (child of &lt;test_case&gt;). */
    HEAP_IN("heap_in", XmlTagType.TEST_CASE),
    /** The &lt;heap_out&gt; tag (child of &lt;test_case&gt;). */
    HEAP_OUT("heap_out", XmlTagType.TEST_CASE),
    /** The &lt;return&gt; tag (child of &lt;test_case&gt;). */
    RETURN("return", XmlTagType.TEST_CASE),
    /** The &lt;exception_flag&gt; tag (child of &lt;test_case&gt;). */
    EXCEPTION_FLAG("exception_flag", XmlTagType.TEST_CASE),
    /** The &lt;trace&gt; tag (child of &lt;test_case&gt;). */
    TRACE("trace", XmlTagType.TEST_CASE),
    /**
     * The &lt;input_constraints&gt; tag (child of &lt;test_case&gt;).
     */
    INPUT_CONSTRAINTS("input_constraints", XmlTagType.TEST_CASE),
    /**
     * The &lt;output_constraints&gt; tag (child of &lt;test_case&gt;).
     */
    OUTPUT_CONSTRAINTS("output_constraints", XmlTagType.TEST_CASE),
    /** The &lt;params&gt; tag (child of &lt;test_case&gt;). */
    PARAMS("params", XmlTagType.TEST_CASE),

    /**
     * The &lt;elem&gt; tag (child of &lt;heap_in&gt; or &lt;heap_out&gt;).
     */
    ELEM("elem", XmlTagType.HEAP_IN, XmlTagType.HEAP_OUT),
    /**
     * The &lt;num&gt; tag (child of &lt;elem&gt;, denotes the name of the element which is used in
     * &lt;ref&gt;).
     */
    NUM("num", XmlTagType.ELEM),

    /** The &lt;array&gt; tag (child of &lt;elem&gt;). */
    ARRAY("array", XmlTagType.ELEM),
    /** The &lt;type&gt; tag (child of &lt;array&gt;). */
    TYPE("type", XmlTagType.ARRAY),
    /** The &lt;num_elems&gt; tag (child of &lt;array&gt;). */
    NUM_ELEMS("num_elems", XmlTagType.ARRAY),
    /** The &lt;args&gt; tag (child of &lt;array&gt;). */
    ARGS("args", XmlTagType.ARRAY),
    /** The &lt;arg&gt; tag (child of &lt;args&gt;). */
    ARG("arg", XmlTagType.ARGS),

    /** The &lt;object&gt; tag (child of &lt;elem&gt;). */
    OBJECT("object", XmlTagType.ELEM),
    /** The &lt;class_name;&gt; tag (child of &lt;object&gt;). */
    CLASS_NAME("class_name", XmlTagType.OBJECT),
    /** The &lt;fields&gt; tag (child of &lt;object&gt;). */
    FIELDS("fields", XmlTagType.OBJECT),
    /** The &lt;field&gt; tag (child of &lt;fields&gt;). */
    FIELD("field", XmlTagType.FIELDS),
    /** The &lt;field_name&gt; tag (child of &lt;field&gt;). */
    FIELD_NAME("field_name", XmlTagType.FIELD),

    /** The &lt;data&gt; tag (denotes a data value). */
    DATA("data", XmlTagType.ARGS_IN, XmlTagType.FIELD),
    /** The &lt;ref&gt; tag (denotes a reference to an array or object). */
    REF("ref", XmlTagType.ARGS_IN, XmlTagType.ARGS, XmlTagType.FIELD);

    /** The value of the XML attribute. */
    private final String tagName;
    private final XmlTagType[] validParentTagTypes;

    /**
     * Initialises the instance.
     *
     * @param tagName
     *            The name of the XML tag.
     */
    private XmlTagType(String tagName, XmlTagType... validParentTagTypes) {
        Validate.notEmpty(validParentTagTypes,
                "The array of valid parent tag types must not be empty or null");

        this.tagName = tagName;
        this.validParentTagTypes = validParentTagTypes;
    }

    /**
     * Returns the name of the XML tag.
     *
     * @return The name of the XML tag.
     */
    public String getTagName() {
        return tagName;
    }

    public XmlTagType[] getValidParentTagTypes() {
        return validParentTagTypes;
    }

    /**
     * Parses the given tag name into a {@link XmlTagType}.
     *
     * @param tagName
     *            the tag name
     * @return the tag type
     */
    public static XmlTagType fromString(String tagName) {
        Validate.notBlank(tagName, "The tag name must not be blank");

        for (XmlTagType pt : XmlTagType.values()) {
            if (pt.tagName.equalsIgnoreCase(tagName) || pt.name().equalsIgnoreCase(tagName)) {
                return pt;
            }
        }

        String message = String.format("Invalid tag name (tag name: [%s], valid tag names: [%s]",
                tagName, Arrays.toString(XmlTagType.values()));
        throw new IllegalArgumentException(message);
    }

    @Override
    public String toString() {
        return '<' + tagName + '>';
    }

    public String closingToString() {
        return "</" + tagName + '>';
    }
}