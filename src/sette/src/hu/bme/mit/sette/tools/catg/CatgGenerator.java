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
package hu.bme.mit.sette.tools.catg;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.primitives.Primitives;

import hu.bme.mit.sette.core.SetteException;
import hu.bme.mit.sette.core.configuration.SetteConfigurationException;
import hu.bme.mit.sette.core.descriptors.eclipse.EclipseClasspathEntry;
import hu.bme.mit.sette.core.descriptors.eclipse.EclipseClasspathEntryKind;
import hu.bme.mit.sette.core.descriptors.eclipse.EclipseProject;
import hu.bme.mit.sette.core.descriptors.java.JavaFileWithMainBuilder;
import hu.bme.mit.sette.core.model.snippet.Snippet;
import hu.bme.mit.sette.core.model.snippet.SnippetContainer;
import hu.bme.mit.sette.core.model.snippet.SnippetProject;
import hu.bme.mit.sette.core.tasks.RunnerProjectGenerator;
import hu.bme.mit.sette.core.util.io.PathUtils;

public class CatgGenerator extends RunnerProjectGenerator<CatgTool> {
    public CatgGenerator(SnippetProject snippetProject, Path outputDir, CatgTool tool,
            String runnerProjectTag) {
        super(snippetProject, outputDir, tool, runnerProjectTag);
    }

    @Override
    protected void afterPrepareRunnerProject(EclipseProject eclipseProject) {
        addGeneratedDirToProject();

        eclipseProject.getClasspathDescriptor().addEntry(EclipseClasspathEntryKind.SOURCE, "src");
        eclipseProject.getClasspathDescriptor().addEntry(EclipseClasspathEntryKind.LIBRARY,
                "lib/asm-all-3.3.1.jar");
        eclipseProject.getClasspathDescriptor().addEntry(EclipseClasspathEntryKind.LIBRARY,
                "lib/choco-solver-2.1.4.jar");
        eclipseProject.getClasspathDescriptor().addEntry(EclipseClasspathEntryKind.LIBRARY,
                "lib/trove-3.0.3.jar");
    }

    @Override
    protected void afterWriteRunnerProject(EclipseProject eclipseProject)
            throws IOException, SetteException {
        createGeneratedFiles();
        copyTool(eclipseProject);
    }

    private void createGeneratedFiles() throws IOException {
        // generate main() for each snippet
        for (SnippetContainer container : getSnippetProject().getSnippetContainers()) {
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
                JavaFileWithMainBuilder main = new JavaFileWithMainBuilder();
                main.packageName(javaClass.getPackage().getName());
                main.className(javaClass.getSimpleName() + '_' + method.getName());

                main.imports().add(javaClass.getName());

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
                        // System.err.println("Method has an unsupported parameter type: "
                        // + parameterType.getName() + " (method: " + method.getName() + ")");
                        continue snippetLoop;
                    }

                    paramNames[i] = paramName;
                    // e.g.: int param1 = catg.CATG.readInt(0);
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
                String relativePath = main.getFullClassName().replace('.', '/');
                String relativePathMain = relativePath.replace('.', '/') + ".java";

                File targetMainFile = new File(getRunnerProjectSettings().getGeneratedDirectory(),
                        relativePathMain);
                PathUtils.createDir(targetMainFile.getParentFile().toPath());
                PathUtils.write(targetMainFile.toPath(), main.build());
            }
        }
    }

    private void copyTool(EclipseProject eclipseProject)
            throws IOException, SetteConfigurationException {
        PathUtils.copy(getTool().getToolDir().resolve("tool"),
                getRunnerProjectSettings().getBaseDir().toPath());

        // edit build.xml
        // TODO make better

        File buildXml = new File(getRunnerProjectSettings().getBaseDir(), "build.xml");

        List<String> newLines = new ArrayList<>();

        List<String> lines = PathUtils.readAllLines(buildXml.toPath());

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
                            .getClasspathEntries()) {
                        if (entry.getKind().equals(EclipseClasspathEntryKind.LIBRARY)) {
                            newLines.add(String.format("%s<pathelement location=\"%s\"/>", indent,
                                    entry.getPath()));
                        }
                    }
                } else if (line.equals("<!-- [SETTE][Sources] -->")) {
                    for (EclipseClasspathEntry entry : eclipseProject.getClasspathDescriptor()
                            .getClasspathEntries()) {
                        if (entry.getKind().equals(EclipseClasspathEntryKind.SOURCE)) {
                            newLines.add(
                                    String.format("%s<src path=\"%s\"/>", indent, entry.getPath()));
                        }
                    }
                } else {
                    throw new SetteConfigurationException(
                            "Invalid SETTE command (XML comment) in CATG build.xml: " + line);
                }
            } else {
                newLines.add(line);
            }
        }

        PathUtils.write(buildXml.toPath(), newLines);
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
        javaClass = Primitives.wrap(javaClass);

        if (javaClass.equals(Byte.class)) {
            return "catg.CATG.readByte((byte) 1)";
        } else if (javaClass.equals(Short.class)) {
            return "catg.CATG.readShort((short) 1)";
        } else if (javaClass.equals(Integer.class)) {
            return "catg.CATG.readInt(1)";
        } else if (javaClass.equals(Long.class)) {
            return "catg.CATG.readLong(1L)";
        } else if (javaClass.equals(Boolean.class)) {
            return "catg.CATG.readBool(false)";
        } else if (javaClass.equals(Character.class)) {
            return "catg.CATG.readChar(' ')";
        } else if (javaClass.equals(String.class)) {
            return "catg.CATG.readString(\"\")";
        } else {
            return null;
        }
    }
}
