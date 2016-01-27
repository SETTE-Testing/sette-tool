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
import java.nio.file.FileSystemException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PathUtils {
    private static final Logger log = LoggerFactory.getLogger(PathUtils.class);

    private PathUtils() {
        throw new UnsupportedOperationException("Static class");
    }

    public static void createDir(Path dir) throws IOException {
        log.info("Creating directory: {}", dir);
        Files.createDirectories(dir);
        log.debug("Created directory: {}", dir);
    }

    public static void copy(Path source, Path target) throws IOException {
        // This is not good (e.g. target already exists and need to merge):
        // Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

        // FIXME check that source is not part of target, etc.

        if (Files.isDirectory(source)) {
            Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path sDir, BasicFileAttributes attrs)
                        throws IOException {
                    Path tDir = target.resolve(source.relativize(sDir));
                    Files.createDirectories(tDir);

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path sFile, BasicFileAttributes attrs)
                        throws IOException {
                    Path tFile = target.resolve(source.relativize(sFile));
                    Files.copy(sFile, tFile, StandardCopyOption.REPLACE_EXISTING);

                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static void delete(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            log.info("Deleting directory recursively {}: ", path);
            Files.walkFileTree(path, new DeleteFileVisitor());
            log.debug("Deleted directory recursively: {}", path);
        } else {
            log.info("Deleting file {}: ", path);
            Files.delete(path);
            log.debug("Deleted: file {}", path);
        }
    }

    public static void deleteIfExists(Path path) throws IOException {
        if (exists(path)) {
            delete(path);
        }
    }

    public static byte[] readAllBytes(Path path) throws IOException {
        return Files.readAllBytes(path);
    }

    public static List<String> readAllLines(Path path) throws IOException {
        return Files.readAllLines(path);
    }

    public static List<String> readAllLinesOrEmpty(Path path) throws IOException {
        if (exists(path)) {
            return PathUtils.readAllLines(path);
        } else {
            return new ArrayList<>();
        }
    }

    public static void write(Path file, byte[] bytes) throws IOException {
        log.debug("Writing file: {}", file);
        Files.write(file, bytes);
        log.debug("Wrote file: {}", file);
    }

    public static void write(Path file, Iterable<? extends CharSequence> lines) throws IOException {
        log.debug("Writing file: {}", file);
        Files.write(file, lines);
        log.debug("Wrote file: {}", file);
    }

    private static void checkExists(Path path) throws IOException {
        if (!exists(path)) {
            throw new NoSuchFileException(path.toString());
        }
    }

    private static void checkIsDirectory(Path path) throws IOException {
        checkExists(path);
        if (!Files.isDirectory(path)) {
            throw new NotDirectoryException(path.toString());
        }
    }

    private static void checkIsRegularFile(Path path) throws IOException {
        checkExists(path);
        if (!Files.isRegularFile(path)) {
            throw new NotRegularFileException(path.toString());
        }
    }

    public static final class NotRegularFileException extends FileSystemException {
        private static final long serialVersionUID = -4910285516621606130L;

        public NotRegularFileException(String file) {
            super(file);
        }
    }

    public static Stream<Path> walk(Path start) throws IOException {
        return Files.walk(start);
    }

    public static boolean exists(Path dir) {
        return Files.exists(dir);
    }

    public static Stream<String> lines(Path path) throws IOException {
        return Files.lines(path);
    }

    public static void copy(InputStream in, Path target) throws IOException {
        Files.copy(in, target);
    }
}
