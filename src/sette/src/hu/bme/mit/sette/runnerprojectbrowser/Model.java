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

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSortedSet;

import hu.bme.mit.sette.core.configuration.SetteConfiguration;
import hu.bme.mit.sette.core.model.runner.RunnerProject;
import hu.bme.mit.sette.core.model.snippet.SnippetProject;
import hu.bme.mit.sette.core.tool.Tool;
import lombok.Getter;

public final class Model {
    private final Logger LOG = LoggerFactory.getLogger(getClass());

    @Getter
    private final SetteConfiguration configuration;
    @Getter
    private final ImmutableSortedSet<SnippetProject> snippetProjects;
    @Getter
    private final ImmutableSortedSet<Tool> tools;
    @Getter
    private final ImmutableSortedSet<RunnerProject<Tool>> runnerProjects;

    private Model() throws Exception {// TODO be more specific on exception
        LOG.info("Loading Runner Project Browser model");

        // load config, snipept projects, tools
        configuration = SetteConfiguration.parse(Paths.get("sette.config.json"));

        Iterator<SnippetProject> snippetProjsIt = configuration.getSnippetProjectDirs()
                .stream()
                .map(dir -> {
                    try {
                        return SnippetProject.parse(dir);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                })
                .iterator();
        snippetProjects = ImmutableSortedSet.copyOf(snippetProjsIt);

        Iterator<Tool> toolsIt = configuration.getToolConfigurations()
                .stream()
                .map(toolConf -> {
                    try {
                        return Tool.create(toolConf);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }).iterator();
        tools = ImmutableSortedSet.copyOf(toolsIt);

        // find runner projects
        Iterator<RunnerProject<Tool>> runnerProjIt = Files.list(configuration.getOutputDir())
                .filter(Files::isDirectory)
                .filter(dir -> !dir.getFileName().toString().startsWith("."))
                .map(dir -> {
                    try {
                        return RunnerProject.parse(snippetProjects, tools, dir);
                    } catch (Exception ex) {
                        LOG.info(ex.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .iterator();
        runnerProjects = ImmutableSortedSet.copyOf(runnerProjIt);

        LOG.info("Loaded Runner Project Browser model");
    }

    public static Model create() {
        try {
            return new Model();
        } catch (Exception ex) {
            throw new RuntimeException("Cannot create model", ex);
        }
    }
}
