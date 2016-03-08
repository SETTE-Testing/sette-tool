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
// NOTE revise this file
package hu.bme.mit.sette.core.model.snippet;

import static com.google.common.base.Preconditions.checkArgument;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ClassUtils;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;

import hu.bme.mit.sette.common.annotations.SetteIncludeCoverage;
import hu.bme.mit.sette.common.annotations.SetteRequiredStatementCoverage;
import hu.bme.mit.sette.core.util.reflection.ExecutableComparator;
import hu.bme.mit.sette.core.util.reflection.SetteAnnotationUtils;
import hu.bme.mit.sette.core.validator.ClassExecutableValidator;
import hu.bme.mit.sette.core.validator.ValidationException;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.val;

/**
 * Represents a code snippet (which is a Java method).
 */
@Data
public final class Snippet implements Comparable<Snippet> {
    /**
     * The id of the snippet, which is generated from class name prefix and method name class + '_'
     * + name. E.g., class name: "A_1_a_MyClass", category: "A_1" method name: "myMethod", id:
     * "A_1_a_myMethod"
     */
    @Getter
    private final String id;

    /** The snippet container to which the instance belongs to. */
    @Getter
    private final SnippetContainer container;

    /** The name of the snippet, which is the name of the method. */
    @Getter
    private final String name;

    /** The {@link Method} the snippet. */
    @Getter
    private final Method method;

    /** The required statement coverage. */
    @Getter
    private final double requiredStatementCoverage;

    /** The constructors which should be considered when measuring coverage. */
    @Getter
    private final ImmutableSortedSet<Constructor<?>> includedConstructors;

    /** The methods which should be considered when measuring coverage. */
    @Getter
    private final ImmutableSortedSet<Method> includedMethods;

    /**
     * Instantiates a new snippet.
     *
     * @param container
     *            the snippet container to which the instance belongs to
     * @param method
     *            the {@link Method} of the snippet
     * @param classLoader
     *            the class loader for loading snippet project classes
     * @throws ValidationException
     *             if validation fails
     */
    Snippet(@NonNull SnippetContainer container, @NonNull Method method)
            throws ValidationException {
        checkArgument(container.getJavaClass().equals(method.getDeclaringClass()),
                "The method must be declared in the Java class of the container\n"
                        + "(container.javaClass: [%s])\n(method.declaringClass: [%s])",
                container.getJavaClass(), method.getDeclaringClass());
        this.method = method;
        this.container = container;
        name = method.getName();

        String clsName = container.getName();
        String cat = container.getCategory();
        String prefix = clsName.substring(0, clsName.indexOf('_', cat.length()));
        id = String.format("%s_%s", prefix, name);

        Parser p = new Parser(method);

        requiredStatementCoverage = p.requiredStatementCoverage;
        includedConstructors = ImmutableSortedSet.copyOf(ExecutableComparator.INSTANCE,
                p.includedConstructors);
        includedMethods = ImmutableSortedSet.copyOf(ExecutableComparator.INSTANCE,
                p.includedMethods);
    }

    /**
     * @return The input factory for the snippet.
     */
    public SnippetInputFactory getInputFactory() {
        if (container.getInputFactoryContainer() != null) {
            return container.getInputFactoryContainer().getInputFactories().get(name);
        } else {
            return null;
        }
    }

    @Override
    public int compareTo(@NonNull Snippet o) {
        return ExecutableComparator.INSTANCE.compare(method, o.method);
    }

    private static final class Parser {
        private static final double COV_MIN = SetteRequiredStatementCoverage.MIN;
        private static final double COV_MAX = SetteRequiredStatementCoverage.MAX;

        private final Method method;
        private final ClassExecutableValidator validator;
        private double requiredStatementCoverage = -1;
        private final Set<Constructor<?>> includedConstructors = new HashSet<>();
        private final Set<Method> includedMethods = new HashSet<>();

        private Parser(@NonNull Method method) throws ValidationException {
            this.method = method;
            validator = new ClassExecutableValidator(method);

            // check SETTE annotations
            val methodAnnots = SetteAnnotationUtils.getSetteAnnotations(method);

            val reqStmtCovAnn = methodAnnots.getInstance(SetteRequiredStatementCoverage.class);
            methodAnnots.remove(SetteRequiredStatementCoverage.class);

            val inclCovAnn = methodAnnots.getInstance(SetteIncludeCoverage.class);
            methodAnnots.remove(SetteIncludeCoverage.class);

            if (!methodAnnots.isEmpty()) {
                validator
                        .addError("The method has disallwed annotations: " + methodAnnots.keySet());
            }

            if (reqStmtCovAnn == null) {
                validator.addError("The required statement coverage must be declared");
            } else if (COV_MIN <= reqStmtCovAnn.value() &&
                    reqStmtCovAnn.value() <= COV_MAX) {
                requiredStatementCoverage = reqStmtCovAnn.value();
            } else {
                validator.addError(String.format(
                        "Required statement coverage must be between %.2f%% and %.2f%%", COV_MIN,
                        COV_MAX));
            }

            parseIncludedMethods(inclCovAnn);

            validator.validate();
        }

