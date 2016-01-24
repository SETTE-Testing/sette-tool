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
package tester;

import java.io.StringWriter;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.convert.AnnotationStrategy;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.stream.Format;

import hu.bme.mit.sette.core.model.parserxml.ArrayParameterElement;
import hu.bme.mit.sette.core.model.parserxml.InputElement;
import hu.bme.mit.sette.core.model.parserxml.ParameterElement;
import hu.bme.mit.sette.core.model.parserxml.SnippetElement;
import hu.bme.mit.sette.core.model.parserxml.SnippetInputsXml;
import hu.bme.mit.sette.core.model.parserxml.SnippetProjectElement;
import hu.bme.mit.sette.core.model.runner.ParameterType;
import hu.bme.mit.sette.core.model.runner.ResultType;
import hu.bme.mit.sette.core.validator.ValidationException;

public final class SnippetInputsXmlMain {
    public static void main(String[] args) throws Exception {
        SnippetInputsXml snippetInputsXml = new SnippetInputsXml();
        snippetInputsXml.setToolName("MyTool");
        snippetInputsXml.setSnippetProjectElement(new SnippetProjectElement());
        snippetInputsXml.getSnippetProjectElement()
                .setBaseDirPath("/data/workspace/dev/SETTE-Snippets");
        snippetInputsXml.setSnippetElement(new SnippetElement());
        snippetInputsXml.getSnippetElement()
                .setContainerName("hu.bme.mit.sette.snippets.MyContainer");
        snippetInputsXml.getSnippetElement().setName("mySnippet");
        snippetInputsXml.setResultType(ResultType.S);

        InputElement i1 = new InputElement();
        i1.getParameters().add(new ParameterElement(ParameterType.INT, "0"));
        i1.getParameters().add(new ParameterElement(ParameterType.DOUBLE, "1.0"));

        InputElement i2 = new InputElement();
        i2.getParameters().add(new ParameterElement(ParameterType.FLOAT, "1.2"));
        i2.getParameters().add(new ParameterElement(ParameterType.CHAR, "c"));

        InputElement i3 = new InputElement();
        ArrayParameterElement i3p1 = new ArrayParameterElement(ParameterType.INT);
        i3p1.getElements().add("1");
        i3p1.getElements().add("2");
        ArrayParameterElement i3p2 = new ArrayParameterElement(ParameterType.CHAR);
        i3p2.getElements().add("a");
        i3p2.getElements().add("b");
        i3p2.getElements().add("c");
        i3.getParameters().add(i3p1);
        i3.getParameters().add(i3p2);

        InputElement i4 = new InputElement();
        i4.getParameters().add(new ParameterElement(ParameterType.EXPRESSION, "\"str\""));
        i4.getParameters().add(new ParameterElement(ParameterType.EXPRESSION,
                "new String(\"str\")+\"str1\"+\"str2\".concat(\"str3\")"));

        InputElement i5 = new InputElement();
        i5.getParameters().add(new ParameterElement(ParameterType.EXPRESSION, "\"str\""));
        i5.getParameters().add(new ParameterElement(ParameterType.EXPRESSION,
                "new String(\"str\")+\"str1\"+\"str2\".concat(\"str3\")"));

        InputElement i6 = new InputElement();
        ArrayParameterElement i6p1 = new ArrayParameterElement(ParameterType.EXPRESSION);
        i6p1.getElements().add("\"str1\"");
        i6p1.getElements().add("\"str2\"");
        i6.getParameters().add(i6p1);
        i6.getParameters()
                .add(new ParameterElement(ParameterType.EXPRESSION, "{\"str1\", \"str2\"}"));

        // TODO is factory needed anymore after heap was added?
        // InputElement i7 = new InputElement();
        // String i7p1Str = "String param = \"str1\";\n"
        // + "return param;";
        // String i7p2Str = "String param = \"str2\";\n"
        // + "return param;";
        // i7.getParameters().add(
        // new SimpleParameterElement(ParameterType.FACTORY,
        // i7p1Str));
        // i7.getParameters().add(
        // new SimpleParameterElement(ParameterType.FACTORY,
        // i7p2Str));
        // i7.setExpected(ArrayIndexOutOfBoundsException.class.getName());

        InputElement i8 = new InputElement();
        String i8Heap = "String p = \"1\";\n";
        i8Heap += "String p1 = p + \"1\";\n";
        i8Heap += "String p2 = p + \"2\";\n";
        i8.setHeap(i8Heap);
        i8.getParameters().add(new ParameterElement(ParameterType.EXPRESSION, "p1"));
        i8.getParameters().add(new ParameterElement(ParameterType.EXPRESSION, "p2"));
        i8.setExpected(ArrayIndexOutOfBoundsException.class.getName());

        snippetInputsXml.getGeneratedInputs().add(i1);
        snippetInputsXml.getGeneratedInputs().add(i2);
        snippetInputsXml.getGeneratedInputs().add(i3);
        snippetInputsXml.getGeneratedInputs().add(i4);
        snippetInputsXml.getGeneratedInputs().add(i5);
        snippetInputsXml.getGeneratedInputs().add(i6);
        // TODO is factory needed anymore after heap was added?
        // snippetInputsXml.getGeneratedInputs().add(i7);
        snippetInputsXml.getGeneratedInputs().add(i8);

        try {
            snippetInputsXml.validate();
        } catch (ValidationException ex) {
            System.err.println(ex.getMessage());
            throw new RuntimeException(ex);
        }

        Serializer serializer = new Persister(new AnnotationStrategy(),
                new Format("<?xml version=\"1.0\" encoding= \"UTF-8\" ?>"));
        StringWriter s = new StringWriter();

        try {
            serializer.write(snippetInputsXml, s);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }

        System.out.println(s);

        snippetInputsXml = serializer.read(SnippetInputsXml.class, s.getBuffer().toString());
        s = new StringWriter();

        serializer.write(snippetInputsXml, s);

    }
}
