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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.google.common.collect.ImmutableSortedMap;

import hu.bme.mit.sette.common.annotations.SetteNotSnippet;
import hu.bme.mit.sette.common.annotations.SetteSnippetContainer;
import hu.bme.mit.sette.common.snippets.JavaVersion;
import hu.bme.mit.sette.core.util.reflection.ClassComparator;
import hu.bme.mit.sette.core.util.reflection.SetteAnnotationUtils;
import hu.bme.mit.sette.core.validator.ClassExecutableValidator;
import hu.bme.mit.sette.core.validator.ClassFieldValidator;
import hu.bme.mit.sette.core.validator.ClassValidator;
import hu.bme.mit.sette.core.validator.ValidationException;
import hu.bme.mit.sette.core.validator.Validator;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.val;

/**
 * Represents a snippet container (which is a Java class).
 */
@Data
public final class SnippetContainer implements Comparable<SnippetContainer> {
    /** The snippet project to which the instance belongs to. */
    @Getter
    private final SnippetProject snippetProject;

    /** The name of the snippet container, which is the simple name of the Java {@link Class}. */
    @Getter
    private final String name;

    /** The Java {@link Class} of the snippet container. */
    @Getter
    private final Class<?> javaClass;

    /** The category . */
    @Getter
    private final String category;

    /** The goal. */
    @Getter
    private final String goal;

    /** The required Java version. */
    @Getter
    private final JavaVersion requiredJavaVersion;

    /** The code snippets (immutable) mapped by their names. */
    @Getter
    private final ImmutableSortedMap<String, Snippet> snippets;

    /** The corresponding snippet input factory container. */
    @Getter
    private final SnippetInputFactoryContainer inputFactoryContainer;

    /**
     * Instantiates a new snippet container.
     *
     * @param snippetProject
     *            the snippet project to which the instance belongs to
     * @param javaClass
     *            the Java {@link Class}
     * @throws ValidationException
     *             if validation fails
     */
    SnippetContainer(@NonNull SnippetProject snippetProject, @NonNull Class<?> javaClass)
            throws ValidationException {
        this.snippetProject = snippetProject;
        this.javaClass = javaClass;
        this.name = javaClass.getSimpleName();

        // Start validation
        Validator<String> v = Validator.of("SnippetContainer: " + javaClass.getName());

        // check: "public final class", no superclass, interface, declared class, one constructor
        ClassValidator cv = new ClassValidator(javaClass);
        cv.isRegular().superclass(Object.class).interfaceCount(0).declaredConstructorCount(1);
        cv.withModifiers(Modifier.PUBLIC | Modifier.FINAL).withoutModifiers(Modifier.ABSTRACT);

        // check: only @SetteSnippetContainer from SETTE
        val annots = SetteAnnotationUtils.getSetteAnnotations(javaClass);
        cv.addErrorIfFalse("The class must have exaclty 1 annotation", annots.size() == 1);

        // check: proper @SetteSnippetContainer annotation
        SetteSnippetContainer containerAnnot = annots.getInstance(SetteSnippetContainer.class);

        if (containerAnnot == null) {
            cv.addError("The class must have the annotation @SetteSnippetContainer");

            // TODO inner parser class like snippets, noew set is needed because they are final
            v.validate();
            throw new RuntimeException("WTF: this should never happen");
        } else {
            cv.addErrorIfTrue("The category in @SetteSnippetContainer must not be blank",
                    containerAnnot.category().trim().isEmpty());

            cv.addErrorIfTrue("The goal in @SetteSnippetContainer must not be blank",
                    containerAnnot.goal().trim().isEmpty());

            category = containerAnnot.category();
            goal = containerAnnot.goal();
            requiredJavaVersion = containerAnnot.requiredJavaVersion();

            cv.addErrorIfFalse("The class name should start with the category: " + category,
                    javaClass.getSimpleName().startsWith(category));

            if (cv.isValid()) {
                cv.addErrorIfFalse("The class name should contain at least one '_' character after "
                        + "the category (the part of the class name before the first '_' after the "
                        + "category will be the prefix of the ids of the snippets",
                        javaClass.getSimpleName().indexOf('_', category.length()) >= 0);
            }
        }

        v.addChild(cv);

        validateFields(v);
        validateConstructor(v);
        v.validate();

        Set<Method> snippetMethods = collectSnippetMethods(v);
        Map<String, Snippet> tmpSnippets = new HashMap<>();

        for (Method method : snippetMethods) {
            try {
                Snippet snippet = new Snippet(this, method);
                tmpSnippets.put(snippet.getName(), snippet);
            } catch (ValidationException ex) {
                cv.addChild(ex.getValidator());
            }

        }

        v.validate();
        snippets = ImmutableSortedMap.copyOf(tmpSnippets);

        // input factory container
        SnippetInputFactoryContainer inputFactCont = null;
        if (!containerAnnot.inputFactoryContainer().equals(Void.class)) {
            // input factory container is present
            try {
                // create input factory container
                inputFactCont = new SnippetInputFactoryContainer(this,
                        containerAnnot.inputFactoryContainer());
            } catch (ValidationException ex) {
                cv.addChild(ex.getValidator());
            }
        }
        v.validate();

        // set input factory container
        inputFactoryContainer = inputFactCont;
    }

