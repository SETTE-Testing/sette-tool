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

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Stream.concat;

import java.util.stream.Stream;

import hu.bme.mit.sette.core.SetteException;
import lombok.Getter;
import lombok.NonNull;

/**
 * Exception class for validation failures. The exception message will be usually multi-line,
 * containing all the errors for the validator or for the validation context.
 */
public final class ValidationException extends SetteException {
    private static final long serialVersionUID = -7969479308454420853L;

    /** The validator. */
    @Getter
    private final transient Validator<?> validator;

    /**
     * Instantiates a new validation exception.
     *
     * @param validator
     *            the validator (must not be <code>null</code>)
     * @throws IllegalArgumentException
     *             if the validator contains no errors
     */
    public ValidationException(@NonNull Validator<?> validator) {
        super(createMessage(validator));
        this.validator = validator;
    }

    /**
     * Creates an exception message for a validator.
     * 
     * @param validator
     *            the validator
     * @return list of the lines of the exception message
     * @throws IllegalArgumentException
     *             if the validator contains no errors
     */
    private static String createMessage(@NonNull Validator<?> validator) {
        checkArgument(!validator.isValid(),
                "Cannot create exception for validator with no errors");

        Stream<String> lines = concat(
                Stream.of("Validation has failed for " + validator.getSubject()),
                validator.toStringLines(5));
        return lines.collect(joining("\n"));
    }
}
