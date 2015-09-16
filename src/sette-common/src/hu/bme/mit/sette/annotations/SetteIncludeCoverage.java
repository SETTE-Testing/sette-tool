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
package hu.bme.mit.sette.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Stores the methods whose coverage should be considered when measuring the coverage on the code
 * snippet. This annotation can be applied on only code snippet functions.
 *
 * Example:
 *
 * <pre>
 * <code>
 * import dependencies.MyClass1;
 * import dependencies.MyClass2;
 * // more imports
 * 
 * {@literal @}SetteUseAll
 * {@literal @}SetteGoal(...)
 * class SetteClass {
 *     // ...
 * 
 *     {@literal @}SetteIncludeCoverage(
 *       classes = {
 *         MyClass1.class,
 *         MyClass2.class,
 *         MyClass2.class,
 *         SetteClass.class
 *       },
 *       methods = {
 *         "*",
 *         "method1(int)",
 *         "method2(int)",
 *         "method(java.lang.Integer, my.library.MyClass3)"
 *       }
 *     )
 *     public static void myMethod() { ... }
 * 
 *     // ...
 * }
 * </code>
 * </pre>
 *
 * This sample describes that all the methods of MyClass1, two methods of MyClass2 and a method of
 * the same class should be considered when examining coverage. In method parameter lists
 * non-primitive types should be specified by full class name.
 *
 * Constructors can be also specified as methods.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@SetteAnnotation
public @interface SetteIncludeCoverage {
    /**
     * The array of classes.
     */
    Class<?>[]classes();

    /**
     * The array of methods.
     */
    String[]methods();
}
