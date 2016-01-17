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
package hu.bme.mit.sette.tools.jpet.xmlparser;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
  

final class XmlTag {
    private final XmlTagType type;
    private final boolean isOpening;

    private XmlTag(XmlTagType type, boolean isOpening) {
        Validate.notNull(type, "The type must not be null");

        this.type = type;
        this.isOpening = isOpening;
    }

    public XmlTagType getType() {
        return type;
    }

    public boolean isOpening() {
        return isOpening;
    }

    public boolean isClosing() {
        return !isOpening;
    }

    public static XmlTag createOpeningTag(XmlTagType type) {
        return new XmlTag(type, true);
    }

    public static XmlTag createOpeningTag(String type) {
        return createOpeningTag(XmlTagType.fromString(type));
    }

    public static XmlTag createClosingTag(XmlTagType type) {
        return new XmlTag(type, false);
    }

    public static XmlTag createClosingTag(String type) {
        return createClosingTag(XmlTagType.fromString(type));
    }

    @Override
    public String toString() {
        if (isOpening) {
            return type.toString();
        } else {
            return type.closingToString();
        }
    }

    public void validateParentTag(XmlTag parentTag) {
        XmlTagType[] validParentTagTypes = type.getValidParentTagTypes();

        Validate.notEmpty(validParentTagTypes,
                "The array of valid parent tag types must not be empty or null");

        List<String> validParentTagTypesAsString = new ArrayList<>();

        for (XmlTagType validParentTagType : validParentTagTypes) {
            if (parentTag != null && parentTag.getType() == validParentTagType) {
                // parent is not ROOT and type OK
                return;
            } else if (parentTag == null && validParentTagType == null) {
                // parent is ROOT and type OK
                return;
            }

            if (validParentTagType == null) {
                validParentTagTypesAsString.add("ROOT");
            } else {
                validParentTagTypesAsString.add(validParentTagType.toString());
            }
        }

        // invalid
        String message = String.format("The parent of %s must be in %s, not in %s", this,
                StringUtils.join(validParentTagTypesAsString, " or "),
                parentTag == null ? "ROOT" : parentTag.toString());
        Validate.isTrue(false, message);
    }
}