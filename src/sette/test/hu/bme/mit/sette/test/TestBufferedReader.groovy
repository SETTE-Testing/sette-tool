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
package hu.bme.mit.sette.test

import groovy.transform.CompileStatic

import java.util.stream.Stream

/**
 * A {@link BufferedReader} implementation for testing which stores severals lines to be returned 
 * later by {@link #readLine()}.
 */
@CompileStatic
public class TestBufferedReader extends BufferedReader {
    final LinkedList<String> data = new LinkedList<>()

    public TestBufferedReader() {
        // superclass is not used but a valid parameter is needed for its ctor
        super(new InputStreamReader(new ByteArrayInputStream(''.bytes)))
    }

    public void addLines(String... lines) {
        data.addAll(lines)
    }

    @Override
    public int read() throws IOException {
        throw new UnsupportedOperationException()
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        throw new UnsupportedOperationException()
    }

    @Override
    public String readLine() throws IOException {
        if (data) {
            throw new IllegalStateException('No data is available')
        } else {
            return data.removeFirst()
        }
    }
    @Override
    public long skip(long n) throws IOException {
        throw new UnsupportedOperationException()
    }

    @Override
    public boolean ready() throws IOException {
        throw new UnsupportedOperationException()
    }

    @Override
    public boolean markSupported() {
        throw new UnsupportedOperationException()
    }

    @Override
    public void mark(int readAheadLimit) throws IOException {
        throw new UnsupportedOperationException()
    }

    @Override
    public void reset() throws IOException {
        throw new UnsupportedOperationException()
    }

    @Override
    public void close() throws IOException {
        throw new UnsupportedOperationException()
    }

    @Override
    public Stream<String> lines() {
        throw new UnsupportedOperationException()
    }
}
