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
package hu.bme.mit.sette.snippetbrowser;

import java.util.ArrayList;
import java.util.List;

import javax.swing.tree.TreeNode;

import hu.bme.mit.sette.core.model.snippet.SnippetContainer;
import hu.bme.mit.sette.core.model.snippet.SnippetDependency;
import hu.bme.mit.sette.core.model.snippet.SnippetProject;

public final class SnippetProjectTreeNode extends TreeNodeBase<TreeNode, SnippetContainerTreeNode> {
    private final SnippetProject project;

    public SnippetProjectTreeNode(SnippetProject project) {
        super(null);
        this.project = project;

        List<SnippetContainerTreeNode> ret = new ArrayList<>();

        for (SnippetContainer container : project.getSnippetContainers()) {
            ret.add(new SnippetContainerTreeNode(this, container));
        }

        setChildrenList(ret);
    }

    public SnippetProject getProject() {
        return project;
    }

    @Override
    public String getTitle() {
        return project.getBaseDir().getFileName().toString();
    }

    @Override
    public String getDescription() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("Base dir: ")
                    .append(project.getBaseDir())
                    .append('\n').append('\n');

            sb.append("Dependency count: " + project.getSnippetDependencies().size())
                    .append('\n');

            for (SnippetDependency dep : project.getSnippetDependencies()) {
                sb.append("  " + dep.getJavaClass().getName()).append('\n');
            }

            return sb.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
            return ex.getMessage();
        }
    }
}
