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

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

import hu.bme.mit.sette.core.util.LazyImmutable;
import lombok.Getter;
import lombok.NonNull;

/**
 * Base validator class for object validation. Please note that modifying the subject during
 * validation because the validator may get to an inconsistent state. The purpose of this class is
 * to validate several requirements on an object at the same time. If {@link #validate()}
 * invalidates the subject, an exception will be thrown containing all the errors, nut just the
 * first one.
 * <p>
 * This class is also able to maintain a complete validator hierarchy, which is a tree-like
 * structure. A validator is valid if it and all of its children are valid. If {@link #validate()}
 * throws an exception, it will contain the errors of all the validators. The
 * {@link #addChild(Validator)} method ensures that the hierarchy will remain truee.
 *
 * @param <T>
 *            Type of the subject which will be validated.
 */
public class Validator<T> {
    /**
     * Creates a new {@link Validator} instance.
     *
     * @param <T>
     *            the type of the subject
     * @param subject
     *            the subject
     * @return a new {@link Validator} instance
     */
    public static <T> Validator<T> of(@NonNull T subject) {
        return new Validator<>(subject);
    }

    /** The subject under validation. */
    @Getter
    private final T subject;

    /** List of validation errors. */
    private final List<ValidationError> errors = new LinkedList<>();

    /** The parent of this validator (only can be set once). */
    private final LazyImmutable<Validator<?>> parent = LazyImmutable.of();

    /** List of children validators. */
    private final List<Validator<?>> children = new LinkedList<>();

    /**
     * Initialises the class.
     *
     * @param subject
     *            the subject, must not be <code>null</code> (<code>null</code> values should be
     *            checked before using a validator)
     */
    Validator(@NonNull T subject) {
        this.subject = subject;
    }

    /**
     * @return a stream of validation errors (does not include the children)
     */
    final Stream<ValidationError> getErrors() {
        return errors.stream();
    }

    /**
     * @return number of validation errors (including the errors in the children validators)
     */
    public final int getTotalErrorCount() {
        return errors.size() + getAllChildren().mapToInt(v -> v.errors.size()).sum();
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
     * @return the parent of this validator or <code>null</code> if not present
     */
    public final Validator<?> getParent() {
        return parent.get();
    }

    /**
     * @return the root validator in the hierarchy
     */
    public final Validator<?> getRoot() {
        Validator<?> v = this;
        while (v.parent.isSet()) {
            v = v.getParent();
        }
        return v;
    }

    /**
     * @return a stream of the children validators
     */
    final Stream<Validator<?>> getChildren() {
        return children.stream();
    }

    /**
     * @return a stream of the all validators in the hierarchy
     */
    final Stream<Validator<?>> getAllChildren() {
        return Stream.concat(
                children.stream(),
                children.stream().flatMap(Validator::getAllChildren));
    }

    /**
     * Adds the specified validator to the hierarchy. The validator to add must not be the same
     * instance, must not belont to anywhere and most not be the root of the hierarchy.
     * 
     * @param child
     *            The validator to add
     */
    public final void addChild(@NonNull Validator<?> child) {
        // child: not this, no parents, not the root
        checkArgument(this != child, "The child must not be the same validator");
        checkArgument(!child.parent.isSet(), "The child must not belong anywhere");
        checkArgument(getRoot() != child, "The child must not bee the root if this hierarchy");

        children.add(child);
        child.parent.set(this);
    }

    /**
     * Adds the specified validator to the hierarchy if it invalidates its subject. The validator to
     * add must not be the same instance, must not belont to anywhere and most not be the root of
     * the hierarchy.
     * <p>
     * Note: only add validators using this method if you they will not be used after the addition.
     * 
     * @param child
     *            The validator to add
     */
    public final void addChildIfInvalid(@NonNull Validator<?> child) {
        if (!child.isValid()) {
            addChild(child);
        }
    }

    /**
     * Checks if the subject is valid.
     *
     * @return <code>true</code> if the subject is valid, otherwise <code>false</code>
     */
    public final boolean isValid() {
        return getTotalErrorCount() == 0;
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
        // with no stack trace
        return toString(0);
    }

    /**
     * Returns a string representation of the validator, containing all the errors in the hierarchy.
     * 
     * @param stackTraceDepth
     *            the depth of the stack trace to display (for no stack trace, specify
     *            <code>0</code>, for full stact trace specify {@link Integer#MAX_VALUE})
     * @return the string representation of the validator
     */
    public final String toString(int stackTraceDepth) {
        checkArgument(stackTraceDepth >= 0);
        return toStringLines(stackTraceDepth).collect(joining("\n"));
    }

    private static final String TO_STRING_INDENT = "    ";

    /**
     * Returns a stream of the lines of the string representation of the validator, containing all
     * the errors in the hierarchy.
     * 
     * @param stackTraceDepth
     *            the depth of the stack trace to display (for no stack trace, specify
     *            <code>0</code>, for full stact trace specify {@link Integer#MAX_VALUE})
     * @return a stream of the lines of the string representation of the validator
     */
    public final Stream<String> toStringLines(int stackTraceDepth) {
        checkArgument(stackTraceDepth >= 0);

        Stream<String> selfLines = concat(
                Stream.of(String.format("[V] %s: %d error(s)", subject, getTotalErrorCount())),
                errors.stream().flatMap(e -> createErrorLines(e, stackTraceDepth)));

        Stream<String> childrenLines = getAllChildren()
                .filter(v -> !v.isValid())
                .flatMap(v -> v.toStringLines(stackTraceDepth))
                .map(l -> TO_STRING_INDENT + l);

        return concat(selfLines, childrenLines);
    }

    private static final Stream<String> createErrorLines(ValidationError error,
            int stackTraceDepth) {
        Stream<String> selfLine = Stream.of(TO_STRING_INDENT + "[E] " + error.getMessage());

        Stream<String> stackTraceLines = error.getStackTrace()
                .stream()
                .map(s -> TO_STRING_INDENT + TO_STRING_INDENT + "[S] " + s)
                .limit(stackTraceDepth);

        return concat(selfLine, stackTraceLines);
    }
}
