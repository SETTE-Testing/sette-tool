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
package hu.bme.mit.sette.common.util.process;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;

/**
 * This class can read all the data from the given {@link Reader} to the given
 * {@link StringBuffer} and also implements {@link Runnable}. It can also notify
 * listeners of the {@link RunnableReaderListener} interface.
 */
public final class RunnableReader extends Reader implements Runnable {
    /**
     * The default buffer size for {@link RunnableReader extends Reader}
     * objects.
     */
    public static final int DEFAULT_BUFFER_SIZE = 1024;

    /**
     * Represents the state of a {@link RunnableReader extends Reader} object.
     */
    public static enum State {
        /** The object has created. */
        CREATED,
        /** The execution has started. */
        STARTED,
        /** The execution has finished. */
        FINISHED,
        /** The execution was stopped. */
        STOPPED,
        /** An error occurred during execution. */
        ERROR
    };

    /** The state of the {@link RunnableReader} object. */
    private State state;

    /** The reader. */
    private final Reader reader;

    /** The buffer. */
    private final StringBuffer buffer;

    /** The buffer size for {@link Reader#read(char[])} calls in bytes. */
    private final int bufferSize;

    /** Whether the thread should stop. */
    private volatile boolean shouldStop = false;

    /** The listeners. */
    private final List<RunnableReaderListener> listeners = new ArrayList<>();

    /**
     * Instantiates a new runnable reader listener.
     *
     * @param pReader
     *            the reader
     * @param pBuffer
     *            the buffer
     */
    public RunnableReader(final Reader pReader,
            final StringBuffer pBuffer) {
        this(pReader, pBuffer, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Instantiates a new runnable reader listener.
     *
     * @param pReader
     *            the reader
     * @param pBuffer
     *            the buffer
     * @param pBufferSize
     *            the buffer size
     */
    public RunnableReader(final Reader pReader,
            final StringBuffer pBuffer, final int pBufferSize) {
        super();

        Validate.notNull(pReader, "The reader must not be null");
        Validate.notNull(pBuffer, "The buffer must not be null");
        Validate.isTrue(pBufferSize > 0,
                " The buffer size must be a positive number");

        state = State.CREATED;

        reader = pReader;
        buffer = pBuffer;
        bufferSize = pBufferSize;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {
        readAllToBuffer();
    }

    /**
     * Reads all the data from the reader to the buffer.
     */
    public void readAllToBuffer() {
        validateState(State.CREATED);

        // init
        state = State.STARTED;
        char[] lBuffer = new char[bufferSize];
        int charactersRead;

        try {
            // read until the process is running
            shouldStop = false;
            while (!shouldStop) {
                charactersRead = read(lBuffer);

                if (charactersRead > 0) {
                    for (RunnableReaderListener listener : listeners) {
                        listener.onRead(this, charactersRead);
                    }

                    buffer.append(lBuffer, 0, charactersRead);
                } else if (charactersRead < 0) {
                    break;
                }
            }

            if (shouldStop) {
                // the execution was stopped
                state = State.STOPPED;
            } else {
                // the execution has finished
                state = State.FINISHED;
            }
        } catch (IOException e) {
            state = State.ERROR;

            for (RunnableReaderListener listener : listeners) {
                listener.onIOException(this, e);
            }
        }

        for (RunnableReaderListener listener : listeners) {
            listener.onComplete(this);
        }
    }

    /**
     * Initiates run stop.
     */
    public void initiateStop() {
        shouldStop = true;
    }

    /**
     * Gets the state of the {@link RunnableReader extends Reader} object.
     *
     * @return the state of the {@link RunnableReader extends Reader} object
     */
    public State getState() {
        return state;
    }

    /**
     * Gets the buffer.
     *
     * @return the buffer
     */
    public StringBuffer getBuffer() {
        return buffer;
    }

    /**
     * Gets the buffer size for {@link Reader#read(char[])} calls in bytes.
     *
     * @return the buffer size for {@link Reader#read(char[])} calls in bytes
     */
    public int getBufferSize() {
        return bufferSize;
    }

    /**
     * Validates the state.
     *
     * @param required
     *            the required state
     */
    private void validateState(final State required) {
        Validate.validState(state.equals(required),
                "Invalid state (state: [%s], required: [%s])", state,
                required);
    }

    /**
     * Adds a listener.
     *
     * @param listener
     *            the listener
     */
    public void addListener(final RunnableReaderListener listener) {
        if (listener == null) {
            return;
        }

        listeners.add(listener);
    }

    /**
     * Removes a listener.
     *
     * @param listener
     *            the listener
     */
    public void removeListener(final RunnableReaderListener listener) {
        listeners.remove(listener);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.io.Reader#read(char[], int, int)
     */
    @Override
    public int read(final char[] cbuf, final int off, final int len)
            throws IOException {
        return reader.read(cbuf, off, len);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.io.Reader#close()
     */
    @Override
    public void close() throws IOException {
        reader.close();
    }
}
