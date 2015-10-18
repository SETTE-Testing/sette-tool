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

import static com.google.common.base.Preconditions.checkArgument;

import lombok.NonNull;

/**
 * Validator for Java classes.
 */
public final class ClassValidator extends ReflectionValidator<Class<?>> {
    /**
     * Instantiates a new Java class validator.
     *
     * @param javaClass
     *            the Java class
     */
    public ClassValidator(Class<?> javaClass) {
        super(javaClass);
    }

    /**
     * Specifies that the Java class must be a regular class.
     * 
     * @return this object
     */
    public ClassValidator isRegular() {
        if (getSubject().isPrimitive() || getSubject().isArray() || getSubject().isEnum()
                || getSubject().isAnnotation() || getSubject().isInterface()) {
            addError("The class must be a regular class");
        }
        return this;
    }

    /**
     * Specifies required modifiers for the Java class.
     *
     * @param modifiers
     *            required modifiers for the Java class.
     * @return this object
     */
    public ClassValidator withModifiers(int modifiers) {
        addErrorIfNotWithModifiers(modifiers, getSubject().getModifiers());
        return this;
    }

    /**
     * Specifies prohibited modifiers for the Java class.
     *
     * @param modifiers
     *            prohibited modifiers for the Java class.
     * @return this object
     */
    public ClassValidator withoutModifiers(int modifiers) {
        addErrorIfNotWithoutModifiers(modifiers, getSubject().getModifiers());
        return this;
    }

    /**
     * Sets the required superclass for the Java class.
     *
     * @param superclass
     *            the required superclass for the Java class.
     * @return this object
     */
    public ClassValidator superclass(@NonNull Class<?> superclass) {
        addErrorIfNotEquals("superclass", superclass, getSubject().getSuperclass());
        return this;
    }

    /**
     * Specifies the required interface count for the Java class. The validation only counts the
     * directly defined interfaces, not the ones which are inherited from the superclass.
     *
     * @param interfaceCount
     *            the required interface count for the Java class.
     * @return this object
     */
    public ClassValidator interfaceCount(int interfaceCount) {
        checkArgument(interfaceCount >= 0);

        addErrorIfNotEquals("interface count", interfaceCount, getSubject().getInterfaces().length);
        return this;
    }

    /**
     * Specifies the required declared constructor count for the Java class.
     *
     * @param declaredConstructorCount
     *            the required declared constructor count for the Java class.
     * @return this object
     */
    public ClassValidator declaredConstructorCount(int declaredConstructorCount) {
        checkArgument(declaredConstructorCount >= 0);

        addErrorIfNotEquals("declared constructor count", declaredConstructorCount,
                getSubject().getDeclaredConstructors().length);
        return this;
    }
}
