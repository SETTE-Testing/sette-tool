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
package hu.bme.mit.sette.common.descriptors.java;

import hu.bme.mit.sette.common.util.JavaFileUtils;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

/**
 * Represents a Java class with a main() method. Please note that this class generates a lines for a
 * Java file based on the given data does not perform any Java compiler check. This class is used
 * for to generated test drivers for test input generators. Example for the generated code:
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
     * @param packageName
     *            The name of the package.
     */
    public void setPackageName(String packageName) {
        this.packageName = StringUtils.trimToNull(packageName);
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
     * @param className
     *            The name of the class.
     */
    public void setClassName(String className) {
        Validate.notBlank(className, "The class name must not be blank");
        this.className = className.trim();
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
            return packageName + JavaFileUtils.PACKAGE_SEPARATOR + className;
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
     * Generates the lines of the Java code.
     *
     * @return A {@link List} containing the lines of the Java code.
     */
    public List<String> generateJavaCodeLines() {
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
