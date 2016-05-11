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
import org.simpleframework.xml.Root;
import org.simpleframework.xml.convert.Convert;

import com.google.common.primitives.Doubles;

import hu.bme.mit.sette.core.model.runner.ResultType;
import hu.bme.mit.sette.core.model.xml.converter.DoublePercentConverter;
import hu.bme.mit.sette.core.validator.Validator;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents an XML file containing the result of the generation for a snippet.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Root(name = "setteSnippetResult")
public final class SnippetResultXml extends SnippetBaseXml {
    /** The achieved coverage. */
    @Element(required = false)
    @Convert(DoublePercentConverter.class)
    private Double achievedCoverage;

    public static SnippetResultXml createForWithResult(SnippetInputsXml inputsXml,
            ResultType resultType, Double achievedCoverage) {
        SnippetResultXml ret = new SnippetResultXml();
        ret.setToolName(inputsXml.getToolName());
        ret.setSnippetProjectElement(inputsXml.getSnippetProjectElement());
        ret.setSnippetElement(inputsXml.getSnippetElement());
        ret.setResultType(resultType);
        ret.setAchievedCoverage(achievedCoverage);
        return ret;
    }

    @Override
    protected void validate2(Validator<?> v) {
        if (getResultType() == ResultType.S) {
            v.addError("The result type must not be S");
        }

        if (getResultType() == ResultType.NC || getResultType() == ResultType.C) {
            if (achievedCoverage == null) {
                v.addError("The achieved coverage of an execution must be set if it is NC or C!");
            }
        } else {
            if (achievedCoverage != null) {
                v.addError(
                        "The achieved coverage of an execution must not be set if it is neither NC nor N");
            }
        }
    }
}