    private void validateFields(Validator<?> validator) {
        // check: only constant ("public static final") or synthetic (~compiler-generated) fields
        for (Field field : javaClass.getDeclaredFields()) {
            if (field.isSynthetic()) {
                continue;
            }

            ClassFieldValidator v = new ClassFieldValidator(field);
            v.withModifiers(Modifier.STATIC);
            validator.addChildIfInvalid(v);
        }
    }

    private void validateConstructor(Validator<?> validator) {
        Constructor<?>[] ctors = javaClass.getDeclaredConstructors();
        if (ctors.length != 1) {
            // error, but constructor count is validated with the class
            return;
        }

        Constructor<?> ctor = ctors[0];
        ClassExecutableValidator v = new ClassExecutableValidator(ctor);
        v.withModifiers(Modifier.PRIVATE).parameterCount(0);

        // check: constructor throws UnsupportedOperationException("Static class")
        Throwable exception = null;
        try {
            ctor.setAccessible(true);
            ctor.newInstance();
        } catch (Exception ex) {
            exception = ex.getCause();
        } finally {
            ctor.setAccessible(false);
        }

        if (exception == null || !exception.getClass().equals(UnsupportedOperationException.class)
                || !exception.getMessage().equals("Static class")) {
            v.addError("The constructor must throw an UnsupportedOperationException with "
                    + "the message \"Static class\"");
        }

        validator.addChildIfInvalid(v);
    }

    private Set<Method> collectSnippetMethods(Validator<?> validator) {
        // check: only "[public|private] static" or synthetic methods
        SortedMap<String, Method> snippetMethods = new TreeMap<>();
        SortedSet<String> duplicateMethods = new TreeSet<>();

        for (Method method : javaClass.getDeclaredMethods()) {
            if (method.isSynthetic()) {
                continue;
            }

            // check: public|private static, non-abstract, non-final, non-native
            ClassExecutableValidator v = new ClassExecutableValidator(method);

            int methodModifiers = method.getModifiers();
            if (!Modifier.isPublic(methodModifiers) && !Modifier.isPrivate(methodModifiers)) {
                v.addError("The method must be public or private");
            }

            v.withModifiers(Modifier.STATIC);
            v.withoutModifiers(Modifier.ABSTRACT | Modifier.FINAL | Modifier.NATIVE);

            // check: public might be snippet or not, private is not snippet
            // non-snippet methods must not have any modifiers
            val methodAnns = SetteAnnotationUtils.getSetteAnnotations(method);

            if (Modifier.isPublic(methodModifiers)) {
                if (methodAnns.get(SetteNotSnippet.class) == null) {
                    // should be snippet (validated by the Snippet class)
                    String methodName = method.getName();
                    if (snippetMethods.keySet().contains(methodName)) {
                        duplicateMethods.add(methodName);
                    } else {
                        snippetMethods.put(methodName, method);
                    }
                } else {
                    // not snippet
                    if (methodAnns.size() != 1) {
                        v.addError("The method must not have any other SETTE annotations "
                                + "if it is not a snippet.");
                    }
                }
            } else {
                if (methodAnns.size() != 0) {
                    v.addError("The method must not have any SETTE annotations (private method)");
                }
            }

            validator.addChildIfInvalid(v);
        }

        // collect duplicates
        if (!duplicateMethods.isEmpty()) {
            Validator<Class<?>> v = Validator.of(javaClass);

            for (String duplicateMethodName : duplicateMethods) {
                v.addError("Non-unique snippet method name: " + duplicateMethodName);
                snippetMethods.remove(duplicateMethodName);
            }

            validator.addChildIfInvalid(v);
        }

        return new HashSet<>(snippetMethods.values());
    }

    @Override
    public int compareTo(@NonNull SnippetContainer o) {
        return ClassComparator.INSTANCE.compare(javaClass, o.javaClass);
    }

    @Override
    public String toString() {
        return "SnippetContainer [snippetProjectName=" + snippetProject.getName() + ", name=" + name
                + ", javaClass=" + javaClass + ", category=" + category + ", goal=" + goal
                + ", requiredJavaVersion=" + requiredJavaVersion + "]";
        // TODO: input factory container
    }
}
