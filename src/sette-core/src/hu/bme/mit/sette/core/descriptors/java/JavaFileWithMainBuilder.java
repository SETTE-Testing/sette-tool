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
package hu.bme.mit.sette.core.descriptors.java;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a Java source file with a main() method. Please note that this class generates a lines for
 * a Java file based on the given data does not perform any Java compiler check. This class is used
 * to generate test drivers for test input generators. Example for the generated code:
 *
 * <pre>
 * <code>
 * package packageName;
 *
 * import java.util.ArrayList;
 * import java.util.List;
 *
 * public final class ClassName {
 *     public static void main(String args[]) throws Exception {
 *         List<String> list = new ArrayList<>();
 *         list.add("my string");
 *         // ...
 *     }
 * }
 * </code>
 * </pre>
 */
public final class JavaFileWithMainBuilder {
    /** The name of the package. */
    private String packageName;

    /** The name of the class. */
    private String className;

    /** The list of imports. */
    private final List<String> imports;

    /** The list containing the code lines for the main() method. */
    private final List<String> codeLines;

    /**
     * Creates a new builder instance.
     */
    public JavaFileWithMainBuilder() {
        imports = new ArrayList<>();
        codeLines = new ArrayList<>();
    }

    /**
     * Sets the name of the package.
     *
     * @param packageName
     *            The name of the package.
     * @return The builder instance.
     */
    public JavaFileWithMainBuilder packageName(String packageName) {
        this.packageName = packageName;
        return this;
    }

    /**
     * Sets the name of the class.
     *
     * @param className
     *            The name of the class.
     * @return The builder instance.
     */
    public void className(String className) {
        this.className = className;
    }

    /**
     * Returns the full name of the class.
     *
     * @return The full name of the class.
     */
    public String getFullClassName() {
        return packageName == null ? className : packageName + '.' + className;
    }

    /**
     * Returns the list of imports.
     *
     * @return The list of imports.
     */
    public List<String> imports() {
        return imports;
    }

    /**
     * Returns the list containing the code lines for the main() method.
     *
     * @return The list containing the code lines for the main() method.
     */
    public List<String> codeLines() {
        return codeLines;
    }

    /**
     * Generates the lines of the Java code.
     *
     * @return A {@link List} containing the lines of the Java code.
     */
    public List<String> build() {
        List<String> generated = new ArrayList<>();

        if (packageName != null) {
            generated.add(String.format("package %s;", packageName));
            generated.add("");
        }

        for (String imp : imports) {
            generated.add(String.format("import %s;", imp));
        }
        generated.add("");

        generated.add(String.format("public final class %s {", className));
        generated.add("    public static void main(String[] args) throws Exception {");

        for (String line : codeLines) {
            generated.add(String.format("        %s", line));
        }

        generated.add("    }");
        generated.add("}");

        return generated;
    }
}
