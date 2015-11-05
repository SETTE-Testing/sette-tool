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
 * Copyright 2014-2015
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
package hu.bme.mit.sette.core.random;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.lang3.Validate;

import lombok.NonNull;

/**
 * Splitter stream to write simultaneously to several {@link OutputStream}s. Please note that this
 * method does not check whether a stream is passed twice, so in this case the data will be written
 * twice and even the {@link #flush()} and {@link #close()} methods will be called twice.
 */
public final class SplitterOutputStream extends OutputStream {
    private OutputStream[] branches;

    /**
     * Creates a new {@link SplitterOutputStream} instance.
     * 
     * @param branches
     *            The target {@link OutputStream}s to write to. Must not be <code>null</code> nor
     *            empty and must not contain <code>null</code> elements.
     */
    public SplitterOutputStream(@NonNull OutputStream... branches) {
        super();
        Validate.notEmpty(branches);
        Validate.noNullElements(branches);
        this.branches = branches.clone();
    }

    @Override
    public void write(int b) throws IOException {
        for (OutputStream branch : branches) {
            branch.write(b);
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        for (OutputStream branch : branches) {
            branch.write(b);
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        for (OutputStream branch : branches) {
            branch.write(b, off, len);
        }
    }

    @Override
    public void flush() throws IOException {
        for (OutputStream branch : branches) {
            branch.flush();
        }
    }

    @Override
    public void close() throws IOException {
        // throw the first exception if any
        IOException exception = null;

        for (OutputStream branch : branches) {
            try {
                branch.close();
            } catch (IOException ex) {
                if (exception == null) {
                    exception = ex;
                }
            }
        }

        if (exception != null) {
            throw exception;
        }
    }
}
