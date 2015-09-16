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
package hu.bme.mit.sette.tools.spf;

import hu.bme.mit.sette.common.descriptors.eclipse.EclipseProject;
import hu.bme.mit.sette.common.descriptors.java.JavaClassWithMain;
import hu.bme.mit.sette.common.exceptions.SetteException;
import hu.bme.mit.sette.common.model.snippet.Snippet;
import hu.bme.mit.sette.common.model.snippet.SnippetContainer;
import hu.bme.mit.sette.common.model.snippet.SnippetProject;
import hu.bme.mit.sette.common.tasks.RunnerProjectGenerator;
import hu.bme.mit.sette.common.util.JavaFileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

public class SpfGenerator extends RunnerProjectGenerator<SpfTool> {
    public SpfGenerator(SnippetProject snippetProject, File outputDirectory, SpfTool tool) {
        super(snippetProject, outputDirectory, tool);
    }

    @Override
    protected void afterPrepareRunnerProject(EclipseProject eclipseProject) {
        addGeneratedDirectoryToProject();
    }

    @Override
    protected void afterWriteRunnerProject(EclipseProject eclipseProject)
            throws IOException, SetteException {
        createGeneratedFiles();

        File buildXml = new File(getRunnerProjectSettings().getBaseDirectory(), "build.xml");
        FileUtils.copyFile(getTool().getDefaultBuildXml(), buildXml);
    }

    private void createGeneratedFiles() throws IOException {
        // generate main() for each snippet
        for (SnippetContainer container : getSnippetProject().getModel().getContainers()) {
            // skip container with higher java version than supported
            if (container.getRequiredJavaVersion()
                    .compareTo(getTool().getSupportedJavaVersion()) > 0) {
                // TODO error handling
                System.err.println("Skipping container: " + container.getJavaClass().getName()
                        + " (required Java version: " + container.getRequiredJavaVersion() + ")");
                continue;
            }

            for (Snippet snippet : container.getSnippets().values()) {
                Method method = snippet.getMethod();
                Class<?> javaClass = method.getDeclaringClass();
                Class<?>[] parameterTypes = method.getParameterTypes();

                // create .jpf descriptor
                JPFConfig jpfConfig = new JPFConfig();
                jpfConfig.target = javaClass.getName() + '_' + method.getName();

                String symbMethod = javaClass.getName() + '.' + method.getName() + '('
                        + StringUtils.repeat("sym", "#", parameterTypes.length) + ')';

                jpfConfig.symbolicMethod.add(symbMethod);

                jpfConfig.classpath = "build" + SystemUtils.FILE_SEPARATOR;

                for (File libraryFile : getSnippetProject().getFiles().getLibraryFiles()) {
                    jpfConfig.classpath += ','
                            + getSnippetProjectSettings().getLibraryDirectoryPath()
                            + SystemUtils.FILE_SEPARATOR + libraryFile.getName();
                }

                jpfConfig.listener = JPFConfig.SYMBOLIC_LISTENER;
                jpfConfig.symbolicDebug = JPFConfig.ON;

                jpfConfig.searchMultipleErrors = JPFConfig.TRUE;
                jpfConfig.decisionProcedure = JPFConfig.DP_CORAL;

                // generate main()
                JavaClassWithMain main = new JavaClassWithMain();
                main.setPackageName(javaClass.getPackage().getName());
                main.setClassName(javaClass.getSimpleName() + '_' + method.getName());

                main.imports().add(javaClass.getName());

                String[] parameterLiterals = new String[parameterTypes.length];

                int i = 0;
                for (Class<?> parameterType : parameterTypes) {
                    parameterLiterals[i] = SpfGenerator.getParameterLiteral(parameterType);
                    i++;
                }

                main.codeLines().add(javaClass.getSimpleName() + '.' + method.getName() + '('
                        + StringUtils.join(parameterLiterals, ", ") + ");");

                // save files
                String relativePath = JavaFileUtils.packageNameToFilename(main.getFullClassName());
                String relativePathJPF = relativePath + '.' + JPFConfig.JPF_CONFIG_EXTENSION;
                String relativePathMain = relativePath + '.' + JavaFileUtils.JAVA_SOURCE_EXTENSION;

                File targetJPFFile = new File(getRunnerProjectSettings().getGeneratedDirectory(),
                        relativePathJPF);
                FileUtils.forceMkdir(targetJPFFile.getParentFile());
                FileUtils.write(targetJPFFile, jpfConfig.generate().toString());

                File targetMainFile = new File(getRunnerProjectSettings().getGeneratedDirectory(),
                        relativePathMain);
                FileUtils.forceMkdir(targetMainFile.getParentFile());
                FileUtils.writeLines(targetMainFile, main.generateJavaCodeLines());
            }
        }
    }

    // TODO enhance visibility or refactor to other place
    /* private */static String getParameterLiteral(Class<?> javaClass) {
        if (javaClass.isPrimitive()) {
            javaClass = ClassUtils.primitiveToWrapper(javaClass);
        }

        if (javaClass.equals(Byte.class)) {
            return "(byte) 1";
        } else if (javaClass.equals(Short.class)) {
            return "(short) 1";
        } else if (javaClass.equals(Integer.class)) {
            return "1";
        } else if (javaClass.equals(Long.class)) {
            return "1L";
        } else if (javaClass.equals(Float.class)) {
            return "1.0f";
        } else if (javaClass.equals(Double.class)) {
            return "1.0";
        } else if (javaClass.equals(Boolean.class)) {
            return "false";
        } else if (javaClass.equals(Character.class)) {
            return "' '";
        } else {
            return "null";
        }
    }
}
