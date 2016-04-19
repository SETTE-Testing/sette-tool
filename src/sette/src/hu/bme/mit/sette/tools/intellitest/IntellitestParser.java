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
package hu.bme.mit.sette.tools.intellitest;

import static java.util.stream.Collectors.toList;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.google.common.base.Charsets;

import hu.bme.mit.sette.core.model.snippet.Snippet;
import hu.bme.mit.sette.core.model.snippet.SnippetContainer;
import hu.bme.mit.sette.core.model.snippet.SnippetProject;
import hu.bme.mit.sette.core.util.io.PathUtils;
import hu.bme.mit.sette.core.validator.PathValidator;
import lombok.extern.slf4j.Slf4j;

/**
 * FIXME this class parses intellitest test cases and generates Java test cases
 */
@Slf4j
public class IntellitestParser {
    private final SnippetProject snippetProject;
    private final Path sourceDir;
    private final Path targetDir;

    public IntellitestParser(SnippetProject snippetProject, Path sourceDir, Path targetDir) {
        this.snippetProject = snippetProject;
        this.sourceDir = sourceDir;
        this.targetDir = targetDir;
    }

    public void parse() {
        try {
            log.info("Parsing: {}", sourceDir.getFileName());

            // FIXME deletion disabled for testing (less log)
            // PathUtils.deleteIfExists(targetDir);
            PathUtils.createDir(targetDir);

            // copy src, lib, junit eclipse files etc
            {
                Path dir = targetDir.resolveSibling("java-sette-snippets");
                PathValidator.forDirectory(dir, true, true, true).validate();
                PathUtils.copy(dir, targetDir);

                // set project name
                Path projectFile = targetDir.resolve(targetDir.resolve(".project"));
                List<String> projectLines = PathUtils.readAllLines(projectFile)
                        .stream()
                        .map(line -> {
                            if (line.contains("<name>sette-snippets")) {
                                return "  <name>" + targetDir.getFileName().toString() + "</name>";
                            } else {
                                return line;
                            }
                        }).collect(toList());

                PathUtils.write(projectFile, projectLines);
            }

            // collect *.g.cs files
            Files.list(sourceDir).filter(p -> {
                return Files.isRegularFile(p) && p.getFileName().toString().endsWith(".g.cs");
            }).forEach(csFile -> {
                String[] parts = csFile.getFileName().toString().split("\\.");

                String snippetContainerName = parts[0].replaceFirst("Test$", "");
                String snippetName = parts[1].replaceFirst("Test$", "");

                SnippetContainer snippetContainer = snippetProject.getSnippetContainers()
                        .stream()
                        .filter(sc -> sc.getName().equals(snippetContainerName))
                        .findAny()
                        .orElse(null);

                if (snippetContainer == null) {
                    log.error("Unknown snippet container");
                    log.error("{} : {}", snippetContainerName, snippetName);
                    log.error(csFile.getFileName().toString());
                    throw new RuntimeException();
                }

                Snippet snippet = snippetContainer.getSnippets().get(snippetName);

                if (snippet == null) {
                    log.error("Unknown snippet");
                    log.error("{} : {}", snippetContainerName, snippetName);
                    log.error(csFile.getFileName().toString());
                    throw new RuntimeException();
                }

                try {
                    parseOne(csFile, snippet);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void parseOne(Path csFile, Snippet snippet) {
        // note the charset
        List<String> csLines = PathUtils.readAllLines(csFile, Charsets.UTF_16LE);
        // remove BOM if needed
        if (csLines.get(0).charAt(0) == 65279) {
            csLines.set(0, csLines.get(0).substring(1));
        }

        // remove unnecessary lines
        List<String> removePrefix = Arrays.asList("//", "using Microsoft", "using System",
                "namespace BME.MIT.SETTE");
        List<String> removeContains = Arrays.asList("[PexGeneratedBy(typeof(");

        csLines.removeIf(line -> line.trim().isEmpty());
        removePrefix.forEach(prefix -> {
            csLines.removeIf(line -> line.startsWith(prefix));
        });
        removeContains.forEach(content -> {
            csLines.removeIf(line -> line.contains(content));
        });

        List<List<String>> testMethods = new ArrayList<>();
        {
            List<String> testMethodLines = null;

            // split by test methods
            for (String line : csLines) {
                if (line.contains("[TestMethod]")) {
                    saveTestMethodLines(testMethods, testMethodLines);
                    testMethodLines = new ArrayList<>();
                } else if (testMethodLines != null) {
                    testMethodLines.add(line);
                }
            }

            // also save the last method
            if (testMethodLines != null) {
                // last two lines: }} (class and namespace)
                testMethodLines.remove(testMethodLines.size() - 1);
                testMethodLines.remove(testMethodLines.size() - 1);
                saveTestMethodLines(testMethods, testMethodLines);
            }
        }

        // create Java lines
        SnippetContainer snippetContainer = snippet.getContainer();
        Class<?> snippetContainerClass = snippetContainer.getJavaClass();
        String testPackageName = snippetContainerClass.getPackage().getName();
        String testClassName = snippetContainer.getName() + "_" + snippet.getName() + "_Test";

        List<String> javaLines = new ArrayList<>();
        javaLines.add("package " + testPackageName + ";");
        javaLines.add("");

        Arrays.asList("junit.framework.TestCase",
                snippetContainerClass.getName(),
                "hu.bme.mit.sette.snippets._1_basic.B6_exceptions.dependencies.*",
                "hu.bme.mit.sette.snippets._2_structures.dependencies.*",
                "hu.bme.mit.sette.snippets._4_generics.dependencies.*",
                "hu.bme.mit.sette.snippets._3_objects.dependencies.*",
                "hu.bme.mit.sette.snippets._5_library.dependencies.*",
                "hu.bme.mit.sette.snippets._6_others.dependencies.*",
                "java.util.*")
                .forEach(imp -> {
                    javaLines.add("import " + imp + ";");
                });

        javaLines.add("");

        javaLines.add("public final class " + testClassName + " extends TestCase {");
        javaLines.add("");

        for (List<String> testMethodLines : testMethods) {
            writeTestMethod(javaLines, testMethodLines, snippet);
        }

        javaLines.add("}");

        // save
        Path testDir = targetDir.resolve("test");
        Path packageDir = testDir.resolve(testPackageName.replace('.', '/'));
        PathUtils.createDir(packageDir);

        Path testFile = packageDir.resolve(testClassName + ".java");
        PathUtils.write(testFile, javaLines);

    }

    private static final String CS_ASSERT_PATTERN_STRING = "^"
            + "(?<indent>\\s+)"
            + "Assert\\.AreEqual<[A-Za-z0-9_]+>"
            + "\\("
            + "(?<params>.*)"
            + "\\)"
            + ";$";
    private static final Pattern CS_ASSERT_PATTERN = Pattern.compile(
            CS_ASSERT_PATTERN_STRING);

    private static void writeTestMethod(List<String> javaLines, List<String> testMethodLines,
            Snippet snippet) {
        SnippetContainer snippetContainer = snippet.getContainer();

        // skip ignored tests
        if (testMethodLines.stream().anyMatch(line -> line.contains("[Ignore]"))) {
            log.debug("Skipping test for : {} : {}", snippet.getId(), testMethodLines);
            return;
        }

        Optional<String> raisedException = testMethodLines.stream()
                .filter(line -> line.startsWith("[PexRaisedException(typeof(")
                        || line.startsWith("[ExpectedException(typeof("))
                .findAny();
        String expectedExceptionClassName = null;
        if (raisedException.isPresent()) {
            testMethodLines.removeIf(line -> line.startsWith("[PexRaisedException(typeof("));
            testMethodLines.removeIf(line -> line.startsWith("[ExpectedException(typeof("));

            String exceptionTypeStr = raisedException.get()
                    .replace("[PexRaisedException(typeof(", "")
                    .replace("[ExpectedException(typeof(", "")
                    .replace("))]", "");

            expectedExceptionClassName = resolveExceptionClass(exceptionTypeStr);
        }

        if (expectedExceptionClassName != null && snippet.getId().equals("B6b_tryCatchFinally")
                && expectedExceptionClassName.equals("MyException")) {
            // fix
            expectedExceptionClassName = "MyRuntimeException";
        }

        // [PexRaisedException(typeof(OverflowException))]

        //
        // Conversions
        //
        Stream<String> linesStream = testMethodLines.stream();

        // add "test_" prefix and "throws Exception"
        linesStream = linesStream.map(line -> {
            if (line.trim().startsWith("public void ")) {
                return line.replaceFirst("public void ", "public void test_") + " throws Exception";
            } else {
                return line;
            }
        });

        // convert string => String
        linesStream = linesStream.map(line -> {
            if (line.trim().startsWith("string ")) {
                return line.replaceFirst("string ", "String ");
            } else {
                return line;
            }
        });

        // convert bool => boolean
        linesStream = linesStream.map(line -> {
            if (line.trim().startsWith("bool ")) {
                return line.replaceFirst("bool ", "boolean ");
            } else {
                return line;
            }
        });

        // convert enum cast
        Pattern patternEnum = Pattern.compile("^(?<begin>.*)\\(State\\)(?<num>\\d+)(?<end>.*)$");
        linesStream = linesStream.map(line -> {
            Matcher m = patternEnum.matcher(line);
            if (m.matches()) {
                return m.group("begin") + "State.values()[" + m.group("num") + "]" + m.group("end");
            } else {
                return line;
            }
        });

        // convert default(MyCustomType)
        Pattern patternDefault = Pattern
                .compile("^(?<begin>.*)default\\((?<type>[A-Za-z0-9_]+)\\)(?<end>.*)$");
        linesStream = linesStream.map(line -> {
            while (true) {
                Matcher m = patternDefault.matcher(line);
                if (m.matches()) {
                    line = m.group("begin") + "new " + m.group("type") + "()" + m.group("end");
                } else {
                    return line;
                }
            }
        });

        // convert generic <int>
        Pattern patternGeneric = Pattern
                .compile("^(?<begin>.*[A-Za-z0-9_])\\<(?<type>int|double)\\>(?<end>.*)$");
        linesStream = linesStream.map(line -> {
            while (true) {
                Matcher m = patternGeneric.matcher(line);
                if (m.matches()) {
                    String newType;
                    switch (m.group("type")) {
                        case "int":
                            newType = "Integer";
                            break;
                        case "double":
                            newType = "Double";
                            break;
                        default:
                            throw new RuntimeException("WTF");
                    }

                    line = m.group("begin") + "<" + newType + ">" + m.group("end");
                } else {
                    return line;
                }
            }
        });

        // convert "this.oneParamBooleanTest(false)" => "B1a_PrimitiveTypes.oneParamBoolean(false)"
        String callSearch = "this\\." + snippet.getName() + "Test";
        linesStream = linesStream.map(line -> {
            if (line.matches(".*" + callSearch + ".*")) {
                return line.replaceAll(callSearch,
                        snippetContainer.getName() + "." + snippet.getName());
            } else {
                return line;
            }
        });

        // convert "new string('\0', 32)" => "new string("\0\0\0....")"
        Pattern newStringRepeatChar = Pattern.compile(
                "^(?<begin>.*)"
                        + "new string\\('"
                        + "(?<char>[^()]*)"
                        + "', "
                        + "(?<cnt>\\d+)"
                        + "\\)"
                        + "(?<end>.*)$");
        linesStream = linesStream.map(line -> {
            while (true) {
                Matcher m = newStringRepeatChar.matcher(line);
                if (m.matches()) {
                    String ch = m.group("char");
                    int cnt = Integer.parseInt(m.group("cnt"));
                    String s = "";
                    for (int i = 0; i < cnt; i++) {
                        s += ch;
                    }

                    line = m.group("begin") + "new String(\"" + s + "\")" + m.group("end");
                } else {
                    return line;
                }
            }
        });

        // convert "PexSafeHelpers.ByteToBoolean((byte)251)" => true or false
        Pattern pexByteToBooleanPattern = Pattern.compile(
                "^(?<begin>.*)"
                        + "PexSafeHelpers[.]ByteToBoolean"
                        + "\\("
                        + "\\(byte\\)"
                        + "(?<num>\\d+)"
                        + "\\)"
                        + "(?<end>.*)$");
        linesStream = linesStream.map(line -> {
            while (true) {
                Matcher m = pexByteToBooleanPattern.matcher(line);
                if (m.matches()) {
                    String boolValue = m.group("num").trim().equals("0") ? "false" : "true";
                    line = m.group("begin") + boolValue + m.group("end");
                } else {
                    return line;
                }
            }
        });

        // convert asserts
        linesStream = linesStream.map(line -> {
            Matcher m = CS_ASSERT_PATTERN.matcher(line);
            if (m.matches()) {
                return m.group("indent") + "assertEquals(" + m.group("params") + ");";
            } else {
                return line;
            }
        });

        // translation
        LinkedHashMap<String, String> translate = new LinkedHashMap<>();
        translate.put("sbyte.MinValue", "0"); // no signed byte in Java
        translate.put("sbyte.MaxValue", "255"); // no signed byte in Java

        translate.put("byte.MinValue", "Byte.MIN_VALUE");
        translate.put("byte.MaxValue", "Byte.MAX_VALUE");
        translate.put("short.MinValue", "Short.MIN_VALUE");
        translate.put("short.MaxValue", "Short.MAX_VALUE");
        translate.put("int.MinValue", "Integer.MIN_VALUE");
        translate.put("int.MaxValue", "Integer.MAX_VALUE");
        translate.put("long.MinValue", "Long.MIN_VALUE");
        translate.put("long.MaxValue", "Long.MAX_VALUE");
        translate.put("float.MinValue", "Float.MIN_VALUE");
        translate.put("float.MaxValue", "Float.MAX_VALUE");
        translate.put("double.MinValue", "Double.MIN_VALUE");
        translate.put("double.MaxValue", "Double.MAX_VALUE");

        translate.put("float.NegativeInfinity", "Float.NEGATIVE_INFINITY");
        translate.put("float.PositiveInfinity", "Float.POSITIVE_INFINITY");
        translate.put("double.NegativeInfinity", "Double.NEGATIVE_INFINITY");
        translate.put("double.PositiveInfinity", "Double.POSITIVE_INFINITY");

        translate.put("(object)", "(Object)");
        translate.put("(string)", "(String)");
        translate.put(".Length", ".length");
        translate.put("Assert.IsNotNull(", "assertNotNull(");

        translate.put("new int()", "new Integer(0)"); // may happen after handling default(MyType)
        translate.put("new List", "new ArrayList");
        translate.put("new string", "new String");
        translate.put("(IEnumerable<Integer>)ints", "Arrays.asList(ints)");

        translate.put("guessIntegerNoHelp<Integer>", "guessIntegerNoHelp");
        translate.put("guessTypeAndUse<Integer>", "<Integer>guessTypeAndUse");
        translate.put("guessSafe<Integer>", "guessSafe");
        translate.put("guessSafeNoHelp<Integer>", "guessSafeNoHelp");
        translate.put("guessType<Integer>", "<Integer>guessType");

        if (snippet.getName().startsWith("guessGenericListWith")) {
            // "int[] ints = new int[1];" => "Integer[] ints = new Object[1]";
            translate.put("int[] ints = new int[", "Integer[] ints = new Integer[");
        } else if (snippet.getName().startsWith("guessGenericVector")
                || snippet.getName().startsWith("guessVector")) {
            translate.put("ArrayList", "Vector");
        }

        // this was replaced by filtering for Vector snippets
        // translate.put("(ArrayList) null", "null");
        // translate.put("(ArrayList)null", "null");

        translate.put("new GenericTriplet<Double>(0, 0, 0)",
                "new GenericTriplet<Double>((double) 0, (double) 0, (double) 0)");

        translate.put("\\v", "\\u0013");
        translate.put("\\a", "\\u0007");

        translate.put("L3_Wrappers.guessDouble(", "L3_Wrappers.guessDouble((double) ");

        translate.put("PexSafeHelpers.ByteToBoolean((byte)Byte.MAX_VALUE)", "true");

        for (Entry<String, String> tr : translate.entrySet()) {
            linesStream = linesStream.map(line -> line.replace(tr.getKey(), tr.getValue()));
        }

        if (expectedExceptionClassName != null) {
            // indent lines (greater indent here)
            testMethodLines = linesStream.map(line -> "        " + line)
                    .collect(toList());
            // 0: public void test_...
            // 1: {
            testMethodLines.add(2, "try {");
            // previous content is here
            testMethodLines.add(testMethodLines.size() - 1, "    fail();");
            testMethodLines.add(testMethodLines.size() - 1,
                    "} catch (" + expectedExceptionClassName + " ex) {");
            testMethodLines.add(testMethodLines.size() - 1, "    // ok");
            testMethodLines.add(testMethodLines.size() - 1, "}");
        } else {
            // indent and add
            testMethodLines = linesStream.map(line -> "    " + line).collect(toList());
        }

        javaLines.addAll(testMethodLines);
        javaLines.add("");
    }

    private static void saveTestMethodLines(List<List<String>> testMethods,
            List<String> testMethodLines) {
        if (testMethodLines != null) {
            testMethods.add(testMethodLines);
        }
    }

    private static String resolveExceptionClass(String exceptionTypeStr) {
        switch (exceptionTypeStr) {
            case "NullReferenceException":
                return "NullPointerException";

            case "ArgumentException":
                return "IllegalArgumentException";

            case "NotSupportedException":
                return "UnsupportedOperationException";

            case "ArgumentOutOfRangeException":
                return "StringIndexOutOfBoundsException";

            case "IndexOutOfRangeException":
                return "IndexOutOfBoundsException";

            case "InvalidOperationException":
                return "IllegalStateException";

            case "InvalidCastException":
                return "ClassCastException";

            case "DivideByZeroException":
                return "ArithmeticException";

            case "OverflowException":
                // not present in Java
                return null;
            default:
                return exceptionTypeStr;
        }
    }

}
