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
package hu.bme.mit.sette.common.descriptors.java;

import hu.bme.mit.sette.common.util.JavaFileUtils;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

/**
 * Represents a Java class with a main() method. Please note that this class
 * does not perform any Java compiler check. The generated code looks like as
 * this example:
 *
 * <pre>
 * <code>
 * package packageName;
 *
 * import [import1];
 * import [import2];
 * import [etc.];
 *
 * public final class ClassName {
 *     public static void main(String args[]) throws Exception {
 *         [code line 1]
 *         [code line 2]
 *         [etc.]
 *     }
 * }
 * </code>
 * </pre>
 */
public final class JavaClassWithMain {
    /** The name of the package. */
    private String packageName = null;
    /** The name of the class. */
    private String className = "";
    /** The list of imports. */
    private final List<String> imports;
    /** The list containing the code lines for the main() method. */
    private final List<String> codeLines;

    /** Creates an instance of the object. */
    public JavaClassWithMain() {
        imports = new ArrayList<>();
        codeLines = new ArrayList<>();
    }

    /**
     * Returns the name of the package.
     *
     * @return The name of the package.
     */
    public String getPackageName() {
        return packageName;
    }

    /**
     * Sets the name of the package.
     *
     * @param pPackageName
     *            The name of the package.
     */
    public void setPackageName(final String pPackageName) {
        packageName = StringUtils.trimToNull(pPackageName);
    }

    /**
     * Returns the name of the class.
     *
     * @return The name of the class.
     */
    public String getClassName() {
        return className;
    }

    /**
     * Sets the name of the class.
     *
     * @param pClassName
     *            The name of the class.
     */
    public void setClassName(final String pClassName) {
        Validate.notBlank(pClassName,
                "The class name must not be blank");
        className = pClassName.trim();
    }

    /**
     * Returns the full name of the class.
     *
     * @return The full name of the class.
     */
    public String getFullClassName() {
        if (packageName == null) {
            return className;
        } else {
            return packageName + JavaFileUtils.PACKAGE_SEPARATOR
                    + className;
        }
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
     * Generates the Java code.
     *
     * @return A {@link StringBuilder} containing the build Java code.
     */
    public StringBuilder generateJavaCode() {
        StringBuilder sb = new StringBuilder();

        if (packageName != null) {
            sb.append("package ").append(packageName).append(";\n");
            sb.append("\n");
        }

        for (String imp : imports) {
            sb.append("import ").append(imp).append(";\n");
        }
        sb.append("\n");

        sb.append("public final class ").append(className)
        .append(" {\n");
        sb.append("    public static void main(String[] args) "
                + "throws Exception {\n");

        for (String line : codeLines) {
            sb.append("        ").append(line).append("\n");
        }

        sb.append("    }\n");
        sb.append("}\n");

        return sb;
    }
}
