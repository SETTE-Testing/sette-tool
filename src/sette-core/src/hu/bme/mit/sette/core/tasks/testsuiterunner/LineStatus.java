package hu.bme.mit.sette.core.tasks.testsuiterunner;

import org.jacoco.core.analysis.ICounter;

public enum LineStatus {
    EMPTY,
    NOT_COVERED,
    FULLY_COVERED,
    PARTLY_COVERED;
    // TODO for future use to show if a line is PARTLY covered several times 
    // FULLY_OR_PARTLY_COVERED; 

    public static LineStatus fromJaCoCo(int status) {
        switch (status) {
            case ICounter.NOT_COVERED:
                return NOT_COVERED;

            case ICounter.FULLY_COVERED:
                return FULLY_COVERED;

            case ICounter.PARTLY_COVERED:
                return LineStatus.PARTLY_COVERED;

            // FIXME if not known in ICounter, fail?
            // case ICounter.EMPTY:
            default:
                return LineStatus.EMPTY;
        }
    }

    public boolean countsForStatementCoverage() {
        return this != EMPTY && this != NOT_COVERED;
    }
}
