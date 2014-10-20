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
package hu.bme.mit.sette.snippetbrowser;

import hu.bme.mit.sette.common.exceptions.SetteException;
import hu.bme.mit.sette.common.model.snippet.SnippetContainer;
import hu.bme.mit.sette.common.model.snippet.SnippetProject;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

// TODO remove suppresswarnigns and general serial version uid one when it is in
// final package
public final class SnippetBrowser extends JFrame {
    private static final long serialVersionUID = -7336092511991754709L;

    private final SnippetProject snippetProject;

    private JTree treeSnippets;
    private JTextArea txtrInfo;

    /**
     * Create the application.
     *
     * @param snippetProjectSettings
     *
     * @throws SetteException
     */
    public SnippetBrowser(SnippetProject snippetProject)
            throws SetteException {
        Validate.notNull(snippetProject,
                "Snippet project must not be null");
        Validate.isTrue(
                snippetProject.getState().equals(
                        SnippetProject.State.PARSED),
                "Snippet project must not be parsed");

        this.snippetProject = snippetProject;

        initialize();
        initialized();
    }

    /**
     * Initialise the contents of the frame.
     */
    private void initialize() {
        setTitle("Snippet Browser");
        this.setBounds(50, 50, 1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JSplitPane splitPane = new JSplitPane();
        splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.3);
        getContentPane().add(splitPane, BorderLayout.CENTER);

        JScrollPane scrollPaneLeft = new JScrollPane();
        splitPane.setLeftComponent(scrollPaneLeft);

        treeSnippets = new JTree();
        scrollPaneLeft.setViewportView(treeSnippets);

        JScrollPane scrollPaneRight = new JScrollPane();
        splitPane.setRightComponent(scrollPaneRight);

        txtrInfo = new JTextArea();
        txtrInfo.setEditable(false);
        scrollPaneRight.setViewportView(txtrInfo);
    }

    private void initialized() {
        DefaultTreeModel model = new DefaultTreeModel(
                new SnippetProjectTreeNode(snippetProject));
        treeSnippets.setModel(model);
        treeSnippets
        .addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                if (treeSnippets.getSelectionCount() == 1) {
                    TreeNodeBase<?, ?> treeNode = (TreeNodeBase<?, ?>) treeSnippets
                            .getSelectionPath()
                            .getLastPathComponent();
                    txtrInfo.setText(treeNode.getDescription());
                } else if (treeSnippets.getSelectionCount() > 1) {
                    int projectContainerCnt = 0;
                    int projectSnippetCnt = 0;

                    projectContainerCnt = snippetProject
                            .getModel().getContainers().size();

                    for (SnippetContainer container : snippetProject
                            .getModel().getContainers()) {
                        projectSnippetCnt += container
                                .getSnippets().size();
                    }

                    int containerCnt = 0;
                    int containerSnippetCnt = 0;
                    int snippetCnt = 0;

                    for (TreePath path : treeSnippets
                            .getSelectionPaths()) {
                        Object node = path
                                .getLastPathComponent();

                        if (node instanceof SnippetContainerTreeNode) {
                            containerCnt++;

                            SnippetContainerTreeNode obj = (SnippetContainerTreeNode) node;
                            containerSnippetCnt += obj
                                    .getContainer()
                                    .getSnippets().size();
                        } else if (node instanceof SnippetTreeNode) {
                            snippetCnt++;
                        }
                    }

                    String[] lines = new String[3];

                    lines[0] = String
                            .format("Project contains %d container(s) with %d snippet(s)",
                                    projectContainerCnt,
                                    projectSnippetCnt);
                    lines[1] = String
                            .format("Selected %d container(s) (%d snippet(s))",
                                    containerCnt,
                                    containerSnippetCnt);
                    lines[2] = String.format(
                            "Selected %d snippet(s)",
                            snippetCnt);

                    txtrInfo.setText(StringUtils.join(lines,
                            '\n'));
                } else {
                    txtrInfo.setText("[No selection]");
                }
            }
        });
    }
}
