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
package hu.bme.mit.sette.core.model.xml.converter;

import java.util.Locale;

import org.simpleframework.xml.convert.Converter;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;

/**
 * Provides conversion between {@link Double} and percent string.
 */
public final class DoublePercentConverter implements Converter<Double> {
    @Override
    public Double read(InputNode node) throws Exception {
        return toDouble(node.getValue());
    }

    @Override
    public void write(OutputNode node, Double value) throws Exception {
        node.setValue(toPercent(value));
    }

    public static final String toPercent(Double value) {
        if (value == null) {
            return null;
        } else {
            return String.format(Locale.ENGLISH, "%.2f%%", value);
        }
    }

    public static final Double toDouble(String percent) {
        if (percent == null) {
            return null;
        } else {
            try {
                if (percent.endsWith("%")) {
                    return Double.parseDouble(percent.substring(0, percent.length() - 1));
                } else {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Not a valid percent value: " + percent);
            }
        }
    }
}
