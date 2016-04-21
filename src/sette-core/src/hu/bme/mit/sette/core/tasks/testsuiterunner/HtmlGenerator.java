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
package hu.bme.mit.sette.core.tasks.testsuiterunner;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import hu.bme.mit.sette.core.model.parserxml.FileCoverageElement;
import hu.bme.mit.sette.core.model.parserxml.SnippetCoverageXml;
import hu.bme.mit.sette.core.model.runner.RunnerProjectUtils;
import hu.bme.mit.sette.core.model.snippet.Snippet;
import hu.bme.mit.sette.core.tasks.EvaluationTaskBase;
import hu.bme.mit.sette.core.tool.Tool;
import hu.bme.mit.sette.core.util.io.PathUtils;

public final class HtmlGenerator {
    private final EvaluationTaskBase<Tool> testSuiteRunner;

    /**
     * @param testSuiteRunner
     */
    public HtmlGenerator(EvaluationTaskBase<Tool> testSuiteRunner) {
        this.testSuiteRunner = testSuiteRunner;
    }

    public void generate(Snippet snippet, SnippetCoverageXml coverageXml) {
        Path htmlFile = RunnerProjectUtils.getSnippetHtmlFile(
                this.testSuiteRunner.getRunnerProjectSettings(),
                snippet);

        String htmlTitle = this.testSuiteRunner.getRunnerProjectSettings().getTool().getName()
                + " - " + snippet.getContainer().getJavaClass().getName() + '.'
                + snippet.getMethod().getName() + "()";
        StringBuilder htmlData = new StringBuilder();
        htmlData.append("<!DOCTYPE html>\n");
        htmlData.append("<html lang=\"hu\">\n");
        htmlData.append("<head>\n");
        htmlData.append("       <meta charset=\"utf-8\" />\n");
        htmlData.append("       <title>" + htmlTitle + "</title>\n");
        htmlData.append("       <style type=\"text/css\">\n");
        htmlData.append("               .code { font-family: 'Consolas', monospace; }\n");
        htmlData.append(
                "               .code .line { border-bottom: 1px dotted #aaa; white-space: pre; }\n");
        htmlData.append("               .code .green { background-color: #CCFFCC; }\n");
        htmlData.append("               .code .yellow { background-color: #FFFF99; }\n");
        htmlData.append("               .code .red { background-color: #FFCCCC; }\n");
        htmlData.append("               .code .line .number {\n");
        htmlData.append("                       display: inline-block;\n");
        htmlData.append("                       width:50px;\n");
        htmlData.append("                       text-align:right;\n");
        htmlData.append("                       margin-right:5px;\n");
        htmlData.append("               }\n");
        htmlData.append("       </style>\n");
        htmlData.append("</head>\n");
        htmlData.append("\n");
        htmlData.append("<body>\n");
        htmlData.append("       <h1>" + htmlTitle + "</h1>\n");

        for (FileCoverageElement fce : coverageXml.getCoverage()) {
            htmlData.append("       <h2>" + fce.getName() + "</h2>\n");
            htmlData.append("       \n");

            Path src = testSuiteRunner.getSnippetProject().getSourceDir().resolve(fce.getName());
            List<String> srcLines = PathUtils.readAllLines(src);

            int[] full = TestSuiteRunnerHelper.linesToArray(fce.getFullyCoveredLines());
            int[] partial = TestSuiteRunnerHelper.linesToArray(fce.getPartiallyCoveredLines());
            int[] not = TestSuiteRunnerHelper.linesToArray(fce.getNotCoveredLines());

            htmlData.append("       <div class=\"code\">\n");
            int i = 1;
            for (String srcLine : srcLines) {
                String divClass = getLineDivClass(i, full, partial, not);
                htmlData.append("               <div class=\"" + divClass
                        + "\"><div class=\"number\">" + i + "</div> " + srcLine + "</div>\n");
                i++;
            }
            htmlData.append("       </div>\n\n");
        }

        htmlData.append("</body>\n");
        htmlData.append("</html>\n");

        PathUtils.write(htmlFile, htmlData.toString().getBytes());
    }

    private static String getLineDivClass(int lineNumber, int[] full, int[] partial, int[] not) {
        if (Arrays.binarySearch(full, lineNumber) >= 0) {
            return "line green";
        } else if (Arrays.binarySearch(partial, lineNumber) >= 0) {
            return "line yellow";
        } else if (Arrays.binarySearch(not, lineNumber) >= 0) {
            return "line red";
        } else {
            return "line";
        }
    }
}
