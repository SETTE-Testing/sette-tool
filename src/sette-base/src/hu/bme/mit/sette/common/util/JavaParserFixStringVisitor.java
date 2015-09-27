package hu.bme.mit.sette.common.util;

import org.apache.commons.lang3.StringEscapeUtils;

import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

// FIXME fixes string escapes in test cases (EvoSuite)
// usage: compilationUnit.accept(new JavaParserFixStringVisitor(), null)
public class JavaParserFixStringVisitor extends VoidVisitorAdapter<Void> {
    @Override
    public void visit(CharLiteralExpr n, Void arg) {
        handle(n);
    }

    @Override
    public void visit(StringLiteralExpr n, Void arg) {
        handle(n);
    }

    // FIXME
    // private static final CharSequenceTranslator MAPPING = new LookupTranslator(
    // new String[][] { { "\\\\", "\\" },
    // // { "\\\"", "\"" },
    // // { "\\'", "'" },
    // // { "\\", "" }
    // });

    private static void handle(StringLiteralExpr n) {
        String v = StringEscapeUtils.escapeJava(n.getValue());
        // v = MAPPING.translate(v); // do not revert back \\u0342 etc...
        n.setValue(v);
    }
}
