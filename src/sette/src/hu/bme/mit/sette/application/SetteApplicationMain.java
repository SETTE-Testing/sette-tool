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
package hu.bme.mit.sette.application;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.Thread.UncaughtExceptionHandler;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hu.bme.mit.sette.core.configuration.SetteConfigurationException;

/**
 * Contains the entry point for the application.
 */
public final class SetteApplicationMain {
    private static final Logger LOG = LoggerFactory.getLogger(SetteApplicationMain.class);

    /**
     * Entry point for the application. After program initialisation, instantiates
     * {@link SetteApplication} and calls {@link SetteApplication#execute(String[])} with the
     * arguments.
     * 
     * @param args
     *            program arguments
     */
    public static void main(String... args) {
        Thread.currentThread().setName("MAIN");
        LOG.info("main() called, arguments: {}", (Object) args);

        // check charset (usually ok on Linux, but might be a problem on Windows)
        Charset utf8Charset = Charset.forName("UTF-8");
        String notUtfFileEncodingMsg = "Please set the 'file.encoding' system property to 'UTF-8' "
                + "(tip: set the JAVA_TOOL_OPTIONS environment variable to '-Dfile.encoding=UTF-8' "
                + "since it may be required )";
        try {
            Charset fileEncodingCharset = Charset.forName(System.getProperty("file.encoding", ""));
            if (utf8Charset != fileEncodingCharset) {
                LOG.error(notUtfFileEncodingMsg);
            }
        } catch (IllegalCharsetNameException ex) { // NOSONAR
            LOG.error(notUtfFileEncodingMsg);
        }

        if (utf8Charset != Charset.defaultCharset()) {
            LOG.error("Please set the default charset to UTF-8 from the current "
                    + Charset.defaultCharset().name());
        }

        // set locale
        try {
            Locale.setDefault(new Locale("en", "GB"));
        } catch (Exception ex) { // NOSONAR
            Locale.setDefault(Locale.ENGLISH);
        }

        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                if (ex instanceof ThreadDeath) {
                    // FIXME this solution was bound to JUnit 3, try interrupt() with JUnit 4.12
                    // required for test runner if it stops a thread with Thread.stop()
                    LOG.warn("Thread death: " + Thread.currentThread().getName(), ex);
                } else {
                    String stackTrace = ExceptionUtils.getStackTrace(ex);
                    if (stackTrace.contains("at hu.bme.mit.sette.snippets")) {
                        // FIXME ugly detection of snippet exceptions from separate threads
                        LOG.error("Exception from a snippet, thread: " + thread.getName(), ex);
                    } else {
                        LOG.error("Uncaught exception, thread: " + thread.getName(), ex);
                        System.exit(1);
                    }
                }
            }
        });

        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        Path configFile = Paths.get("sette.config.json");

        SetteApplication app;
        try {
            app = new SetteApplication(input, System.out, System.err, configFile); // NOSONAR
            app.execute(args);
        } catch (SetteConfigurationException ex) {
            LOG.error("Invalid configuration", ex);
        }

        LOG.info("main() has finished");
    }

    private SetteApplicationMain() {
        throw new UnsupportedOperationException("Static class");
    }
}
