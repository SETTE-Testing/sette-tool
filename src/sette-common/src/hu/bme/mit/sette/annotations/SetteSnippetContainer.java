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
package hu.bme.mit.sette.annotations;

import hu.bme.mit.sette.common.snippets.JavaVersion;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the class is a code snippet container. The class must be
 * "public final" and it must have exactly one private constructor taking no
 * parameters and throwing an UnsupportedOperationException with the message
 * "Static class". The class can only contain static methods. Public methods are
 * considered code snippets unless the @SetteNotSnippet annotation is applied on
 * them. Code snippet method must have unique name, i.e. using the same method
 * name with different parameter lists is not allowed.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SetteAnnotation
public @interface SetteSnippetContainer {
    /**
     * The category which is checked by the code snippets in the container.
     */
    String category();

    /**
     * The goal of the code snippets in the container.
     */
    String goal();

    /**
     * Class of the input factory container or NullType.class. The input factory
     * container must be a "public final" class and it must have exactly one
     * private constructor taking no parameters and throwing an
     * UnsupportedOperationException with the message "Static class". It should
     * have exactly one method for one code snippet with the same name. Input
     * factory methods take no parameters and return a SnippetInput container
     * containing the inputs for the code snippet. With the inputs returned by
     * the input factory, the specified required coverage should be achieved.
     */
    Class<?> inputFactoryContainer();

    /**
     * The required Java version for all the snippets in the container. The
     * default requirement is Java SE 6 (1.6).
     */
    JavaVersion requiredJavaVersion() default JavaVersion.JAVA_6;
}
