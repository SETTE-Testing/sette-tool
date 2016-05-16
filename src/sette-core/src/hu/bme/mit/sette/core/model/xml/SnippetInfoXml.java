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
package hu.bme.mit.sette.core.model.xml;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.convert.Convert;

import hu.bme.mit.sette.core.model.xml.converter.PathConverter;
import hu.bme.mit.sette.core.util.ListUtils;
import hu.bme.mit.sette.core.util.xml.XmlElement;
import hu.bme.mit.sette.core.validator.ValidationException;
import hu.bme.mit.sette.core.validator.Validator;
import lombok.Data;

@Data
@Root(name = "setteSnippetInfo")
public final class SnippetInfoXml implements XmlElement {
    @Element
    @Convert(PathConverter.class)
    private Path workingDirectory;

    @ElementList(entry = "c")
    private ArrayList<String> command = new ArrayList<>();

    @Element
    private int exitValue;

    @Element
    private boolean destroyed;

    @Element
    private long elapsedTimeInMs;

    public void setCommand(List<String> command) {
        this.command = ListUtils.asArrayList(command);
    }

    @Override
    public void validate() throws ValidationException {
        Validator<SnippetInfoXml> v = Validator.of(this);
        v.addErrorIfTrue("The working directory must not be null", workingDirectory == null);

        if (command == null) {
            v.addError("The command must not be null");
        } else {
            v.addErrorIfTrue("The command must not be empty", command.isEmpty());
        }

        v.addErrorIfTrue("The elapsed time must not be negative", elapsedTimeInMs < 0);
        v.validate();
    }
}
