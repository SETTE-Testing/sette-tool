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
package hu.bme.mit.sette.application;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.Messages;
import org.kohsuke.args4j.spi.OneArgumentOptionHandler;
import org.kohsuke.args4j.spi.Setter;

import lombok.NonNull;

public class TimeInMsOptionHandler extends OneArgumentOptionHandler<Integer> {
    public TimeInMsOptionHandler(@NonNull CmdLineParser parser, @NonNull OptionDef option,
            @NonNull Setter<? super Integer> setter) {
        super(parser, option, setter);
    }

    @Override
    protected Integer parse(@NonNull String time) throws CmdLineException {
        if (time.endsWith("ms")) {
            return Integer.parseInt(time.substring(0, time.length() - 2));
        } else if (time.endsWith("s")) {
            return Integer.parseInt(time.substring(0, time.length() - 1)) * 1000;
        } else {
            throw new CmdLineException(owner, Messages.ILLEGAL_OPERAND, option.toString(), time);
        }
    }

    @Override
    public String getDefaultMetaVariable() {
        return "[ 30000ms | 30s ]";
    }
}
