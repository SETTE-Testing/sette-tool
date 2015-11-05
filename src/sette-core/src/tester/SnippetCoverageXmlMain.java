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
package tester;

import java.io.StringWriter;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.convert.AnnotationStrategy;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.stream.Format;

import hu.bme.mit.sette.core.model.parserxml.FileCoverageElement;
import hu.bme.mit.sette.core.model.parserxml.SnippetCoverageXml;
import hu.bme.mit.sette.core.model.parserxml.SnippetElement;
import hu.bme.mit.sette.core.model.parserxml.SnippetProjectElement;
import hu.bme.mit.sette.core.model.runner.ResultType;
import hu.bme.mit.sette.core.validator.ValidationException;

public final class SnippetCoverageXmlMain {
    public static void main(String[] args) throws Exception {
        SnippetCoverageXml snippetCoverageXml = new SnippetCoverageXml();
        snippetCoverageXml.setToolName("MyTool");
        snippetCoverageXml.setSnippetProjectElement(new SnippetProjectElement());
        snippetCoverageXml.getSnippetProjectElement()
                .setBaseDirPath("/data/workspace/dev/SETTE-Snippets");
        snippetCoverageXml.setSnippetElement(new SnippetElement());
        snippetCoverageXml.getSnippetElement()
                .setContainerName("hu.bme.mit.sette.snippets.MyContainer");
        snippetCoverageXml.getSnippetElement().setName("mySnippet");
        snippetCoverageXml.setResultType(ResultType.S);

        FileCoverageElement fce1 = new FileCoverageElement();
        fce1.setName("hu.bme.mit.sette.snippets.MyContainer");
        fce1.setFullyCoveredLines("1 2 3");
        fce1.setPartiallyCoveredLines("4 5");
        fce1.setNotCoveredLines("6 7 8");

        FileCoverageElement fce2 = new FileCoverageElement();
        fce2.setName("hu.bme.mit.sette.snippets.MyDep");
        fce2.setFullyCoveredLines("1 2 10");
        fce2.setPartiallyCoveredLines("3 4 9 5 20");
        fce2.setNotCoveredLines("6 7 8 30");

        snippetCoverageXml.getCoverage().add(fce1);
        snippetCoverageXml.getCoverage().add(fce2);

        try {
            snippetCoverageXml.validate();
        } catch (ValidationException ex) {
            System.err.println(ex.getMessage());
            System.exit(-1);
        }

        Serializer serializer = new Persister(new AnnotationStrategy(),
                new Format("<?xml version=\"1.0\" encoding= \"UTF-8\" ?>"));
        StringWriter s = new StringWriter();

        try {
            serializer.write(snippetCoverageXml, s);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        }

        System.out.println(s);

        snippetCoverageXml = serializer.read(SnippetCoverageXml.class, s.getBuffer().toString());
        s = new StringWriter();

        serializer.write(snippetCoverageXml, s);
    }
}
