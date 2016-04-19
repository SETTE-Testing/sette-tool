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

import java.util.Comparator;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.ComparisonChain;

/**
 * Comparator for sorting {@link List}s of {@link Class}es. The {@link #compare(List, List)} method
 * checks for difference between the elements of the same index. If one list is a complete prefix of
 * another list, the shorter list will precede the longer one.
 */
public class ClassListComparator implements Comparator<List<Class<?>>> {
    public static final ClassListComparator INSTANCE = new ClassListComparator();

    @Override
    public int compare(List<Class<?>> o1, List<Class<?>> o2) {
        Preconditions.checkArgument(!o1.contains(null));
        Preconditions.checkArgument(!o2.contains(null));

        ComparisonChain cmp = ComparisonChain.start();

        for (int i = 0; i < Math.min(o1.size(), o2.size()); i++) {
            cmp = cmp.compare(o1.get(i), o2.get(i), ClassComparator.INSTANCE);
        }

        return cmp.compare(o1.size(), o2.size()).result();
    }

}
