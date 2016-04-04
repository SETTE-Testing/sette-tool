
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
