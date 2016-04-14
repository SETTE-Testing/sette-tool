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
package hu.bme.mit.sette.runnerprojectbrowser;

import static java.util.stream.Collectors.toList;

import java.awt.Desktop;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import org.apache.commons.lang3.SystemUtils;

import hu.bme.mit.sette.core.model.runner.RunnerProject;
import hu.bme.mit.sette.core.model.snippet.Snippet;
import hu.bme.mit.sette.core.model.snippet.SnippetContainer;
import hu.bme.mit.sette.core.model.snippet.SnippetProject;
import hu.bme.mit.sette.core.tool.Tool;
import hu.bme.mit.sette.core.util.io.PathUtils;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

public final class Controller implements Initializable {
    private Model model;

    @FXML
    private ListView<SnippetProject> snippetProjectFilter;
    @FXML
    private ListView<Tool> toolFilter;
    @FXML
    private TextField runnerProjectTagFilter;
    @FXML
    private ListView<RunnerProject<Tool>> runnerProjectList;
    @FXML
    private TextField snippetFilter;
    @FXML
    private ListView<Snippet> snippetList;
    @FXML
    private GridPane infoPane;
    @FXML
    private TextArea snippetInfo;
    @FXML
    private Button openSnippetCode;
    @FXML
    private Button openSnippetInputCode;
    @FXML
    private Button openInfoFile;
    @FXML
    private Button openOutFile;
    @FXML
    private Button openErrFile;
    @FXML
    private Button openTestCode;
    @FXML
    private Button openTestCodeEvosuiteScaffolding;
    @FXML
    private Button openInputsXml;
    @FXML
    private Button openResultXml;
    @FXML
    private Button openCoverageXml;
    @FXML
    private Button openHtml;

    private ObservableList<RunnerProject<Tool>> availableRunnerProjects;
    private final ObjectProperty<RunnerProject<Tool>> selectedRunnerProject = new SimpleObjectProperty<>();

    private ObservableList<Snippet> availableSnippets;
    private final ObjectProperty<Snippet> selectedSnippet = new SimpleObjectProperty<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // check FXML loading
        try {
            for (Field field : getClass().getDeclaredFields()) {
                if (field.getAnnotation(FXML.class) != null && field.get(this) == null) {
                    throw new RuntimeException("FXML field is null: " + field.getName());
                }
            }
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }

        // initialize
        model = Model.create();
        availableRunnerProjects = FXCollections.observableArrayList(model.getRunnerProjects());
        availableSnippets = FXCollections.emptyObservableList();

