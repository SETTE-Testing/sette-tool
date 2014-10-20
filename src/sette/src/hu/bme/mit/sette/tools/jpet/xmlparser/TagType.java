package hu.bme.mit.sette.tools.jpet.xmlparser;

import java.util.Arrays;

import org.apache.commons.lang3.Validate;

enum TagType {
    /** The &lt;pet&gt; (root) tag. */
    PET("pet", (TagType) null),

    /** The &lt;test_case&gt; tag (child of &lt;pet&gt;). */
    TEST_CASE("test_case", TagType.PET),

    /** The &lt;method&gt; tag (child of &lt;test_case&gt;). */
    METHOD("method", TagType.TEST_CASE),
    /** The &lt;args_in&gt; tag (child of &lt;test_case&gt;). */
    ARGS_IN("args_in", TagType.TEST_CASE),
    /** The &lt;heap_in&gt; tag (child of &lt;test_case&gt;). */
    HEAP_IN("heap_in", TagType.TEST_CASE),
    /** The &lt;heap_out&gt; tag (child of &lt;test_case&gt;). */
    HEAP_OUT("heap_out", TagType.TEST_CASE),
    /** The &lt;return&gt; tag (child of &lt;test_case&gt;). */
    RETURN("return", TagType.TEST_CASE),
    /** The &lt;exception_flag&gt; tag (child of &lt;test_case&gt;). */
    EXCEPTION_FLAG("exception_flag", TagType.TEST_CASE),
    /** The &lt;trace&gt; tag (child of &lt;test_case&gt;). */
    TRACE("trace", TagType.TEST_CASE),
    /** The &lt;input_constraints&gt; tag (child of &lt;test_case&gt;). */
    INPUT_CONSTRAINTS("input_constraints", TagType.TEST_CASE),
    /** The &lt;output_constraints&gt; tag (child of &lt;test_case&gt;). */
    OUTPUT_CONSTRAINTS("output_constraints", TagType.TEST_CASE),
    /** The &lt;params&gt; tag (child of &lt;test_case&gt;). */
    PARAMS("params", TagType.TEST_CASE),

    /**
     * The &lt;elem&gt; tag (child of &lt;heap_in&gt; or &lt;heap_out&gt;).
     */
    ELEM("elem", TagType.HEAP_IN, TagType.HEAP_OUT),
    /**
     * The &lt;num&gt; tag (child of &lt;elem&gt;, denotes the name of the
     * element which is used in &lt;ref&gt;).
     */
    NUM("num", TagType.ELEM),

    /** The &lt;array&gt; tag (child of &lt;elem&gt;). */
    ARRAY("array", TagType.ELEM),
    /** The &lt;type&gt; tag (child of &lt;array&gt;). */
    TYPE("type", TagType.ARRAY),
    /** The &lt;num_elems&gt; tag (child of &lt;array&gt;). */
    NUM_ELEMS("num_elems", TagType.ARRAY),
    /** The &lt;args&gt; tag (child of &lt;array&gt;). */
    ARGS("args", TagType.ARRAY),
    /** The &lt;arg&gt; tag (child of &lt;args&gt;). */
    ARG("arg", TagType.ARGS),

    /** The &lt;object&gt; tag (child of &lt;elem&gt;). */
    OBJECT("object", TagType.ELEM),
    /** The &lt;class_name;&gt; tag (child of &lt;object&gt;). */
    CLASS_NAME("class_name", TagType.OBJECT),
    /** The &lt;fields&gt; tag (child of &lt;object&gt;). */
    FIELDS("fields", TagType.OBJECT),
    /** The &lt;field&gt; tag (child of &lt;fields&gt;). */
    FIELD("field", TagType.FIELDS),
    /** The &lt;field_name&gt; tag (child of &lt;field&gt;). */
    FIELD_NAME("field_name", TagType.FIELD),

    /** The &lt;data&gt; tag (denotes a data value). */
    DATA("data", TagType.ARGS_IN, TagType.FIELD),
    /** The &lt;ref&gt; tag (denotes a reference to an array or object). */
    REF("ref", TagType.ARGS_IN, TagType.ARGS, TagType.FIELD);

    /** The value of the XML attribute. */
    private final String tagName;
    private final TagType[] validParentTagTypes;

    /**
     * Initialises the instance.
     *
     * @param pTagName
     *            The name of the XML tag.
     */
    private TagType(final String pTagName,
            final TagType... pValidParentTagTypes) {
        Validate.notEmpty(pValidParentTagTypes,
                "The array of valid parent tag types must not be empty or null");

        tagName = pTagName;
        validParentTagTypes = pValidParentTagTypes;
    }

    /**
     * Returns the name of the XML tag.
     *
     * @return The name of the XML tag.
     */
    public String getTagName() {
        return tagName;
    }

    public TagType[] getValidParentTagTypes() {
        return validParentTagTypes;
    }

    /**
     * Parses the given tag name into a {@link TagType}.
     *
     * @param tagName
     *            the tag name
     * @return the tag type
     */
    public static TagType fromString(final String tagName) {
        Validate.notBlank(tagName, "The tag name must not be blank");

        for (TagType pt : TagType.values()) {
            if (pt.tagName.equalsIgnoreCase(tagName)
                    || pt.name().equalsIgnoreCase(tagName)) {
                return pt;
            }
        }

        String message = String
                .format("Invalid tag name (tag name: [%s], valid tag names: [%s]",
                        tagName, Arrays.toString(TagType.values()));
        throw new IllegalArgumentException(message);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Enum#toString()
     */
    @Override
    public String toString() {
        return '<' + tagName + '>';
    }

    public String closingToString() {
        return "</" + tagName + '>';
    }
}