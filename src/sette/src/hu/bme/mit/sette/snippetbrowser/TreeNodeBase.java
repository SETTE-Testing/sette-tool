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

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.swing.tree.TreeNode;

public abstract class TreeNodeBase<P extends TreeNode, C extends TreeNode> implements TreeNode {
    private final P parent;
    private List<C> childrenList = null;

    public TreeNodeBase(P parent) {
        this.parent = parent;
    }

    protected final void setChildrenList(List<C> childrenList) {
        this.childrenList = childrenList;
    }

    @Override
    public final TreeNode getChildAt(int childIndex) {
        return this.childrenList.get(childIndex);
    }

    @Override
    public final int getChildCount() {
        return this.childrenList.size();
    }

    @Override
    public final P getParent() {
        return this.parent;
    }

    @Override
    public final int getIndex(TreeNode node) {
        return this.childrenList.indexOf(node);
    }

    @Override
    public final boolean getAllowsChildren() {
        return this.childrenList != null;
    }

    @Override
    public final boolean isLeaf() {
        return this.childrenList == null;
    }

    @Override
    public final Enumeration<C> children() {
        return Collections.enumeration(this.childrenList);
    }

    @Override
    public final String toString() {
        return this.getTitle();
    }

    public abstract String getTitle();

    public abstract String getDescription();
}
