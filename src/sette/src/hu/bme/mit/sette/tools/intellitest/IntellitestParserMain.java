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
package hu.bme.mit.sette.tools.intellitest;

import static java.util.stream.Collectors.toList;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

import hu.bme.mit.sette.core.configuration.SetteConfiguration;
import hu.bme.mit.sette.core.model.snippet.SnippetProject;
import hu.bme.mit.sette.core.validator.PathValidator;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IntellitestParserMain {
    public static void main(String[] args) throws Exception {
        log.info("== Intellitest Parser Main ==");

        try {
            Locale.setDefault(new Locale("en", "GB"));
        } catch (Exception ex) {
            Locale.setDefault(Locale.ENGLISH);
        }

        Path configFile = Paths.get("sette.config.json");
        SetteConfiguration setteConfig = SetteConfiguration.parse(configFile);

        Path intellitestDir = setteConfig.getBaseDir().resolve("sette-mutation-intellitest");
        PathValidator.forDirectory(intellitestDir, true, true, true).validate();

        SnippetProject snippetProject = SnippetProject.parse(
                setteConfig.getBaseDir().resolve("sette-snippets/java/sette-snippets"));

        List<Path> sourceDirs = Files.list(intellitestDir)
                .filter(dir -> dir.getFileName().toString().startsWith("sette-snippets.Tests"))
                .collect(toList());

        for (Path sourceDir : sourceDirs) {
            Path targetDir = sourceDir
                    .resolveSibling("java-" + sourceDir.getFileName().toString());
            new IntellitestParser(snippetProject, sourceDir, targetDir).parse();
        }

        log.info("== Done");
    }
}
