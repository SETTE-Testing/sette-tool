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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import com.google.common.collect.ImmutableList;

import lombok.Getter;
import lombok.NonNull;

/**
 * Simple validator class for object validation. Please not that modifying the subject during
 * validation because the validator may get to an inconsistent state. The purpose of this class is
 * to validate several requirements on an object at the same time. If {@link #validate()}
 * invalidates the subject, an exception will be thrown containing all the errors, nut just the
 * first one.
 *
 * @param <T>
 *            Type of the subject which will be validated.
 */
public class Validator<T> {
    /** The subject under validation. */
    @Getter
    private final T subject;

    /** List of validation errors. */
    private final List<ValidationError> errors;

    /**
     * Initializes the class.
     *
     * @param subject
     *            the subject, must not be <code>null</code> (<code>null</code> values should be
     *            checked before using a validator)
     */
    public Validator(@NonNull T subject) {
        this.subject = subject;
        errors = new ArrayList<>();
    }

    /**
     * Gets the number of validation errors.
     *
     * @return the number of validation errors
     */
    public final int getErrorCount() {
        return errors.size();
    }

    /**
     * Gets an immutable list of errors representing the current state. Please note that this list
     * will not be updated when a new error is added to the validator.
     * 
     * @return an immutable list of errors
     */
    public final ImmutableList<ValidationError> getErrors() {
        return ImmutableList.copyOf(errors);
    }

    /**
     * Adds a validation error to the validator.
     * 
     * @param message
     *            the error message
     */
    public final void addError(@NonNull String message) {
        errors.add(new ValidationError(message));
    }

    /**
     * Adds a validation error to the validator if the expression is <code>true</code>.
     * 
     * @param message
     *            the error message
     * @param expression
     *            the expression
     */
    public final void addErrorIfTrue(@NonNull String message, boolean expression) {
        // do not use other addError* methods to avoid spamming the stack trace
        if (expression) {
            errors.add(new ValidationError(message));
        }
    }

    /**
     * 
     * Adds a validation error to the validator if the predicate evaluates as <code>true</code> on
     * the subject.
     * 
     * @param message
     *            the error message
     * @param predicate
     *            the predicate
     */
    public final void addErrorIfTrue(@NonNull String message, @NonNull Predicate<T> predicate) {
        // do not use other addError* methods to avoid spamming the stack trace
        if (predicate.test(subject)) {
            errors.add(new ValidationError(message));
        }
    }

    /**
     * Adds a validation error to the validator if the expression is <code>false</code>.
     * 
     * @param message
     *            the error message
     * @param expression
     *            the expression
     */
    public final void addErrorIfFalse(@NonNull String message, boolean expression) {
        // do not use other addError* methods to avoid spamming the stack trace
        if (!expression) {
            errors.add(new ValidationError(message));
        }
    }

    /**
     * Adds a validation error to the validator if the predicate evaluates as <code>false</code> on
     * the subject.
     * 
     * @param message
     *            the error message
     * @param predicate
     *            the predicate
     */
    public final void addErrorIfFalse(@NonNull String message, @NonNull Predicate<T> predicate) {
        // do not use other addError* methods to avoid spamming the stack trace
        if (!predicate.test(subject)) {
            errors.add(new ValidationError(message));
        }
    }

    /**
     * Adds a validation error to the validator if the expected value does not equal to the actual
     * one.
     * 
     * @param property
     *            the name of the validated property of the subject (used in the error message)
     * @param expected
     *            the expected value
     * @param actual
     *            the actual value
     */
    public final <V> void addErrorIfNotEquals(String property, V expected, V actual) {
        // do not use other addError* methods to avoid spamming the stack trace
        if (!Objects.equals(expected, actual)) {
            String msg = String.format("%s: expected %s instead of %s", property, expected, actual);
            errors.add(new ValidationError(msg));
        }
    }

    /**
     * Checks if the subject is valid.
     *
     * @return <code>true</code> if the subject is valid, otherwise <code>false</code>
     */
    public final boolean isValid() {
        return errors.isEmpty();
    }

    /**
     * Validates the object.
     *
     * @throws ValidationException
     *             if validation fails
     */
    public final void validate() throws ValidationException {
        if (!isValid()) {
            throw new ValidationException(this);
        }
    }

    @Override
    public final String toString() {
        return String.format("%s [subject=%s, subjectClass=%s, errorCount=%d]",
                getClass().getSimpleName(), subject, subject.getClass().getName(), getErrorCount());
    }
}
