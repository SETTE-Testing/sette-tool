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
package hu.bme.mit.sette.common.validator.reflection;

/**
 * Enum for Java class types.
 */
public enum ClassType {
    /** Any class. */
    CLASS,
    /** Regular class. */
    REGULAR_CLASS,
    /** Member class (i.e. nested). */
    MEMBER_CLASS,
    /** Anonymous class (e.g. inline interface implementation). */
    ANONYMOUS_CLASS,
    /** Local class (e.g. defined within a method). */
    LOCAL_CLASS,

    /** Any interface. */
    INTERFACE,
    /** Regular interface. */
    REGULAR_INTERFACE,
    /** Member interface (i.e. nested). */
    MEMBER_INTERFACE,

    /** Any enum. */
    ENUM,
    /** Regular enum. */
    REGULAR_ENUM,
    /** Member enum (i.e. nested). */
    MEMBER_ENUM,

    /** Any annotation. */
    ANNOTATION,
    /** Regular annotation. */
    REGULAR_ANNOTATION,
    /** Member annotation (i.e. nested). */
    MEMBER_ANNOTATION,

    /** Primitive type. */
    PRIMITIVE,
    /** Array. */
    ARRAY
}
