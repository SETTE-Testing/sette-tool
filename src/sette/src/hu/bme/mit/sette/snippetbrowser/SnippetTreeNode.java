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
// TODO z revise this file
package hu.bme.mit.sette.snippetbrowser;

import hu.bme.mit.sette.common.model.snippet.Snippet;

import java.lang.reflect.Method;

import javax.swing.tree.TreeNode;

public final class SnippetTreeNode extends TreeNodeBase<SnippetContainerTreeNode, TreeNode> {
    private final Snippet snippet;

    public SnippetTreeNode(SnippetContainerTreeNode tnContainer, Snippet snippet) {
        super(tnContainer);
        this.snippet = snippet;
    }

    public Snippet getSnippet() {
        return snippet;
    }

    @Override
    public String getTitle() {
        return snippet.getMethod().getName();
    }

    @Override
    public String getDescription() {
        try {
            StringBuilder sb = new StringBuilder();

            sb.append(getParent().getDescription()).append('\n');

            sb.append(snippet.getMethod().toGenericString()).append('\n').append('\n');
            sb.append("Required statement coverage: " + snippet.getRequiredStatementCoverage()
                    + "%\n");
            sb.append('\n');

            if (!snippet.getIncludedMethods().isEmpty()) {
                sb.append("Included coverage methods:\n");

                for (Method method : snippet.getIncludedMethods()) {
                    sb.append("  ").append(method.getDeclaringClass().getName()).append('\n');
                    sb.append("    ").append(method.toGenericString()).append('\n');
                }

                sb.append('\n');
            }

            if (snippet.getInputFactory() == null) {
                sb.append("No sample inputs");
            } else {
                sb.append("Sample input count: " + snippet.getInputFactory().getInputs().size());
            }

            sb.append('\n');

            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }
}
