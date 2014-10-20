package hu.bme.mit.sette.tools.jpet.xmlparser;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

final class Tag {
    private final TagType type;
    private final boolean isOpening;

    private Tag(TagType pType, boolean isOpening) {
        Validate.notNull(pType, "The type must not be null");

        type = pType;
        this.isOpening = isOpening;
    }

    public TagType getType() {
        return type;
    }

    public boolean isOpening() {
        return isOpening;
    }

    public boolean isClosing() {
        return !isOpening;
    }

    public static Tag createOpeningTag(TagType type) {
        return new Tag(type, true);
    }

    public static Tag createOpeningTag(String type) {
        return createOpeningTag(TagType.fromString(type));
    }

    public static Tag createClosingTag(TagType type) {
        return new Tag(type, false);
    }

    public static Tag createClosingTag(String type) {
        return createClosingTag(TagType.fromString(type));
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        if (isOpening) {
            return type.toString();
        } else {
            return type.closingToString();
        }
    }

    public void validateParentTag(Tag parentTag) {
        TagType[] validParentTagTypes = type.getValidParentTagTypes();

        Validate.notEmpty(validParentTagTypes,
                "The array of valid parent tag types must not be empty or null");

        List<String> validParentTagTypesAsString = new ArrayList<>();

        for (TagType validParentTagType : validParentTagTypes) {
            if (parentTag != null
                    && parentTag.getType() == validParentTagType) {
                // parent is not ROOT and type OK
                return;
            } else if (parentTag == null && validParentTagType == null) {
                // parent is ROOT and type OK
                return;
            }

            if (validParentTagType == null) {
                validParentTagTypesAsString.add("ROOT");
            } else {
                validParentTagTypesAsString.add(validParentTagType
                        .toString());
            }
        }

        // invalid
        String message = String.format(
                "The parent of %s must be in %s, not in %s", this,
                StringUtils.join(validParentTagTypesAsString, " or "),
                parentTag == null ? "ROOT" : parentTag.toString());
        Validate.isTrue(false, message);
    }
}