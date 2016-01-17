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
package hu.bme.mit.sette.core.util.reflection;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.Comparator;

/**
 * Comparator to order {@link Class} objects by their full names ({@link Class#getName()}).
 */
public final class ExecutableComparator implements Comparator<Executable> {
    public static final ExecutableComparator INSTANCE = new ExecutableComparator();

    private ExecutableComparator() {
        // use INSTANCE instead
    }

    @Override
    public int compare(Executable o1, Executable o2) {
        if (o1 == o2) {
            return 0;
        } else if (o1 == null) {
            return -1;
        } else if (o2 == null) {
            return 1;
        }

        // compare class
        int cmp = ClassComparator.INSTANCE.compare(o1.getDeclaringClass(), o2.getDeclaringClass());

        if (cmp == 0) {
            // same class, constructors should be before methods
            cmp = Boolean.compare(o1 instanceof Method, o2 instanceof Method);

            if (cmp == 0) {
                // same type, compare name (for constructors it is the same)
                cmp = o1.getName().compareTo(o2.getName());
                if (cmp == 0) {
                    // same name, compare parameter names
                    for (int i = 0; i < Math.min(o1.getParameterCount(),
                            o2.getParameterCount()); i++) {
                        Class<?> p1 = o1.getParameterTypes()[i];
                        Class<?> p2 = o2.getParameterTypes()[i];
                        cmp = ClassComparator.INSTANCE.compare(p1, p2);

                        if (cmp != 0) {
                            // different parameter, done
                            return cmp;
                        }
                    }

                    // same beginning of the parameter list, shorter should be before longer
                    return Integer.compare(o1.getParameterCount(), o2.getParameterCount());
                } else {
                    return cmp;
                }
            } else {
                return cmp;
            }
        } else {
            return cmp;
        }
    }
}
