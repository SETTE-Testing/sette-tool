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
package hu.bme.mit.sette.core.util;

import org.apache.commons.lang3.text.translate.JavaUnicodeEscaper;

import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

/**
 * Escapes special (non-ASCII) charaters int char and string literals using Unicode escape sequence,
 * e.g., "Aoő" -> "Ao\u0151". After this transformation, javac and ant will not fail because of
 * special characters (e.g., \u0016). Usage:
 * 
 * <pre>
 * <code>
 * CompilationUnit cu = JavaParser.parse(...);
 * cu.accept(new EscapeSpecialCharactersVisitor(), null);
 * </code>
 * </pre>
 */
public class EscapeSpecialCharactersVisitor extends VoidVisitorAdapter<Void> {
    private static final JavaUnicodeEscaper ESCAPER = JavaUnicodeEscaper.outsideOf(32, 0x7f);

    @Override
    public void visit(CharLiteralExpr n, Void arg) {
        n.setValue(escape(n.getValue()));
    }

    @Override
    public void visit(StringLiteralExpr n, Void arg) {
        n.setValue(escape(n.getValue()));
    }

    public static String escape(String string) {
        return ESCAPER.translate(string);
    }
}
