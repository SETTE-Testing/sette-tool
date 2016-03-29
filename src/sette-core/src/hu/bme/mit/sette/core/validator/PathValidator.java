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
package hu.bme.mit.sette.core.validator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.google.common.base.Splitter;

import lombok.NonNull;

/**
 * Validator for paths.
 */
public final class PathValidator extends Validator<Path> {
    /**
     * Instantiates a new path validator.
     *
     * @param path
     *            the path
     */
    public PathValidator(Path path) {
        super(path);
    }

    /**
     * Specifies the required type of the path.
     *
     * @param type
     *            the required type of the path
     * @return this object
     */
    public PathValidator type(@NonNull PathType type) {
        addErrorIfNotEquals("type", type, PathType.forPath(getSubject()));
        return this;
    }

    /**
     * Specifies whether the path should be readable or not.
     *
     * @param isReadable
     *            <code>true</code> if the path should be readable, <code>false</code> if it should
     *            not be
     * @return this object
     */
    public PathValidator readable(boolean isReadable) {
        addErrorIfNotEquals("readable", isReadable, Files.isReadable(getSubject()));
        return this;
    }

    /**
     * Specifies whether the path should be writable or not.
     *
     * @param isWritable
     *            <code>true</code> if the path should be writable, <code>false</code> if it should
     *            not be
     * @return this object
     */
    public PathValidator writable(boolean isWritable) {
        addErrorIfNotEquals("writable", isWritable, Files.isWritable(getSubject()));
        return this;
    }

    /**
     * Specifies whether the path should be executable or not.
     *
     * @param isExecutable
     *            <code>true</code> if the path should be executable, <code>false</code> if it
     *            should not be
     * @return this object
     */
    public PathValidator executable(boolean isExecutable) {
        addErrorIfNotEquals("executable", isExecutable, Files.isExecutable(getSubject()));
        return this;
    }

    /**
     * Specifies the required extension of the filename.
     *
     * @param extension
     *            The required file extension (<code>null</code> means that the filename must not
     *            have any extension, i.e. file name must not contain '.'). Example: "jar",
     *            "tar.gz", "txt|log" (latter will only accept both txt and log)
     * @return this object
     */
    public PathValidator extension(String extension) {
        String filename = getSubject().getFileName().toString();

        if (extension == null) {
            addErrorIfFalse("extension: the filename must not have an extension",
                    filename.indexOf('.') < 0);
        } else {
            // e.g.: "tar.gz|java" => accept *.tar.gz, *.java
            List<String> exts = Splitter.on('|').splitToList(extension);

            if (!exts.stream().anyMatch(ext -> filename.endsWith('.' + ext))) {
                addError("extension: the extension must be: " + extension);
            }

        }

        return this;
    }

    /**
     * Creates a {@link PathValidator} for a nonexistent file.
     * 
     * @param nonexistentFile
     *            the file to validate
     * @return a new {@link PathValidator} instance
     */
    public static PathValidator forNonexistent(Path nonexistentFile) {
        return new PathValidator(nonexistentFile).type(PathType.NONEXISTENT);
    }

    /**
     * Creates a {@link PathValidator} for a directory with permission validation.
     * 
     * @param dir
     *            the directory to validate
     * @param isReadable
     *            see {@link #readable(boolean)} (<code>null</code> means no requirement)
     * @param isWritable
     *            see {@link #writable(boolean)} (<code>null</code> means no requirement)
     * @param executable
     *            see {@link #executable(boolean)} (<code>null</code> means no requirement)
     * @return a new {@link PathValidator} instance
     */
    public static PathValidator forDirectory(Path dir, Boolean isReadable, Boolean isWritable,
            Boolean executable) {
        PathValidator v = new PathValidator(dir).type(PathType.DIRECTORY);
        if (isReadable != null) {
            v.readable(isReadable);
        }
        if (isWritable != null) {
            v.readable(isWritable);
        }
        if (executable != null) {
            v.readable(executable);
        }

        return v;
    }

    /**
     * Creates a {@link PathValidator} for a regular with permission and extension validation.
     * 
     * @param regularFile
     *            the file to validate
     * @param isReadable
     *            see {@link #readable(boolean)} (<code>null</code> means no requirement)
     * @param isWritable
     *            see {@link #writable(boolean)} (<code>null</code> means no requirement)
     * @param executable
     *            see {@link #executable(boolean)} (<code>null</code> means no requirement)
     * @param extension
     *            see {@link #extension(String)} (<code>null</code> means no extension is required)
     * @return a new {@link PathValidator} instance
     */
    public static PathValidator forRegularFile(Path regularFile, Boolean isReadable,
            Boolean isWritable, Boolean isExecutable, String extension) {
        PathValidator v = new PathValidator(regularFile).type(PathType.REGULAR_FILE);
        if (isReadable != null) {
            v.readable(isReadable);
        }
        if (isWritable != null) {
            v.readable(isWritable);
        }
        if (isExecutable != null) {
            v.readable(isExecutable);
        }
        v.extension(extension);
        return v;
    }
}
