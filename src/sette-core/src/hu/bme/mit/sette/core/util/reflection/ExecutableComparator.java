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

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.google.common.collect.ComparisonChain;

import lombok.NonNull;

/**
 * Comparator to sort {@link Executable} objects by:
 * <ol>
 * <li>Declaring class (using {@link ClassComparator})
 * <li>{@link Constructor}s before {@link Method}s
 * <li>Method name
 * <li>Method argument list (using {@link ClassListComparator})
 * </ol>
 */
public final class ExecutableComparator implements Comparator<Executable> {
    public static final ExecutableComparator INSTANCE = new ExecutableComparator();

    private ExecutableComparator() {
        // use INSTANCE instead
    }

    @Override
    public int compare(@NonNull Executable o1, @NonNull Executable o2) {
        if (o1 == o2) {
            return 0;
        }

        ComparisonChain cmp = ComparisonChain.start()
                .compare(o1.getDeclaringClass(), o2.getDeclaringClass(), ClassComparator.INSTANCE)
                .compareTrueFirst(o1 instanceof Constructor, o2 instanceof Constructor)
                .compare(o1.getName(), o2.getName());

        if (cmp.result() == 0) {
            List<Class<?>> params1 = Arrays.asList(o1.getParameterTypes());
            List<Class<?>> params2 = Arrays.asList(o2.getParameterTypes());
            return cmp.compare(params1, params2, ClassListComparator.INSTANCE).result();
        } else {
            return cmp.result();
        }
    }
}
