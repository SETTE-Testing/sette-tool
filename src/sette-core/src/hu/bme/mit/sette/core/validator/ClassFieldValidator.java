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
// NOTE revise this file
package hu.bme.mit.sette.core.validator;

import java.lang.reflect.Field;

/**
 * Validator for fields.
 */
public final class ClassFieldValidator extends ReflectionValidator<Field> {

    /**
     * Instantiates a new field validator.
     *
     * @param field
     *            the field
     */
    public ClassFieldValidator(Field field) {
        super(field);
    }

    /**
     * Specifies required modifiers for the field.
     *
     * @param modifiers
     *            required modifiers for the field.
     * @return this object
     */
    public ClassFieldValidator withModifiers(int modifiers) {
        addErrorIfNotWithModifiers(modifiers, getSubject().getModifiers());
        return this;
    }

    /**
     * Specifies prohibited modifiers for the field.
     *
     * @param modifiers
     *            prohibited modifiers for the field.
     * @return this object
     */
    public ClassFieldValidator withoutModifiers(int modifiers) {
        addErrorIfNotWithoutModifiers(modifiers, getSubject().getModifiers());
        return this;
    }
}
