package hu.bme.mit.sette.tools.jpet;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

public final class JPetTypeConverter {
    /**
     * Static class.
     */
    private JPetTypeConverter() {
        throw new UnsupportedOperationException("Static class");
    }

    private static final BidiMap<String, String> javaPrimitivesToJPetMapping;

    static {
        javaPrimitivesToJPetMapping = new DualHashBidiMap<>();
        javaPrimitivesToJPetMapping.put("byte", "B");
        javaPrimitivesToJPetMapping.put("short", "S");
        javaPrimitivesToJPetMapping.put("int", "I");
        javaPrimitivesToJPetMapping.put("long", "J");
        javaPrimitivesToJPetMapping.put("float", "F");
        javaPrimitivesToJPetMapping.put("double", "D");
        javaPrimitivesToJPetMapping.put("boolean", "Z");
        javaPrimitivesToJPetMapping.put("char", "C");
        javaPrimitivesToJPetMapping.put("void", "V");
    }

    public static String fromJava(String javaType) {
        Validate.notBlank(javaType, "The Java type must not be blank");

        if (javaType.startsWith("[") || javaType.endsWith("[]")) {
            // array
            // e.g.: int[][] => [[I
            // e.g.: [[I => [[I
            // e.g.: java.lang.Object[][] => [[java/lang/Object
            // e.g.: [[java.lang.Object => [[java/lang/Object

            // eliminate []'s
            // (int[][] -> int, brackets=2; [[I -> I, brackets=2)

            int brackets = 0;

            while (javaType.startsWith("[")) {
                javaType = StringUtils.substring(javaType, 1);
                brackets++;
            }

            while (javaType.endsWith("[]")) {
                javaType = StringUtils.substring(javaType, 0, -2);
                brackets++;
            }

            // convert the primitive or object
            javaType = fromJava(javaType);

            // pad and return
            return StringUtils.leftPad(javaType, javaType.length()
                    + brackets, '[');
        } else if (javaPrimitivesToJPetMapping.containsKey(javaType)) {
            // primitive type
            // e.g.: int => I
            return javaPrimitivesToJPetMapping.get(javaType);
        } else if (javaPrimitivesToJPetMapping.containsValue(javaType)) {
            // primitive type
            // e.g.: I => I
            return javaType;
        } else {
            // object
            if (javaType.startsWith("L") && javaType.endsWith(";")) {
                // e.g.: Ljava.lang.Object; => Ljava/lang/Object;
                return javaType.replace('.', '/');
            } else {
                // e.g.: java.lang.Object => Ljava/lang/Object;
                return 'L' + javaType.replace('.', '/') + ';';
            }
        }
    }

    public static String fromJava(Class<?> javaType) {
        Validate.notNull(javaType, "The Java type must not be null");
        return fromJava(javaType.getName());
    }

    public static String toJava(String jPetType) {
        Validate.notBlank(jPetType, "The jPet type must not be blank");

        if (jPetType.startsWith("[")) {
            // array
            // e.g.: [[I => [[I
            // e.g.: [[java/lang/Object => [[java.lang.Object
            return jPetType.replace('/', '.');

            // // eliminate ['s
            // // ([[I -> I, brackets=2)
            //
            // int brackets = 0;
            //
            // while (jPetType.startsWith("[")) {
            // jPetType = StringUtils.substring(jPetType, 1);
            // brackets++;
            // }
            //
            // // convert the primitive or object
            // jPetType = jPetType.replace('/', '.');
            //
            // // pad and return
            // return StringUtils.leftPad(jPetType, jPetType.length()
            // + brackets, '[');
        } else if (javaPrimitivesToJPetMapping.containsValue(jPetType)) {
            // primitive type
            // e.g.: I -> int
            return javaPrimitivesToJPetMapping.getKey(jPetType);
        } else if (jPetType.startsWith("L") && jPetType.endsWith(";")) {
            // e.g.: Ljava/lang/Object; => java.lang.Object
            return StringUtils.substring(jPetType, 1, -1).replace('/',
                    '.');
        } else {
            // e.g.: java/lang/Object => java.lang.Object
            return jPetType.replace('/', '.');
        }
    }

    public static Class<?> toJava(String jPetType,
            ClassLoader classLoader) throws ClassNotFoundException {
        Validate.notBlank(jPetType, "The jPet type must not be blank");
        Validate.notNull(classLoader,
                "The classloader must not be null");

        String javaType = toJava(jPetType);

        if (javaType.equals("void")) {
            // void is not handled by ClassUtils.getClass()
            return void.class;
        } else {
            // this method also handles the primitive types
            return ClassUtils.getClass(classLoader, javaType, false);
        }
    }
}
