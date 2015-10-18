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
package hu.bme.mit.sette.core.validator;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;

import lombok.Getter;
import lombok.NonNull;

/**
 * Represents a validation error described with a message and a stack trace.
 */
public final class ValidationError {
    /** Error message. */
    @Getter
    private final String message;

    /** Stack trace describing the location where the error happened (immutable). */
    @Getter
    private final ImmutableList<StackTraceElement> stackTrace;

    public ValidationError(@NonNull String message) {
        this.message = message;

        // collect the stack trace
        StackTraceElement[] fullStackTrace = Thread.currentThread().getStackTrace();

        // skip 2 elements: call to Thread, and constructor
        Stream<StackTraceElement> stream = Arrays.stream(fullStackTrace).skip(2);

        this.stackTrace = ImmutableList.copyOf(stream.collect(Collectors.toList()));
    }

    @Override
    public String toString() {
        return "ValidationError [message=" + message + ", stackTrace=" + stackTrace + "]";
    }
}
