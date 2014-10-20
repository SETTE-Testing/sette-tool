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
package tester;

import hu.bme.mit.sette.common.model.runner.ResultType;
import hu.bme.mit.sette.common.model.runner.xml.SnippetElement;
import hu.bme.mit.sette.common.model.runner.xml.SnippetProjectElement;
import hu.bme.mit.sette.common.model.runner.xml.SnippetResultXml;
import hu.bme.mit.sette.common.validator.exceptions.ValidatorException;

import java.io.StringWriter;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.convert.AnnotationStrategy;
import org.simpleframework.xml.core.Persister;

public final class SnippetResultXmlMain {
    public static void main(final String[] args) throws Exception {
        SnippetResultXml resultXml = new SnippetResultXml();
        resultXml.setToolName("MyTool");
        resultXml.setSnippetProjectElement(new SnippetProjectElement());
        resultXml.getSnippetProjectElement().setBaseDirectoryPath(
                "/data/workspace/dev/SETTE-Snippets");
        resultXml.setSnippetElement(new SnippetElement());
        resultXml.getSnippetElement().setContainerName(
                "hu.bme.mit.sette.snippets.MyContainer");
        resultXml.getSnippetElement().setName("mySnippet");
        resultXml.setStatementCoverage(95.6);
        resultXml.setResultType(ResultType.C);

        try {
            resultXml.validate();
        } catch (ValidatorException e) {
            System.err.println(e.getFullMessage());
            System.exit(-1);
        }

        Serializer serializer = new Persister(new AnnotationStrategy());
        StringWriter s = new StringWriter();

        serializer.write(resultXml, s);

        System.out.println(s);

        resultXml = serializer.read(SnippetResultXml.class, s
                .getBuffer().toString());
        s = new StringWriter();

        serializer.write(resultXml, s);
    }
}
