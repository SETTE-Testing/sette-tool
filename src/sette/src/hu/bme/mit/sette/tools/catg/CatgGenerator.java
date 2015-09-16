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
// TODO z revise this file
package hu.bme.mit.sette.tools.catg;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;

import hu.bme.mit.sette.common.descriptors.eclipse.EclipseClasspathEntry;
import hu.bme.mit.sette.common.descriptors.eclipse.EclipseClasspathEntry.Kind;
import hu.bme.mit.sette.common.descriptors.eclipse.EclipseProject;
import hu.bme.mit.sette.common.descriptors.java.JavaClassWithMain;
import hu.bme.mit.sette.common.exceptions.ConfigurationException;
import hu.bme.mit.sette.common.exceptions.SetteException;
import hu.bme.mit.sette.common.model.snippet.Snippet;
import hu.bme.mit.sette.common.model.snippet.SnippetContainer;
import hu.bme.mit.sette.common.model.snippet.SnippetProject;
import hu.bme.mit.sette.common.tasks.RunnerProjectGenerator;
import hu.bme.mit.sette.common.util.JavaFileUtils;

public class CatgGenerator extends RunnerProjectGenerator<CatgTool> {
    public CatgGenerator(SnippetProject snippetProject, File outputDirectory, CatgTool tool) {
        super(snippetProject, outputDirectory, tool);
    }

    @Override
    protected void afterPrepareRunnerProject(EclipseProject eclipseProject) {
        addGeneratedDirectoryToProject();

        eclipseProject.getClasspathDescriptor().addEntry(Kind.SOURCE, "src");
        eclipseProject.getClasspathDescriptor().addEntry(Kind.LIBRARY, "lib/asm-all-3.3.1.jar");
        eclipseProject.getClasspathDescriptor().addEntry(Kind.LIBRARY,
                "lib/choco-solver-2.1.4.jar");
        eclipseProject.getClasspathDescriptor().addEntry(Kind.LIBRARY, "lib/trove-3.0.3.jar");
    }

    @Override
    protected void afterWriteRunnerProject(EclipseProject eclipseProject)
            throws IOException, SetteException {
        createGeneratedFiles();
        copyTool(eclipseProject);
    }

    private void createGeneratedFiles() throws IOException {
        // generate main() for each snippet
        for (SnippetContainer container : getSnippetProject().getModel().getContainers()) {
            if (container.getRequiredJavaVersion()
                    .compareTo(getTool().getSupportedJavaVersion()) > 0) {
                // TODO enhance message
                System.err.println("Skipping container: " + container.getJavaClass().getName()
                        + " (required Java version: " + container.getRequiredJavaVersion() + ")");
                continue;
            }

            snippetLoop: for (Snippet snippet : container.getSnippets().values()) {
                Method method = snippet.getMethod();
                Class<?> javaClass = method.getDeclaringClass();
                Class<?>[] parameterTypes = method.getParameterTypes();

                // generate main()
                JavaClassWithMain main = new JavaClassWithMain();
                main.setPackageName(javaClass.getPackage().getName());
                main.setClassName(javaClass.getSimpleName() + '_' + method.getName());

                main.imports().add(javaClass.getName());
                main.imports().add("catg.CATG");

                String[] paramNames = new String[parameterTypes.length];
                List<String> createVariableLines = new ArrayList<>(parameterTypes.length);
                List<String> sysoutVariableLines = new ArrayList<>(parameterTypes.length);

                int i = 0;
                for (Class<?> parameterType : parameterTypes) {
                    String paramName = "param" + (i + 1);
                    String varType = CatgGenerator.getTypeString(parameterType);
                    String catgRead = CatgGenerator.createCatgRead(parameterType);

                    if (varType == null || catgRead == null) {
                        // TODO make better
                        System.err.println("Method has an unsupported parameter type: "
                                + parameterType.getName() + " (method: " + method.getName() + ")");
                        continue snippetLoop;
                    }

                    paramNames[i] = paramName;
                    // e.g.: int param1 = CATG.readInt(0);
                    createVariableLines
                            .add(String.format("%s %s = %s;", varType, paramName, catgRead));
                    // e.g.: System.out.println(" int param1 = " + param1);
                    sysoutVariableLines
                            .add(String.format("System.out.println(\"  %s %s = \" + %s);", varType,
                                    paramName, paramName));
                    i++;
                }

                main.codeLines().addAll(createVariableLines);

                main.codeLines().add("");

                main.codeLines().add(String.format("System.out.println(\"%s#%s\");",
                        javaClass.getSimpleName(), method.getName()));
                main.codeLines().addAll(sysoutVariableLines);

                String functionCall = String.format("%s.%s(%s)", javaClass.getSimpleName(),
                        method.getName(), StringUtils.join(paramNames, ", "));

                Class<?> returnType = method.getReturnType();

                if (Void.TYPE.equals(returnType) || Void.class.equals(returnType)) {
                    // void return type
                    main.codeLines().add(functionCall + ';');
                    main.codeLines().add("System.out.println(\"  result: void\");");
                } else {
                    // non-void return type
                    main.codeLines()
                            .add("System.out.println(\"  result: \" + " + functionCall + ");");
                }

                // save files
                String relativePath = JavaFileUtils.packageNameToFilename(main.getFullClassName());
                String relativePathMain = relativePath + '.' + JavaFileUtils.JAVA_SOURCE_EXTENSION;

                File targetMainFile = new File(getRunnerProjectSettings().getGeneratedDirectory(),
                        relativePathMain);
                FileUtils.forceMkdir(targetMainFile.getParentFile());
                FileUtils.writeLines(targetMainFile, main.generateJavaCodeLines());
            }
        }
    }

