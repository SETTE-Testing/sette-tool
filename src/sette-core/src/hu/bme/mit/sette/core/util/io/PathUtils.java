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
package hu.bme.mit.sette.core.util.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class for {@link Path}s and IO wrapping JDK NIO {@link Files} by adding logging and
 * wrapping {@link IOException}s into {@link UncheckedIOException}s. (SETTE should always fail if an
 * IO operation fails).
 */
public final class PathUtils {
    private static final Logger log = LoggerFactory.getLogger(PathUtils.class);

    private PathUtils() {
        throw new UnsupportedOperationException("Static class");
    }

    public static void createDir(Path dir) {
        if (Files.exists(dir)) {
            return;
        }

        try {
            log.info("Creating directory: {}", dir);
            Files.createDirectories(dir);
            log.debug("Created directory: {}", dir);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public static void copy(Path source, Path target) {
        // This is not good (e.g. target already exists and need to merge):
        // Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

        // FIXME check that source is not part of target, etc.

        try {
            if (Files.isDirectory(source)) {
                log.info("Copying directory: {} -> {}", source, target);
                // FIXME extract to copy file visitor
                Files.walkFileTree(source, new CopyFileVisitor(source, target));
                log.debug("Copied directory: {} -> {}", source, target);
            } else {
                log.debug("Copying file: {} -> {}", source, target);
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                log.debug("Copied file: {} -> {}", source, target);
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public static void move(Path source, Path target) {
        try {
            // FIXME check that source is not part of target, etc.
            log.info("Moving path: {} -> {}", source, target);
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public static void delete(Path path) {
        try {
            if (Files.isDirectory(path)) {
                log.info("Deleting directory recursively {}: ", path);
                Files.walkFileTree(path, new DeleteFileVisitor());
                log.debug("Deleted directory recursively: {}", path);
            } else {
                log.info("Deleting file {}: ", path);
                Files.delete(path);
                log.debug("Deleted: file {}", path);
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public static void deleteIfExists(Path path) {
        if (exists(path)) {
            delete(path);
        }
    }

    public static byte[] readAllBytes(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public static List<String> readAllLines(Path path) {
        return readAllLines(path, StandardCharsets.UTF_8);
    }

    public static List<String> readAllLines(Path path, Charset charset) {
        try {
            return Files.readAllLines(path, charset);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public static List<String> readAllLinesOrEmpty(Path path) {
        return readAllLinesOrEmpty(path, StandardCharsets.UTF_8);
    }

    public static List<String> readAllLinesOrEmpty(Path path, Charset charset) {
        if (exists(path)) {
            return readAllLines(path, charset);
        } else {
            return new ArrayList<>();
        }
    }

    public static void write(Path file, byte[] bytes) {
        try {
            log.debug("Writing file: {}", file);
            Files.createDirectories(file.getParent());
            Files.write(file, bytes);
            log.debug("Wrote file: {}", file);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public static void write(Path file, Iterable<? extends CharSequence> lines) {
        try {
            log.debug("Writing file: {}", file);
            Files.createDirectories(file.getParent());
            Files.write(file, lines);
            log.debug("Wrote file: {}", file);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public static Stream<Path> walk(Path start) {
        try {
            return Files.walk(start);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public static boolean exists(Path path) {
        return Files.exists(path);
    }

    public static Stream<String> lines(Path path) {
        try {
            return Files.lines(path);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public static void copy(InputStream in, Path target) {
        try {
            Files.copy(in, target);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public static void copy(URL url, Path target) {
        try {
            copy(url.openStream(), target);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public static Path toRealPath(Path baseDir) {
        try {
            return baseDir.toRealPath();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public static long size(Path path) {
        try {
            return Files.size(path);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
