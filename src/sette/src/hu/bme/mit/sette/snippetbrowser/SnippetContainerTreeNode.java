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
import hu.bme.mit.sette.common.model.snippet.SnippetContainer;

import java.util.ArrayList;
import java.util.List;

public final class SnippetContainerTreeNode
        extends TreeNodeBase<SnippetProjectTreeNode, SnippetTreeNode> {
    private final SnippetContainer container;

    public SnippetContainerTreeNode(SnippetProjectTreeNode tnProject, SnippetContainer container) {
        super(tnProject);
        this.container = container;

        List<SnippetTreeNode> ret = new ArrayList<>();

        for (Snippet snippet : container.getSnippets().values()) {
            ret.add(new SnippetTreeNode(this, snippet));
        }

        setChildrenList(ret);
    }

    public SnippetContainer getContainer() {
        return container;
    }

    @Override
    public String getTitle() {
        return container.getJavaClass().getName();
    }

    @Override
    public String getDescription() {
        return "Category: " + container.getCategory() + "\nGoal: " + container.getGoal()
                + "\nSnippet count: " + container.getSnippets().size() + "\n";
    }
}
