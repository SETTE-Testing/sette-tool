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
package hu.bme.mit.sette.common.validator;

import hu.bme.mit.sette.common.validator.exceptions.FileValidationException;

import java.io.File;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;

/**
 * Validator for files.
 */
public final class FileValidator extends AbstractValidator<File> {
    /**
     * Instantiates a new file validator.
     *
     * @param file
     *            the file
     */
    public FileValidator(final File file) {
        super(file);

        if (file == null) {
            this.addException("The file class must not be null");
        }
    }

    @Override
    public void addException(final String message, final Throwable cause) {
        this.addException(new FileValidationException(message,
                getSubject(), cause));
    }

    /**
     * Sets the required type of the file.
     *
     * @param type
     *            the required type of the file.
     * @return this object
     */
    public FileValidator type(final FileType type) {
        Validate.notNull(type, "The type must not be null");

        if (getSubject() != null) {
            File file = getSubject();
            boolean isTypeValid = false;

            switch (type) {
            case NONEXISTENT:
                isTypeValid = !file.exists();
                break;

            case EXISTENT:
                isTypeValid = file.exists();
                break;

            case DIRECTORY:
                isTypeValid = file.isDirectory();
                break;

            case REGULAR_FILE:
                isTypeValid = file.isFile();
                break;

            default:
                throw new UnsupportedOperationException(
                        "Unknown file type: " + type);
            }

            if (!isTypeValid) {
                this.addException(String.format(
                        "The file must have the specified type\n"
                                + "(type: [%s])", type));
            }
        }

        return this;
    }

    /**
     * Sets whether the file should be hidden or not.
     *
     * @param isHidden
     *            true if the file should be hidden, false if it should not be
     * @return this object
     */
    public FileValidator hidden(final boolean isHidden) {
        if (getSubject() != null) {
            File file = getSubject();

            if (isHidden ^ file.isHidden()) {
                String must;

                if (isHidden) {
                    must = "must";
                } else {
                    must = "must not";
                }

                this.addException(String.format(
                        "The file %s be hidden", must));
            }
        }

        return this;
    }

    /**
     * Sets whether the file should be readable or not.
     *
     * @param isReadable
     *            true if the file should be readable, false if it should not be
     * @return this object
     */
    public FileValidator readable(final boolean isReadable) {
        if (getSubject() != null) {
            File file = getSubject();

            if (isReadable ^ file.canRead()) {
                String must;

                if (isReadable) {
                    must = "must";
                } else {
                    must = "must not";
                }

                this.addException(String.format(
                        "The file %s be readable", must));
            }
        }

        return this;
    }

    /**
     * Sets whether the file should be writable or not.
     *
     * @param isWritable
     *            true if the file should be writable, false if it should not be
     * @return this object
     */
    public FileValidator writable(final boolean isWritable) {
        if (getSubject() != null) {
            File file = getSubject();

            if (isWritable ^ file.canWrite()) {
                String must;

                if (isWritable) {
                    must = "must";
                } else {
                    must = "must not";
                }

                this.addException(String.format(
                        "The file %s be writable", must));
            }
        }

        return this;
    }

    /**
     * Sets whether the file should be executable or not.
     *
     * @param isExecutable
     *            true if the file should be executable, false if it should not
     *            be
     * @return this object
     */
    public FileValidator executable(final boolean isExecutable) {
        if (getSubject() != null) {
            File file = getSubject();

            if (isExecutable ^ file.canRead()) {
                String must;

                if (isExecutable) {
                    must = "must";
                } else {
                    must = "must not";
                }

                this.addException(String.format(
                        "The file %s be executable", must));
            }
        }

        return this;
    }

    /**
     * Sets the allowed extensions for the file.
     *
     * @param allowedExtensions
     *            the allowed extensions
     * @return this object
     */
    public FileValidator extension(final String... allowedExtensions) {
        if (getSubject() != null) {
            File file = getSubject();

            if (!FilenameUtils.isExtension(file.getName(),
                    allowedExtensions)) {
                this.addException(String.format(
                        "The file has an inappropriate extension\n"
                                + "(allowedExtensions: [%s])",
                                ArrayUtils.toString(allowedExtensions)));
            }
        }
        return this;
    }
}
