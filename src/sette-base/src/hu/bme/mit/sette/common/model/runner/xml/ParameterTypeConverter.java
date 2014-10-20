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

import org.simpleframework.xml.convert.Converter;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;

/**
 * Provides conversion between {@link ParameterType} and string.
 */
public final class ParameterTypeConverter implements
Converter<ParameterType> {
    /*
     * (non-Javadoc)
     *
     * @see
     * org.simpleframework.xml.convert.Converter#read(org.simpleframework.xml
     * .stream.InputNode)
     */
    @Override
    public ParameterType read(final InputNode node) throws Exception {
        return ParameterType.fromString(node.getValue().trim());
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.simpleframework.xml.convert.Converter#write(org.simpleframework.xml
     * .stream.OutputNode, java.lang.Object)
     */
    @Override
    public void write(final OutputNode node, final ParameterType value)
            throws Exception {
        node.setValue(value.toString());
    }
}