        private void parseIncludedMethods(SetteIncludeCoverage annot) {
            if (annot == null) {
                return;
            }

            // null annotation arrays and null values result in Java compilation error
            val includedClasses = ImmutableList.copyOf(annot.classes());
            val includedMethodStrings = ImmutableList.copyOf(annot.methods());

            // check the arrays: not empty, no null element, same lengths
            validator.addErrorIfTrue("The included class list must not be empty",
                    includedClasses.isEmpty());
            validator.addErrorIfTrue("The included method list must not be empty",
                    includedMethodStrings.isEmpty());
            validator.addErrorIfFalse(
                    "The included class list and method list must have the same length",
                    includedClasses.size() == includedMethodStrings.size());

            if (validator.isValid()) {
                // check and add methods
                for (int i = 0; i < includedClasses.size(); i++) {
                    Class<?> includedClass = includedClasses.get(i);
                    String includedMethodString = includedMethodStrings.get(i).trim();

                    if (includedMethodString.equals("*")) {
                        // add all non-synthetic constructors
                        for (Constructor<?> c : includedClass.getDeclaredConstructors()) {
                            if (!c.isSynthetic()) {
                                addIncludedConstructor(c);
                            }
                        }
                        // add all non-synthetic methods
                        for (Method m : includedClass.getDeclaredMethods()) {
                            if (!m.isSynthetic()) {
                                addIncludedMethod(m);
                            }
                        }
                    } else {
                        parseIncludedMethod(includedClass, includedMethodString);
                    }
                }
            }
        }

        private void addIncludedConstructor(Constructor<?> c) {
            if (includedConstructors.contains(c)) {
                // duplicate
                validator.addError(
                        "The constructor has been already added for included coverage: " + c);
            } else {
                // add method to the list
                includedConstructors.add(c);
            }
        }

        private void addIncludedMethod(Method m) {
            if (includedMethods.contains(m)) {
                // duplicate
                validator.addError("The method has been already added for included coverage: " + m);
            } else {
                // add method to the list
                includedMethods.add(m);
            }
        }

        private void parseIncludedMethod(Class<?> includedClass, String includedMethodString) {
            // Pattern for method strings in @SetteIncludeCoverage annotation
            // e.g., "methodName(int, my.pkg.MyClass)"
            final Pattern methodStringPattern = Pattern.compile("(.+)\\((.*)\\)");
            Matcher matcher = methodStringPattern.matcher(includedMethodString);

            if (!matcher.matches() || matcher.groupCount() != 2) {
                // invalid method string
                String message = String.format(
                        "The included method string must match "
                                + "the required format.\n(includedMethodString: [%s])",
                        includedMethodString);
                validator.addError(message);
            } else {
                // valid method string
                String includedMethodName = matcher.group(1).trim();
                List<String> paramTypeStrings = Lists.newArrayList(Splitter.on(',').trimResults()
                        .split(matcher.group(2)));

                Class<?>[] paramTypes = new Class<?>[paramTypeStrings.size()];
                boolean isConstructor = includedMethodName.equals(includedClass.getSimpleName());
                // the parameters

                // check parameter types
                for (int i = 0; i < paramTypes.length; i++) {
                    String parameterTypeString = paramTypeStrings.get(i).trim();

                    if (parameterTypeString.isEmpty()) {
                        // blank parameter type string
                        String message = String.format(
                                "The included method string has a blank parameter type.\n"
                                        + "(includedMethodString: [%s])\n(index: [%d])",
                                includedMethodString, i);
                        validator.addError(message);
                    } else {
                        try {
                            paramTypes[i] = ClassUtils.getClass(
                                    method.getDeclaringClass().getClassLoader(),
                                    parameterTypeString);
                        } catch (ClassNotFoundException ex) {
                            // parameter type was not found
                            String format = "The parameter type in the included method string "
                                    + "could not have been loaded.\n(includedMethodString: [%s])\n"
                                    + "(index: [%d])";
                            String message = String.format(format, includedMethodString, i);
                            validator.addError(message);
                        }
                    }
                }

                if (validator.isValid()) {
                    // get included method object
                    if (isConstructor) {
                        try {
                            // only search declared constructors
                            Constructor<?> found = includedClass.getDeclaredConstructor(paramTypes);
                            addIncludedConstructor(found);
                        } catch (NoSuchMethodException ex) {
                            String format = "Included constructor cannot be found "
                                    + "(it must be declared in the class)\n(includedClass: [%s])\n"
                                    + "(includedMethodString: [%s])";
                            String message = String.format(format, includedClass,
                                    includedMethodString);
                            validator.addError(message);
                        }
                    } else {
                        try {
                            // only search declared methods
                            Method found = includedClass.getDeclaredMethod(includedMethodName,
                                    paramTypes);
                            addIncludedMethod(found);
                        } catch (NoSuchMethodException ex) {
                            String format = "Included method cannot be found "
                                    + "(it must be declared in the class)\n(includedClass: [%s])\n"
                                    + "(includedMethodString: [%s])";
                            String message = String.format(format, includedClass,
                                    includedMethodString);
                            validator.addError(message);
                        }
                    }
                }
            }
        }
    }
}