        //////////////////////////////////////////////////
        // Runner project selector
        //////////////////////////////////////////////////
        // snippet project filter
        snippetProjectFilter.setCellFactory(lv -> new SnippetProjectCell());
        snippetProjectFilter.getItems().addAll(model.getSnippetProjects());
        snippetProjectFilter.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        snippetProjectFilter.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> updateRunnerProjectList());
        snippetProjectFilter.getSelectionModel().selectAll();
        resizeHeightForItemCount(snippetProjectFilter);

        // tool filter
        toolFilter.setCellFactory(lv -> new ToolCell());
        toolFilter.getItems().addAll(model.getTools());
        toolFilter.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        toolFilter.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> updateRunnerProjectList());
        toolFilter.getSelectionModel().selectAll();
        resizeHeightForItemCount(toolFilter);

        // tag filter
        runnerProjectTagFilter.textProperty()
                .addListener((observable, oldValue, newValue) -> updateRunnerProjectList());

        // runner project list
        runnerProjectList.setCellFactory(lv -> new RunnerProjectCell());
        selectedRunnerProject.bind(runnerProjectList.getSelectionModel().selectedItemProperty());
        selectedRunnerProject.addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                availableSnippets = FXCollections.emptyObservableList();
            } else {
                List<Snippet> snippetIds = newValue.getSnippetProject().getSnippetContainers()
                        .stream()
                        .map(sc -> sc.getSnippets())
                        .flatMap(s -> s.values().stream())
                        .sorted()
                        .collect(toList());

                availableSnippets = FXCollections.observableList(snippetIds);
            }
            updateSnippetList();
        });

        //////////////////////////////////////////////////
        // Snippet project selector
        //////////////////////////////////////////////////
        // snippet filter
        snippetFilter.textProperty()
                .addListener((observable, oldValue, newValue) -> updateSnippetList());

        // snippet list
        snippetList.setCellFactory(lv -> new SnippetCell());
        selectedSnippet.bind(snippetList.getSelectionModel().selectedItemProperty());
        selectedSnippet.addListener((observable, oldValue, newValue) -> updateInfoPane());

        //////////////////////////////////////////////////
        // Info for runner project - snippet
        //////////////////////////////////////////////////
    }

    private void updateRunnerProjectList() {
        FilteredList<RunnerProject<Tool>> filteredList = availableRunnerProjects.filtered(rp -> {
            return snippetProjectFilter.getSelectionModel().getSelectedItems()
                    .contains(rp.getSnippetProject())
                    && toolFilter.getSelectionModel().getSelectedItems().contains(rp.getTool())
                    && rp.getTag().toLowerCase()
                            .contains(runnerProjectTagFilter.getText().toLowerCase());
        });
        runnerProjectList.setItems(new SortedList<>(filteredList));

        updateSnippetList();
    }

    private void updateSnippetList() {
        if (availableSnippets.isEmpty()) {
            snippetList.setItems(FXCollections.emptyObservableList());
        } else {
            FilteredList<Snippet> filteredList = availableSnippets.filtered(
                    s -> s.getId().toLowerCase().contains(snippetFilter.getText().toLowerCase()));
            snippetList.setItems(new SortedList<>(filteredList));
        }

        updateInfoPane();
    }

    private void updateInfoPane() {
        if (selectedSnippet.get() == null) {
            infoPane.setVisible(false);
        } else {
            infoPane.setVisible(true);

            RunnerProject<Tool> runnerProject = selectedRunnerProject.get();
            Snippet snippet = selectedSnippet.get();
            SnippetContainer snippetContainer = snippet.getContainer();
            SnippetProject snippetProject = snippetContainer.getSnippetProject();

            List<String> infoLines = new ArrayList<>();
            infoLines.add("Runner project:    " + runnerProject.getProjectName());
            infoLines.add("Snippet container: " + snippet.getContainer().getName());
            infoLines.add("Snippet id:        " + snippet.getId());
            infoLines.add("Required coverage: " + snippet.getRequiredStatementCoverage());

            if (!snippet.getIncludedConstructors().isEmpty()) {
                infoLines.add("Incl. ctors:");
                for (Constructor<?> ctor : snippet.getIncludedConstructors()) {
                    infoLines.add("    " + ctor);
                }
            }

            if (!snippet.getIncludedMethods().isEmpty()) {
                infoLines.add("Incl. methods:");
                for (Method method : snippet.getIncludedMethods()) {
                    infoLines.add("    " + method);
                }
            }

            infoLines.add("");

            Path infoFile = runnerProject.getInfoFile(snippet);
            if (PathUtils.exists(infoFile)) {
                try {
                    infoLines.addAll(PathUtils.readAllLines(infoFile));
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            } else {
                infoLines.add("No .info file");
            }

            snippetInfo.setText(String.join("\n", infoLines));

            Path snippetSourceFile = snippetProject.getSourceDir().resolve(
                    snippetContainer.getJavaClass().getName().replace('.', '/') + ".java");

            Path snippetSourceInputFile;
            if (snippetContainer.getInputFactoryContainer() != null) {
                snippetSourceInputFile = snippetProject.getInputSourceDir().resolve(
                        snippetContainer.getInputFactoryContainer().getJavaClass().getName()
                                .replace('.', '/') + ".java");
            } else {
                snippetSourceInputFile = null;
            }

            // either file or a directory
            Path testCodePath = runnerProject.getTestDirectory().resolve(
                    snippetContainer.getJavaClass().getName().replace('.', '/') + '_'
                            + snippet.getName() + "_Test");
            Path testCodeEvosuiteScaffoldingPath = null;
            if (!Files.exists(testCodePath)) {
                String base = testCodePath.getFileName().toString();
                testCodePath = testCodePath.resolveSibling(base + ".java");

                if (!Files.exists(testCodePath)) {
                    base = base.replaceAll("_Test$", "") + '_' + snippet.getName() + "_Test";
                    testCodePath = testCodePath.resolveSibling(base + ".java");
                    testCodeEvosuiteScaffoldingPath = testCodePath
                            .resolveSibling(base + "_scaffolding.java");
                }
            }

            updateButton(openSnippetCode, snippetSourceFile);
            updateButton(openSnippetInputCode, snippetSourceInputFile);
            updateButton(openInfoFile, infoFile);
            updateButton(openOutFile, runnerProject.getOutputFile(snippet));
            updateButton(openErrFile, runnerProject.getErrorOutputFile(snippet));
            updateButton(openTestCode, testCodePath);
            updateButton(openTestCodeEvosuiteScaffolding, testCodeEvosuiteScaffoldingPath);
            updateButton(openInputsXml, runnerProject.getInputsXmlFile(snippet));
            updateButton(openResultXml, runnerProject.getResultXmlFile(snippet));
            updateButton(openCoverageXml, runnerProject.getCoverageXmlFile(snippet));
            updateButton(openHtml, runnerProject.getCoverageHtmlFile(snippet));
        }

    }

    private static void updateButton(Button button, Path path) {
        try {
            if (path != null && PathUtils.exists(path)) {
                if (Files.isDirectory(path)) {
                    // TODO
                    button.setText("Open directory");
                    button.setDisable(false);
                    button.setOnAction(event -> updateButtonAction(path));
                } else {
                    long size = Files.size(path);
                    if (size == 0) {
                        button.setText("Empty file");
                        button.setDisable(true);
                        button.setOnAction(null);
                    } else {
                        button.setText(String.format("Open file (%.2f kiB)", (double) size / 1024));
                        button.setDisable(false);
                        button.setOnAction(event -> updateButtonAction(path));
                    }
                }
            } else {
                button.setText("Does not exist");
                button.setDisable(true);
                button.setOnAction(null);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void updateButtonAction(Path path) {
        try {
            if (Files.isDirectory(path)) {
                Desktop.getDesktop().open(path.toFile());
            } else if (path.getFileName().toString().endsWith("html")) {
                Desktop.getDesktop().open(path.toFile());
            } else if (SystemUtils.IS_OS_WINDOWS) {
                // try to open with Notepad++
                String[] cmd = {
                        "cmd", "/c", "REG", "QUERY",
                        "HKEY_CLASSES_ROOT\\Applications\\notepad++.exe"
                };
                int exitCode = Runtime.getRuntime().exec(cmd).waitFor();
                if (exitCode == 0) {
                    cmd = new String[] {
                            "cmd", "/c", "start",
                            "notepad++", path.toAbsolutePath().toString()
                    };
                    Runtime.getRuntime().exec(cmd).waitFor();
                } else {
                    Desktop.getDesktop().edit(path.toFile());
                }
            } else {
                Desktop.getDesktop().edit(path.toFile());
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static final class SnippetProjectCell extends ListCell<SnippetProject> {
        @Override
        protected void updateItem(SnippetProject snippetProject, boolean empty) {
            super.updateItem(snippetProject, empty);

            if (empty || snippetProject == null) {
                setText(null);
                setGraphic(null);
            } else {
                setText(snippetProject.getName());
            }
        }

    }

    private static void resizeHeightForItemCount(ListView<?> listView) {
        listView.setPrefHeight(24 * listView.getItems().size());
    }

    private static final class ToolCell extends ListCell<Tool> {
        @Override
        protected void updateItem(Tool tool, boolean empty) {
            super.updateItem(tool, empty);

            if (empty || tool == null) {
                setText(null);
                setGraphic(null);
            } else {
                setText(tool.getName());
            }
        }
    }

    private static final class RunnerProjectCell extends ListCell<RunnerProject<Tool>> {
        @Override
        protected void updateItem(RunnerProject<Tool> runnerProject, boolean empty) {
            super.updateItem(runnerProject, empty);

            if (empty || runnerProject == null) {
                setText(null);
                setGraphic(null);
            } else {
                setText(runnerProject.getProjectName());
            }
        }
    }

    private static final class SnippetCell extends ListCell<Snippet> {
        @Override
        protected void updateItem(Snippet snippet, boolean empty) {
            super.updateItem(snippet, empty);

            if (empty || snippet == null) {
                setText(null);
                setGraphic(null);
            } else {
                setText(snippet.getId());
            }
        }
    }
}