    private void copyTool(EclipseProject eclipseProject)
            throws IOException, ConfigurationException {
        FileUtils.copyDirectory(getTool().getToolDirectory(),
                getRunnerProjectSettings().getBaseDirectory());

        // edit build.xml
        // TODO make better

        File buildXml = new File(getRunnerProjectSettings().getBaseDirectory(), "build.xml");
        List<String> newLines = new ArrayList<>();

        InputStream fis = null;
        try {
            fis = new FileInputStream(buildXml);
            List<String> lines = IOUtils.readLines(fis);

            for (String line : lines) {
                if (line.contains("[SETTE]")) {
                    String indent = "";

                    for (int i = 0; i < line.length(); i++) {
                        char ch = line.charAt(i);

                        if (ch == ' ' || ch == '\t') {
                            indent += ch;
                        } else {
                            break;
                        }
                    }

                    line = line.trim();
                    if (line.equals("<!-- [SETTE][Libraries] -->")) {
                        for (EclipseClasspathEntry entry : eclipseProject.getClasspathDescriptor()
                                .classpathEntries()) {
                            if (entry.getKind().equals(Kind.LIBRARY)) {
                                newLines.add(String.format("%s<pathelement location=\"%s\"/>",
                                        indent, entry.getPath()));
                            }
                        }
                    } else if (line.equals("<!-- [SETTE][Sources] -->")) {
                        for (EclipseClasspathEntry entry : eclipseProject.getClasspathDescriptor()
                                .classpathEntries()) {
                            if (entry.getKind().equals(Kind.SOURCE)) {
                                newLines.add(String.format("%s<src path=\"%s\"/>", indent,
                                        entry.getPath()));
                            }
                        }
                    } else {
                        throw new ConfigurationException(
                                "Invalid SETTE command (XML comment) in CATG build.xml: " + line);
                    }
                } else {
                    newLines.add(line);
                }
            }
        } finally {
            IOUtils.closeQuietly(fis);
        }

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(buildXml);
            IOUtils.writeLines(newLines, null, fos);
        } finally {
            IOUtils.closeQuietly(fos);
        }
    }

    private static String getTypeString(Class<?> javaClass) {
        if (javaClass.isPrimitive()) {
            javaClass = ClassUtils.primitiveToWrapper(javaClass);
        }

        if (javaClass.equals(Byte.class)) {
            return "byte";
        } else if (javaClass.equals(Short.class)) {
            return "short";
        } else if (javaClass.equals(Integer.class)) {
            return "int";
        } else if (javaClass.equals(Long.class)) {
            return "long";
        } else if (javaClass.equals(Boolean.class)) {
            return "boolean";
        } else if (javaClass.equals(Character.class)) {
            return "char";
        } else if (javaClass.equals(String.class)) {
            return "String";
        } else {
            return null;
        }
    }

    private static String createCatgRead(Class<?> javaClass) {
        if (javaClass.isPrimitive()) {
            javaClass = ClassUtils.primitiveToWrapper(javaClass);
        }

        if (javaClass.equals(Byte.class)) {
            return "CATG.readByte((byte) 1)";
        } else if (javaClass.equals(Short.class)) {
            return "CATG.readShort((short) 1)";
        } else if (javaClass.equals(Integer.class)) {
            return "CATG.readInt(1)";
        } else if (javaClass.equals(Long.class)) {
            return "CATG.readLong(1L)";
        } else if (javaClass.equals(Boolean.class)) {
            return "CATG.readBool(false)";
        } else if (javaClass.equals(Character.class)) {
            return "CATG.readChar(' ')";
        } else if (javaClass.equals(String.class)) {
            return "CATG.readString(\"\")";
        } else {
            return null;
        }
    }
}
