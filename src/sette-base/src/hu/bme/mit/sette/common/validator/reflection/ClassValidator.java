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
package hu.bme.mit.sette.common.validator.reflection;

import hu.bme.mit.sette.common.validator.AbstractValidator;
import hu.bme.mit.sette.common.validator.exceptions.reflection.ClassValidationException;

import java.lang.reflect.Modifier;

import org.apache.commons.lang3.Validate;

/**
 * Validator for Java classes.
 */
public final class ClassValidator extends AbstractValidator<Class<?>> {
    /**
     * Instantiates a new class validator.
     *
     * @param javaClass
     *            the Java class
     */
    public ClassValidator(final Class<?> javaClass) {
        super(javaClass);

        if (javaClass == null) {
            this.addException("The Java class must not be null");
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * hu.bme.mit.sette.common.validator.AbstractValidator#addException(java
     * .lang.String, java.lang.Throwable)
     */
    @Override
    public void addException(final String message, final Throwable cause) {
        this.addException(new ClassValidationException(message,
                getSubject(), cause));
    }

    /**
     * Sets the required type for the Java class.
     *
     * @param type
     *            the required type for the Java class.
     * @return this object
     */
    public ClassValidator type(final ClassType type) {
        Validate.notNull(type, "The type must not be null");

        if (getSubject() != null) {
            Class<?> javaClass = getSubject();
            boolean isTypeValid = false;

            switch (type) {
            case CLASS:
                isTypeValid = !javaClass.isInterface()
                && !javaClass.isEnum()
                && !javaClass.isAnnotation()
                && !javaClass.isPrimitive()
                && !javaClass.isArray();
                break;

            case REGULAR_CLASS:
                isTypeValid = !javaClass.isInterface()
                && !javaClass.isEnum()
                && !javaClass.isAnnotation()
                && !javaClass.isPrimitive()
                && !javaClass.isArray()
                && !javaClass.isMemberClass()
                && !javaClass.isAnonymousClass()
                && !javaClass.isLocalClass();
                break;

            case MEMBER_CLASS:
                isTypeValid = !javaClass.isInterface()
                && !javaClass.isEnum()
                && !javaClass.isAnnotation()
                && !javaClass.isPrimitive()
                && !javaClass.isArray()
                && javaClass.isMemberClass();
                break;

            case ANONYMOUS_CLASS:
                isTypeValid = !javaClass.isInterface()
                && !javaClass.isEnum()
                && !javaClass.isAnnotation()
                && !javaClass.isPrimitive()
                && !javaClass.isArray()
                && javaClass.isAnonymousClass();
                break;

            case LOCAL_CLASS:
                isTypeValid = !javaClass.isInterface()
                && !javaClass.isEnum()
                && !javaClass.isAnnotation()
                && !javaClass.isPrimitive()
                && !javaClass.isArray()
                && javaClass.isLocalClass();
                break;

            case INTERFACE:
                isTypeValid = javaClass.isInterface();
                break;

            case REGULAR_INTERFACE:
                isTypeValid = javaClass.isInterface()
                && !javaClass.isMemberClass();
                break;

            case MEMBER_INTERFACE:
                isTypeValid = javaClass.isInterface()
                && javaClass.isMemberClass();
                break;

            case ENUM:
                isTypeValid = javaClass.isEnum();
                break;

            case REGULAR_ENUM:
                isTypeValid = javaClass.isEnum()
                && !javaClass.isMemberClass();
                break;

            case MEMBER_ENUM:
                isTypeValid = javaClass.isEnum()
                && javaClass.isMemberClass();
                break;

            case ANNOTATION:
                isTypeValid = javaClass.isAnnotation();
                break;

            case REGULAR_ANNOTATION:
                isTypeValid = javaClass.isAnnotation()
                && !javaClass.isMemberClass();
                break;

            case MEMBER_ANNOTATION:
                isTypeValid = javaClass.isAnnotation()
                && javaClass.isMemberClass();
                break;

            case PRIMITIVE:
                isTypeValid = javaClass.isPrimitive();
                break;

            case ARRAY:
                isTypeValid = javaClass.isArray();
                break;

            default:
                throw new UnsupportedOperationException(
                        "Unknown class type: " + type);
            }

            if (!isTypeValid) {
                this.addException(String.format(
                        "The Java class must have the specified type\n"
                                + "(type: [%s])", type));
            }
        }

        return this;
    }

    /**
     * Sets the required modifiers for the Java class.
     *
     * @param modifiers
     *            the required modifiers for the Java class.
     * @return this object
     */
    public ClassValidator withModifiers(final int modifiers) {
        if (getSubject() != null) {
            Class<?> javaClass = getSubject();

            if ((javaClass.getModifiers() & modifiers) != modifiers) {
                this.addException(String
                        .format("The Java class must have all the "
                                + "specified modifiers\n(modifiers: [%s])",
                                Modifier.toString(modifiers)));
            }
        }

        return this;
    }

    /**
     * Sets the prohibited modifiers for the Java class.
     *
     * @param modifiers
     *            the prohibited modifiers for the Java class.
     * @return this object
     */
    public ClassValidator withoutModifiers(final int modifiers) {
        if (getSubject() != null) {
            Class<?> javaClass = getSubject();

            if ((javaClass.getModifiers() & modifiers) != 0) {
                this.addException(String.format(
                        "The Java class must not have "
                                + "any of the specified modifiers\n"
                                + "(modifiers: [%s])",
                                Modifier.toString(modifiers)));
            }
        }

        return this;
    }

    /**
     * Sets whether the Java class should be synthetic or not.
     *
     * @param isSynthetic
     *            true if the Java class should be synthetic, false if it should
     *            not be
     * @return this object
     */
    public ClassValidator synthetic(final boolean isSynthetic) {
        if (getSubject() != null) {
            Class<?> javaClass = getSubject();

            if (isSynthetic ^ javaClass.isSynthetic()) {
                String must;

                if (isSynthetic) {
                    must = "must";
                } else {
                    must = "must not";
                }

                this.addException(String.format(
                        "The Java class %s be synthetic", must));
            }
        }

        return this;
    }

    /**
     * Sets the required superclass for the Java class.
     *
     * @param superclass
     *            the required superclass for the Java class.
     * @return this object
     */
    public ClassValidator superclass(final Class<?> superclass) {
        Validate.notNull(superclass, "The superclass must not be null");

        if (getSubject() != null) {
            Class<?> javaClass = getSubject();

            if (!javaClass.getSuperclass().equals(superclass)) {
                this.addException(String.format(
                        "The Java class must have "
                                + "the specified superclass\n"
                                + "(superclass: [%s])", superclass));
            }
        }

        return this;
    }

    /**
     * Sets the required interface count for the Java class.
     *
     * @param interfaceCount
     *            the required interface count for the Java class.
     * @return this object
     */
    public ClassValidator interfaceCount(final int interfaceCount) {
        Validate.isTrue(interfaceCount >= 0,
                "The required interface count must be "
                        + "a non-negative number");

        if (getSubject() != null) {
            Class<?> javaClass = getSubject();

            if (javaClass.getInterfaces().length != interfaceCount) {
                if (interfaceCount == 0) {
                    this.addException("The Java class must not implement "
                            + "any interfaces");
                } else if (interfaceCount == 1) {
                    this.addException("The Java class must implement "
                            + "exactly 1 interface");
                } else {
                    this.addException(String.format(
                            "The Java class must implement "
                                    + "exactly %d interfaces",
                                    interfaceCount));
                }
            }
        }

        return this;
    }

    /**
     * Sets the required member class count for the Java class.
     *
     * @param memberClassCount
     *            the required member class count for the Java class.
     * @return this object
     */
    public ClassValidator memberClassCount(final int memberClassCount) {
        Validate.isTrue(memberClassCount >= 0,
                "The required member class count must be "
                        + "a non-negative number");

        if (getSubject() != null) {
            Class<?> javaClass = getSubject();

            if (javaClass.getClasses().length != memberClassCount) {
                if (memberClassCount == 0) {
                    this.addException("The Java class must not have "
                            + "any member classes");
                } else if (memberClassCount == 1) {
                    this.addException("The Java class must have "
                            + "exactly 1 member class");
                } else {
                    this.addException(String.format(
                            "The Java class must have "
                                    + "exactly %d member classes",
                                    memberClassCount));
                }
            }
        }

        return this;
    }

    /**
     * Sets the required declared constructor count for the Java class.
     *
     * @param declCtorCount
     *            the required declared constructor count for the Java class.
     * @return this object
     */
    public ClassValidator declaredConstructorCount(
            final int declCtorCount) {
        Validate.isTrue(declCtorCount >= 0,
                "The required declared constructor count must be "
                        + "a non-negative number");

        if (getSubject() != null) {
            Class<?> javaClass = getSubject();

            if (javaClass.getDeclaredConstructors().length != declCtorCount) {
                if (declCtorCount == 0) {
                    this.addException("The Java class must not have "
                            + "any declared constructors");
                } else if (declCtorCount == 1) {
                    this.addException("The Java class must have "
                            + "exactly 1 declared constructor");
                } else {
                    this.addException(String
                            .format("The Java class must have "
                                    + "exactly %d declared constructors",
                                    declCtorCount));
                }
            }
        }

        return this;
    }
}
