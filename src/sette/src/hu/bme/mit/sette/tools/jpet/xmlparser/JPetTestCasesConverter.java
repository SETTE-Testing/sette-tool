package hu.bme.mit.sette.tools.jpet.xmlparser;

import hu.bme.mit.sette.common.model.runner.ParameterType;
import hu.bme.mit.sette.common.model.runner.xml.AbstractParameterElement;
import hu.bme.mit.sette.common.model.runner.xml.InputElement;
import hu.bme.mit.sette.common.model.runner.xml.ParameterElement;
import hu.bme.mit.sette.common.model.runner.xml.SnippetInputsXml;
import hu.bme.mit.sette.common.model.snippet.Snippet;
import hu.bme.mit.sette.common.validator.exceptions.ValidatorException;
import hu.bme.mit.sette.tools.jpet.JPetTypeConverter;
import hu.bme.mit.sette.tools.jpet.xmlparser.HeapElement.HeapArray;
import hu.bme.mit.sette.tools.jpet.xmlparser.HeapElement.HeapObject;
import hu.bme.mit.sette.tools.jpet.xmlparser.HeapElement.HeapObject.HeapObjectField;

import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public final class JPetTestCasesConverter {
    private static final Pattern exceptionFlagPattern = Pattern
            .compile("exception\\((.*)\\)");

    public static void convert(Snippet snippet,
            List<TestCase> testCases, SnippetInputsXml inputsXml)
                    throws ValidatorException {
        // TODO Auto-generated method stub

        for (TestCase testCase : testCases) {
            InputElement inputElement = createInputElement(snippet,
                    testCase);

            if (inputElement != null) {
                inputsXml.getGeneratedInputs().add(inputElement);
            } else {
                System.err.println("Invalid input, skip");
            }
        }

    }

    private static InputElement createInputElement(Snippet snippet,
            TestCase testCase) throws ValidatorException {
        InputElement inputElement = new InputElement();

        // heap
        String heap = parseHeap(snippet, testCase);

        // TODO debug
        // if (heap.length() > 0) {
        // System.err.println(">>>>>>>>>>>>>>>");
        // System.err.println(heap);
        // System.err.println("<<<<<<<<<<<<<<<");
        // inputElement.setHeap(heap.toString());
        // }

        if (heap.length() > 0) {
            inputElement.setHeap(heap.toString());
        }

        // parameters
        int paramIndex = 0;
        for (DataOrRef arg : testCase.argsIn()) {
            AbstractParameterElement parameterElement = createParameterElement(
                    snippet, testCase, arg, paramIndex);

            if (parameterElement != null) {
                inputElement.getParameters().add(parameterElement);
            } else {
                System.err
                .println("Invalid parameter element, skipping input");
                return null;
            }
            paramIndex++;
        }

        // expected exception
        if (testCase.getExceptionFlag().equals("ok")) {
            inputElement.setExpected(null);
        } else {
            Matcher matcher = exceptionFlagPattern.matcher(testCase
                    .getExceptionFlag());

            if (matcher.matches()) {
                String num = StringUtils.trimToNull(matcher.group(1));

                if (testCase.heapOut().containsKey(num)) {
                    String jPetClassName = testCase.heapOut().get(num)
                            .asHeapObject().getClassName();
                    inputElement.setExpected(JPetTypeConverter
                            .toJava(jPetClassName));
                } else {
                    // TODO error handling
                    throw new RuntimeException(
                            "Cannot find exception object referenced as '"
                                    + num + "' in heap_out.");
                }
            } else {
                // TODO error handling
                throw new RuntimeException("Bad exception_flag: "
                        + testCase.getExceptionFlag()
                        + " (it should match: "
                        + exceptionFlagPattern.toString() + ")");
            }
        }

        inputElement.validate();
        return inputElement;
    }

    private static String parseHeap(Snippet snippet, TestCase testCase) {
        StringBuilder heap = new StringBuilder();
        StringBuilder heapSet = new StringBuilder();

        for (Entry<String, HeapElement> heapEntry : testCase.heapIn()
                .entrySet()) {
            String num = heapEntry.getKey();
            HeapElement element = heapEntry.getValue();

            heap.append("// heap_").append(num).append('\n');

            if (element.isArray()) {
                HeapArray heapArray = element.asHeapArray();

                String javaType = JPetTypeConverter.toJava(heapArray
                        .getType());

                heap.append(
                        String.format("%s[] heap_%s = new %s[%s];",
                                javaType, num, javaType,
                                heapArray.getNumElems())).append('\n');

                int i = 0;
                for (DataOrRef arg : heapArray.args()) {
                    if (arg.isData()) {
                        heapSet.append(
                                String.format("heap_%s[%d] = %s;", num,
                                        i, arg.getText())).append('\n');
                    } else if (arg.isRef()) {
                        heapSet.append(
                                String.format("heap_%s[%d] = heap_%s;",
                                        num, i, arg.getText())).append(
                                                '\n');
                    } else {
                        // TODO error handling
                        throw new RuntimeException(
                                "Neither data, nor ref");
                    }
                    i++;
                }
            } else if (element.isObject()) {
                // TODO enhance

                HeapObject heapObject = element.asHeapObject();

                String javaType = JPetTypeConverter.toJava(heapObject
                        .getClassName());

                heap.append(
                        String.format("%s heap_%s = new %s();",
                                javaType, num, javaType)).append('\n');

                // set fields via reflection
                for (HeapObjectField field : heapObject.fields()) {
                    String[] fieldNameParts = StringUtils.split(
                            field.getFieldName(), ":", 2);
                    String fieldName = StringUtils
                            .trimToNull(fieldNameParts[0]);
                    // String fieldType = JPetTypeConverter
                    // .toJava(StringUtils
                    // .trimToNull(fieldNameParts[1]));

                    // generated example:
                    // // heap_A
                    // Object heap_A = new Object();
                    //
                    // // field x
                    // java.lang.reflect.Field heap_A_x = null;
                    // for (java.lang.reflect.Field f : heap_A
                    // .getClass().getFields()) {
                    // if ("x".equals(f.getName())) {
                    // heap_A_x = f;
                    // break;
                    // }
                    // }
                    //
                    // if (heap_A_x == null) {
                    // for (java.lang.reflect.Field f : heap_A
                    // .getClass().getDeclaredFields()) {
                    // if ("x".equals(f.getName())) {
                    // heap_A_x = f;
                    // break;
                    // }
                    // }
                    // }
                    //
                    // if (heap_A_x == null) {
                    // throw new RuntimeException(
                    // "The field x was not found in class Object");
                    // }
                    //
                    // heap_A_x.setAccessible(true);
                    // heap_A_x.set(heap_A, "my value");
                    // heap_A_x.setAccessible(false);

                    heap.append("// field ").append(fieldName)
                    .append('\n');
                    heap.append(
                            String.format(
                                    "java.lang.reflect.Field heap_%s_%s = null;",
                                    num, fieldName)).append('\n');

                    heap.append("for (java.lang.reflect.Field f : heap_"
                            + num + ".getClass().getFields()) {\n");
                    heap.append("    if (\"" + fieldName
                            + "\".equals(f.getName())) {\n");
                    heap.append("        heap_" + num + "_" + fieldName
                            + " = f;\n");
                    heap.append("        break;\n");
                    heap.append("    }\n");
                    heap.append("}\n\n");

                    heap.append("if (heap_" + num + "_" + fieldName
                            + " == null) {\n");
                    heap.append("    for (java.lang.reflect.Field f : heap_"
                            + num
                            + ".getClass().getDeclaredFields()) {\n");
                    heap.append("        if (\"" + fieldName
                            + "\".equals(f.getName())) {\n");
                    heap.append("            heap_" + num + "_"
                            + fieldName + " = f;\n");
                    heap.append("            break;\n");
                    heap.append("        }\n");
                    heap.append("    }\n");
                    heap.append("}\n\n");

                    // TODO exception if null

                    heapSet.append("heap_" + num + "_" + fieldName
                            + ".setAccessible(true);\n");

                    if (field.getDataOrRef().isData()) {
                        heapSet.append("heap_" + num + "_" + fieldName
                                + ".set(heap_" + num + ", "
                                + field.getDataOrRef().getText()
                                + ");\n");
                    } else if (field.getDataOrRef().isRef()) {
                        heapSet.append("heap_" + num + "_" + fieldName
                                + ".set(heap_" + num + ", heap_"
                                + field.getDataOrRef().getText()
                                + ");\n");
                    } else {
                        // TODO error handling
                        throw new RuntimeException(
                                "Neither data, nor ref");
                    }
                    heapSet.append("heap_" + num + "_" + fieldName
                            + ".setAccessible(false);\n\n");
                }
                // TODO debug
                // System.err.println(heap);
                // System.exit(0);
            } else {
                // TODO handle error
                throw new RuntimeException(
                        "FATAL ERROR: neither array, nor object!");
            }

            heap.append('\n');
        }

        return heap.append(heapSet).toString();
    }

    private static AbstractParameterElement createParameterElement(
            Snippet snippet, TestCase testCase, DataOrRef arg,
            int paramIndex) throws ValidatorException {
        if (arg.isData()) {
            ParameterElement parameterElement = new ParameterElement();

            if (arg.getText().equals("null")) {
                parameterElement.setType(ParameterType.EXPRESSION);
                parameterElement.setValue("null");
            } else {
                // TODO enhance
                try {
                    Class<?>[] paramTypes = snippet.getMethod()
                            .getParameterTypes();

                    int intVal = Integer.parseInt(arg.getText());

                    parameterElement
                    .setType(ParameterType
                            .primitiveFromJavaClass(paramTypes[paramIndex]));

                    if (paramTypes[paramIndex].equals(boolean.class)) {
                        parameterElement.setValue(intVal != 0);
                    } else {
                        parameterElement.setValue(intVal);
                    }
                } catch (NumberFormatException e) {
                    // problem: jpet sometimes generates refs in <data> fields
                    // with no real heap object!
                    parameterElement.setType(ParameterType.EXPRESSION);
                    parameterElement.setValue("null");
                }

            }

            parameterElement.validate();
            return parameterElement;
        } else if (arg.isRef()) {
            HeapElement heapElement = testCase.heapIn().get(
                    arg.getText());

            if (heapElement == null) {
                System.err.println("Missing referenced heap element ("
                        + arg.getText() + "), assuming null");

                ParameterElement parameterElement = new ParameterElement();
                parameterElement.setType(ParameterType.EXPRESSION);
                parameterElement.setValue("null");
                parameterElement.validate();
                return parameterElement;
            } else {
                // TODO: implement better heap support

                ParameterElement parameterElement = new ParameterElement();
                parameterElement.setType(ParameterType.EXPRESSION);
                parameterElement.setValue("heap_"
                        + heapElement.getNum());
                parameterElement.validate();
                return parameterElement;
            }
        } else {
            // TODO error handling
            throw new RuntimeException(
                    "Unknown arg (neither data nor ref), FATAL ERROR: "
                            + arg);
        }
    }
}
