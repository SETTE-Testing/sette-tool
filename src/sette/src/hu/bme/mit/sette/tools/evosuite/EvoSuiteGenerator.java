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
package hu.bme.mit.sette.tools.evosuite;

import java.io.File;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Set;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;

import hu.bme.mit.sette.core.SetteException;
import hu.bme.mit.sette.core.descriptors.eclipse.EclipseProject;
import hu.bme.mit.sette.core.model.snippet.Snippet;
import hu.bme.mit.sette.core.model.snippet.SnippetContainer;
import hu.bme.mit.sette.core.model.snippet.SnippetProject;
import hu.bme.mit.sette.core.tasks.RunnerProjectGeneratorBase;
import hu.bme.mit.sette.core.util.EscapeSpecialCharactersVisitor;
import hu.bme.mit.sette.core.util.io.PathUtils;

public class EvoSuiteGenerator extends RunnerProjectGeneratorBase<EvoSuiteTool> {
    public EvoSuiteGenerator(SnippetProject snippetProject, Path outputDir, EvoSuiteTool tool,
            String runnerProjectTag) {
        super(snippetProject, outputDir, tool, runnerProjectTag);
    }

    @Override
    protected void afterWriteRunnerProject(EclipseProject eclipseProject) throws SetteException {
        createSpecialSnippetFiles();

        File buildXml = new File(getRunnerProjectSettings().getBaseDir(), "build.xml");
        PathUtils.copy(tool.getDefaultBuildXml(), buildXml.toPath());
    }

    private void createSpecialSnippetFiles() {
        // generate snippet classes containing exactly one one-method
        // generation is based on the runner project snippet source files (which does not have
        // annotations, only Java code)
        // NOTE this iteration is repeated at a lot of places, maybe visitor?
        for (SnippetContainer container : getSnippetProject().getSnippetContainers()) {
            String javaPackageName = container.getJavaClass().getPackage().getName();
            String javaClassName = container.getJavaClass().getSimpleName();
            Set<String> snippetMethodNames = container.getSnippets().keySet();

            for (Snippet snippet : container.getSnippets().values()) {
                String methodName = snippet.getMethod().getName();
                String newJavaClassName = javaClassName + '_' + methodName;

                File originalSourceFile = new File(
                        getRunnerProjectSettings().getSnippetSourceDirectory(),
                        javaPackageName.replace('.', '/') + '/' + javaClassName + ".java");
                if (!originalSourceFile.exists()) {
                    throw new RuntimeException(
                            "Original source file is missing: " + originalSourceFile);
                }
                File newSourceFile = new File(originalSourceFile.getParentFile(),
                        newJavaClassName + ".java");

                try {
                    log.debug("Parsing with JavaParser: {}", originalSourceFile);
                    CompilationUnit originalCu = JavaParser.parse(originalSourceFile);
                    log.debug("Parsed with JavaParser: {}", originalSourceFile);
                    CompilationUnit newCu = (CompilationUnit) originalCu.clone();

                    // NOTE this does not work for private methods, so keep all non-snippet method!
                    // delete this code if reviewed and not needed
                    //
                    // // add static import to the original class
                    // // e.g.: import static my.snippet.proj.SnippetContainer.*;
                    // // so other methods will be used from this class
                    // if (snippetCu.getImports() == null) {
                    // snippetCu.setImports(new ArrayList<>());
                    // }
                    //
                    // snippetCu.getImports().add(new ImportDeclaration(
                    // new NameExpr(javaPackageName + '.' + javaClassName), true, true));

                    // remove all methods which are not the snippet method
                    TypeDeclaration newCuType = newCu.getTypes().get(0);
                    newCuType.setName(newJavaClassName);
                    for (Iterator<BodyDeclaration> iterator = newCuType.getMembers()
                            .iterator(); iterator.hasNext();) {
                        BodyDeclaration bodyDecl = iterator.next();

                        if (bodyDecl instanceof ConstructorDeclaration) {
                            // keep constructor, but change name
                            ((ConstructorDeclaration) bodyDecl).setName(newJavaClassName);
                        } else if (bodyDecl instanceof MethodDeclaration) {
                            MethodDeclaration methodDecl = (MethodDeclaration) bodyDecl;
                            if (!methodDecl.getName().equals(methodName)
                                    && snippetMethodNames.contains(methodDecl.getName())) {
                                // another snippet method, remove
                                iterator.remove();
                            }
                        } else if (bodyDecl instanceof FieldDeclaration) {
                            // keep fields
                        } else if (bodyDecl instanceof ClassOrInterfaceDeclaration) {
                            // keep inner classes and interfaces
                        } else if (bodyDecl instanceof InitializerDeclaration) {
                            // keep inner classes and interfaces
                        } else {
                            throw new RuntimeException(
                                    "Unhandled case " + bodyDecl.getClass().getName() + " file: "
                                            + originalSourceFile);
                        }
                    }

                    // save new source code

                    // NOTE JavaParser problem with escape chars???
                    // newCu.accept(new VoidVisitorAdapter<Void>() {
                    // @Override
                    // public void visit(CharLiteralExpr n, Void arg) {
                    // System.err.println(n);
                    // }
                    //
                    // @Override
                    // public void visit(StringLiteralExpr n, Void arg) {
                    // System.err.println(n);
                    // }
                    // }, null);

                    // without comment might be buggy???
                    newCu.accept(new EscapeSpecialCharactersVisitor(), null);
                    PathUtils.write(newSourceFile.toPath(), newCu.toString().getBytes());
                } catch (Exception ex) {
                    throw new RuntimeException("SETTE ERROR", ex);
                }
            }
        }
    }

}
