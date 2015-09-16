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
// TODO z revise this file
package hu.bme.mit.sette.common.validator;

import hu.bme.mit.sette.common.validator.exceptions.ValidationException;
import hu.bme.mit.sette.common.validator.exceptions.ValidatorException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.Validate;

/**
 * Base validator class for object validation.
 *
 * @param <T>
 *            Type of the subject which will be validated.
 */
public abstract class AbstractValidator<T> {
    /** The exceptions (validation errors). */
    private final List<ValidationException> exceptions;

    /** The subject. */
    private final T subject;

    /** The children validators of this validator. */
    private final List<AbstractValidator<?>> children;

    /**
     * Instantiates a new abstract validator.
     *
     * @param subject
     *            the subject
     */
    public AbstractValidator(T subject) {
        this.subject = subject;
        exceptions = new ArrayList<>();
        children = new ArrayList<>();
    }

    /**
     * Gets the subject.
     *
     * @return the subject
     */
    public final T getSubject() {
        return this.subject;
    }

    /**
     * Gets the number of exceptions.
     *
     * @return the number of exceptions
     */
    public final int getExceptionCount() {
        int count = this.exceptions.size();

        for (AbstractValidator<?> v : this.children) {
            count += v.getExceptionCount();
        }

        return count;
    }

    /**
     * Checks if the subject is valid.
     *
     * @return true, if the subject is valid, otherwise false
     */
    public final boolean isValid() {
        if (!this.exceptions.isEmpty()) {
            return false;
        } else {
            for (AbstractValidator<?> v : this.children) {
                if (!v.isValid()) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Validates the object.
     *
     * @throws ValidatorException
     *             if validation has failed
     */
    public final void validate() throws ValidatorException {
        if (!this.isValid()) {
            throw new ValidatorException(this);
        }
    }

    /**
     * Gets the exceptions (validation errors).
     *
     * @return the exceptions (validation errors)
     */
    public final List<ValidationException> getExceptions() {
        return Collections.unmodifiableList(this.exceptions);
    }

    /**
     * Adds an exception to the validator.
     *
     * @param exception
     *            the exception
     */
    public final void addException(ValidationException exception) {
        Validate.notNull("The exception must not be null");
        this.exceptions.add(exception);
    }

    /**
     * Adds an exception to the validator.
     *
     * @param message
     *            the message
     */
    public final void addException(String message) {
        addException(message, null);
    }

    /**
     * Adds an exception to the validator.
     *
     * @param message
     *            the message
     * @param cause
     *            the cause
     */
    public abstract void addException(String message, Throwable cause);

    /**
     * Gets the children validators of this validator.
     *
     * @return the children validators of this validator
     */
    public final List<AbstractValidator<?>> getChildren() {
        return Collections.unmodifiableList(children);
    }

    /**
     * Returns a list of all the validators in the hierarchy.
     *
     * @return a list of all the validators in the hierarchy.
     */
    public final List<AbstractValidator<?>> getAllValidators() {
        List<AbstractValidator<?>> list = new ArrayList<>();
        addChildrenTo(list);
        return list;
    }

    /**
     * Adds the children validators to the given collection (recursively).
     *
     * @param collection
     *            the collection
     */
    private void addChildrenTo(Collection<AbstractValidator<?>> collection) {
        collection.add(this);
        for (AbstractValidator<?> v : this.children) {
            v.addChildrenTo(collection);
        }
    }

    /**
     * Returns a list of the exceptions stored by the validator hierarchy.
     *
     * @return a list of the exceptions stored by the validator hierarchy.
     */
    public final List<ValidationException> getAllExceptions() {
        List<ValidationException> list = new ArrayList<>();
        addExceptionsTo(list);
        return list;

    }

    /**
     * Adds the exceptions to the given collection (recursively).
     *
     * @param collection
     *            the collection
     */
    private void addExceptionsTo(Collection<ValidationException> collection) {
        collection.addAll(this.exceptions);
        for (AbstractValidator<?> v : this.children) {
            v.addExceptionsTo(collection);
        }
    }

    /**
     * Adds the given validator to this as a child.
     *
     * @param validator
     *            the validator
     */
    public final void addChild(AbstractValidator<?> validator) {
        // TODO avoid circles
        Validate.notNull("The validator must not be null");
        this.children.add(validator);
    }

    /**
     * Adds the given validator to this as a child if the given validator is invalid.
     *
     * @param validator
     *            the validator
     */
    public final void addChildIfInvalid(AbstractValidator<?> validator) {
        // TODO avoid circles
        Validate.notNull("The validator must not be null");
        if (!validator.isValid()) {
            this.children.add(validator);
        }
    }
}
