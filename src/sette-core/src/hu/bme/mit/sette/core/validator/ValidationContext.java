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
package hu.bme.mit.sette.core.validator;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;

import lombok.Getter;
import lombok.NonNull;

/**
 * The purpose of this class to provide functionality for validating several validators at the same
 * time by adding them to the same context. A validation context is valid if all the subjects of the
 * validators are valid. If not, an exception will be thrown by {@link #validate()} containing all
 * the errors of all the failing validators, not just the first one.
 */
public final class ValidationContext {
    /** An object describing the context of the validation. */
    @Getter
    private final Object context;
    /**
     * List of validators related to the context. This list does not contain duplicated validators.
     */
    // List is used instead of Set to preserve order
    private final List<Validator<?>> validators = new ArrayList<>();

    /**
     * Initializes the class.
     *
     * @param context
     *            an object describing the context of the validation
     */
    public ValidationContext(@NonNull Object context) {
        this.context = context;
    }

    /**
     * Gets an immutable list of validators representing the current state. Please note that this
     * list will not be updated when a new validator is added to the context.
     * 
     * @return an immutable list of validators
     */
    public ImmutableList<Validator<?>> getValidators() {
        return ImmutableList.copyOf(validators);
    }

    /**
     * Adds a validator to the validation context. If the validator is already present, it will not
     * be added again.
     * 
     * @param validator
     *            the validator
     */
    public void addValidator(Validator<?> validator) {
        if (!validators.contains(validator)) {
            validators.add(validator);
        }
    }

    /**
     * Adds a validator to the validation context. If the validator validates the subject or it is
     * already present, it will not be added again. Please note that this method should only be used
     * if you do not use the validator after passing it to this method in order to prevent
     * inconsistency. The advantage of this method is that if you are running validation on a huge
     * context, unnecessary validators will not be added to this object.
     * 
     * @param validator
     *            the validator
     */
    public void addValidatorIfInvalid(Validator<?> validator) {
        if (!validator.isValid() && !validators.contains(validator)) {
            validators.add(validator);
        }
    }

    /**
     * Gets the number of errors for this and all the children.
     *
     * @return the number of errors
     */
    public int getErrorCount() {
        return validators.stream().mapToInt(v -> v.getErrorCount()).sum();
    }

    /**
     * Checks if all validators are valid.
     *
     * @return <code>true</code> if all validators are valid, otherwise <code>false</code>
     */
    public boolean isValid() {
        return !validators.stream().anyMatch(v -> !v.isValid());
    }

    /**
     * Performs validation on all validators.
     *
     * @throws ValidationException
     *             if validation fails
     */
    public void validate() throws ValidationException {
        if (!isValid()) {
            throw new ValidationException(this);
        }
    }

    @Override
    public String toString() {
        return String.format("%s [context=%s, contextClass=%s, errorCount=%d]",
                getClass().getSimpleName(), context, context.getClass().getName(), getErrorCount());
    }
}
