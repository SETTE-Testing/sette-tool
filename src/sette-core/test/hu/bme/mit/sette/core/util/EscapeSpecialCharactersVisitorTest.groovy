package hu.bme.mit.sette.core.util

import static hu.bme.mit.sette.core.util.EscapeSpecialCharactersVisitor.escape
import groovy.transform.TypeChecked

import org.junit.Test

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.CompilationUnit

@TypeChecked
class EscapeSpecialCharactersVisitorTest {

    @Test
    void testNothingToEscape() {
        String str = 'asdf ghjkl'
        assert escape(str) == str
    }

    @Test
    void testDoesNotEscapeSlashes() {
        String str = 'a\\b\"c\'de\\\\'
        assert escape(str) == str
    }

    @Test
    void testEscapesTabAndNewLine() {
        String str = 'a\tb\rc\nd'
        assert escape(str) == 'a\\u0009b\\u000Dc\\u000Ad'
    }

    @Test
    void testEscapesSpecialChars() {
        String str = 'AáőűZ\u0016'
        assert escape(str) == 'A\\u00E1\\u0151\\u0171Z\\u0016'
    }

    @Test
    void testJavaParserIntegration() {
        // description might be tricky
        // in source and expected, escape is needed because they are strings
        String source = '''
class MyClass {
    char ch1 = 'ő';
    char ch2 = '\\\\';
    String str1 = "a\\\\b\\"c\\'de\\\\\\\\";
    String str2 = "a\\tb\\rc\\nd";
    String str3 = "AáőűZ\\u0016";
}
'''

        List<String> expectedLines = '''
class MyClass {
    char ch1 = '\\u0151';
    char ch2 = '\\\\';
    String str1 = "a\\\\b\\"c\\'de\\\\\\\\";
    String str2 = "a\\tb\\rc\\nd";
    String str3 = "A\\u00E1\\u0151\\u0171Z\\u0016";
}
'''.tokenize('\n')*.trim() // source file EOL can cause troubles (LF vs CRLF)


        CompilationUnit cu = JavaParser.parse(new ByteArrayInputStream(source.bytes))
        cu.accept(new EscapeSpecialCharactersVisitor(), null)

        // also skip empty lines
        List<String> outputLines = cu.toString() .tokenize('\n')*.trim().findAll { !it.isEmpty() }

        assert expectedLines == outputLines
    }
}
