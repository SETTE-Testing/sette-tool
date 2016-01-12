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
package hu.bme.mit.sette.tests.tools.jpet;

  import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import hu.bme.mit.sette.tools.jpet.JPetTypeConverter;

@RunWith(Parameterized.class)
public final class JPetTypeConverterTest {
    private final String jPetType;
    private final String javaType;
    private final boolean bothStringDirections;
    private final Class<?> javaClass;

    private final ClassLoader classLoader;

    public JPetTypeConverterTest(String jPetType, String javaType,
            boolean bothStringDirections, Class<?> javaClass) {
        this.jPetType = jPetType;
        this.javaType = javaType;
        this.bothStringDirections = bothStringDirections;
        this.javaClass = javaClass;

        classLoader = this.getClass().getClassLoader();
    }

    @Test
    public final void test_fromJavaString() {
        Assert.assertEquals(String.format("javaType: <%s>", javaType),
                jPetType, JPetTypeConverter.fromJava(javaType));
    }

    @Test
    public final void test_toJavaString() {
        if (bothStringDirections) {
            Assert.assertEquals(
                    String.format("jPetType: <%s>", jPetType),
                    javaType, JPetTypeConverter.toJava(jPetType));
        }
    }

    @Test
    public final void test_fromJavaClass() {
        if (javaClass != null) {
            Assert.assertEquals(
                    String.format("javaClass: <%s>",
                            javaClass.getName()), jPetType,
                    JPetTypeConverter.fromJava(javaClass));
        }
    }

    @Test
    public final void test_toJavaClass() throws ClassNotFoundException {
        if (javaClass != null) {
            Assert.assertEquals(
                    String.format("jPetType: <%s>", jPetType),
                    javaClass,
                    JPetTypeConverter.toJava(jPetType, classLoader));
        }
    }

    @Parameters
    public static List<Object[]> generateData() {
        // contains test data
        // 0: jPet type (string)
        // 1: Java type (string)
        // 2: true = both directions, false = only Java->jPet direction (only
        // for string)
        // 3: Java class (null = skip)
        Object[][] testData = new Object[][] {
                // primitives
                { "B", "byte", true, byte.class },
                { "S", "short", true, short.class },
                { "I", "int", true, int.class },
                { "J", "long", true, long.class },
                { "F", "float", true, float.class },
                { "D", "double", true, double.class },
                { "Z", "boolean", true, boolean.class },
                { "C", "char", true, char.class },
                { "V", "void", true, void.class },

                // arrays - regular
                { "[B", "[B", true, byte[].class },
                { "[S", "[S", true, short[].class },
                { "[I", "[I", true, int[].class },
                { "[J", "[J", true, long[].class },
                { "[F", "[F", true, float[].class },
                { "[D", "[D", true, double[].class },
                { "[Z", "[Z", true, boolean[].class },
                { "[C", "[C", true, char[].class },

                { "[[I", "[[I", true, int[][].class },
                { "[[[I", "[[[I", true, int[][][].class },
                { "[[[[I", "[[[[I", true, int[][][][].class },

                // arrays - Java canonical, only Java->jPet
                { "[B", "byte[]", false, null },
                { "[S", "short[]", false, null },
                { "[I", "int[]", false, null },
                { "[J", "long[]", false, null },
                { "[F", "float[]", false, null },
                { "[D", "double[]", false, null },
                { "[Z", "boolean[]", false, null },
                { "[C", "char[]", false, null },

                { "[[I", "int[][]", false, null },
                { "[[[I", "int[][][]", false, null },
                { "[[[[I", "int[][][][]", false, null },

                // objects - regular
                { "Ljava/lang/Object;", "java.lang.Object", true,
                        Object.class },
                { "Ljava/lang/Integer;", "java.lang.Integer", true,
                        Integer.class },
                { "Ljava/lang/String;", "java.lang.String", true,
                        String.class },
                { "Ljava/lang/Void;", "java.lang.Void", true,
                        Void.class },
                { "Ljava/io/File;", "java.io.File", true, File.class },

                // objects - L notation, only Java->jPet
                { "Ljava/lang/Object;", "Ljava.lang.Object;", false,
                        null },
                { "Ljava/lang/Integer;", "Ljava.lang.Integer;", false,
                        null },
                { "Ljava/lang/String;", "Ljava.lang.String;", false,
                        null },
                { "Ljava/lang/Void;", "Ljava.lang.Void;", false, null },
                { "Ljava/io/File;", "Ljava.io.File;", false, null },

                // arrays of objects - regular
                { "[Ljava/lang/Object;", "[Ljava.lang.Object;", true,
                        Object[].class },
                { "[[Ljava/lang/Object;", "[[Ljava.lang.Object;", true,
                        Object[][].class },
                { "[[[Ljava/lang/Object;", "[[[Ljava.lang.Object;",
                        true, Object[][][].class },

                // arrays of objects - Java canonical, only Java->jPet
                { "[Ljava/lang/Object;", "java.lang.Object[]", false,
                        null },
                { "[[Ljava/lang/Object;", "java.lang.Object[][]",
                        false, null },
                { "[[[Ljava/lang/Object;", "java.lang.Object[][][]",
                        false, null } };

        return Arrays.asList(testData);
    }
}
