package hu.bme.mit.sette.common.model.runner.xml;

import hu.bme.mit.sette.common.validator.AbstractValidator;
import hu.bme.mit.sette.common.validator.GeneralValidator;
import hu.bme.mit.sette.common.validator.exceptions.ValidatorException;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.simpleframework.xml.Element;

/**
 * Represents a file coverage element.
 */
public class FileCoverageElement implements XmlElement {
    /** The name. */
    @Element(name = "name", data = true)
    private String name;

    /** The fully covered lines. */
    @Element(name = "fullyCoveredLines", data = true)
    private String fullyCoveredLines;

    /** The partially covered lines. */
    @Element(name = "partiallyCoveredLines", data = true)
    private String partiallyCoveredLines;

    /** The not covered lines. */
    @Element(name = "notCoveredLines", data = true)
    private String notCoveredLines;

    /**
     * Instantiates a file coverage element.
     */
    public FileCoverageElement() {
    }

    /**
     * Gets the name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name.
     *
     * @param name
     *            the new name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the fully covered lines.
     *
     * @return the fully covered lines
     */
    public String getFullyCoveredLines() {
        return fullyCoveredLines;
    }

    /**
     * Sets the fully covered lines.
     *
     * @param fullyCoveredLines
     *            the new fully covered lines
     */
    public void setFullyCoveredLines(String fullyCoveredLines) {
        this.fullyCoveredLines = fullyCoveredLines;
    }

    /**
     * Gets the partially covered lines.
     *
     * @return the partially covered lines
     */
    public String getPartiallyCoveredLines() {
        return partiallyCoveredLines;
    }

    /**
     * Sets the partially covered lines.
     *
     * @param partiallyCoveredLines
     *            the new partially covered lines
     */
    public void setPartiallyCoveredLines(String partiallyCoveredLines) {
        this.partiallyCoveredLines = partiallyCoveredLines;
    }

    /**
     * Gets the not covered lines.
     *
     * @return the not covered lines
     */
    public String getNotCoveredLines() {
        return notCoveredLines;
    }

    /**
     * Sets the not covered lines.
     *
     * @param notCoveredLines
     *            the new not covered lines
     */
    public void setNotCoveredLines(String notCoveredLines) {
        this.notCoveredLines = notCoveredLines;
    }

    /*
     * (non-Javadoc)
     *
     * @see hu.bme.mit.sette.common.model.runner.xml.XmlElement#validate()
     */
    @Override
    public void validate() throws ValidatorException {
        GeneralValidator v = new GeneralValidator(this);

        // TODO validate notnull
        // TODO lines can be blank respectively

        Set<Integer> setA = validateAndGetLineNumbers(
                fullyCoveredLines, v);
        Set<Integer> setB = validateAndGetLineNumbers(
                partiallyCoveredLines, v);
        Set<Integer> setC = validateAndGetLineNumbers(notCoveredLines,
                v);

        Collection<Integer> intersection1 = CollectionUtils
                .intersection(setA, setB);
        Collection<Integer> intersection2 = CollectionUtils
                .intersection(setA, setC);
        Collection<Integer> intersection3 = CollectionUtils
                .intersection(setB, setC);

        TreeSet<Integer> union = new TreeSet<>();
        union.addAll(intersection1);
        union.addAll(intersection2);
        union.addAll(intersection3);

        if (!union.isEmpty()) {
            // TODO enhance message
            v.addException("No common elements are allowed ("
                    + StringUtils.join(union, ", ") + ")");
        }

        v.validate();
    }

    /**
     * Validates and gets the line numbers.
     *
     * @param lines
     *            the lines
     * @param validator
     *            a validator
     * @return the line numbers
     */
    private Set<Integer> validateAndGetLineNumbers(String lines,
            AbstractValidator<?> validator) {
        String[] parts = lines.split("\\s+");
        Set<Integer> lineNumbers = new HashSet<>();

        // TODO enhance validator messages
        for (String part : parts) {
            if (StringUtils.isBlank(part)) {
                continue;
            }

            try {
                int lineNumber = Integer.parseInt(part);

                if (lineNumber >= 1) {
                    if (!lineNumbers.contains(lineNumber)) {
                        lineNumbers.add(lineNumber);
                    } else {
                        validator
                        .addException("Duplicates are not allowed");
                    }
                } else {
                    validator
                    .addException("Only positive integers are allowed");
                }
            } catch (NumberFormatException e) {
                validator.addException(
                        "Only positive integers are allowed (invalid: "
                                + part + ")", e);
            }
        }

        return lineNumbers;
    }
}
