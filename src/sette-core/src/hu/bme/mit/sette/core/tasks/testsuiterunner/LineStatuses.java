package hu.bme.mit.sette.core.tasks.testsuiterunner;

import com.google.common.base.Preconditions;

public final class LineStatuses {
    private final int beginLine;
    private final int endLine;
    private final LineStatus[] lineStatuses;

    public LineStatuses(int beginLine, int endLine) {
        this.beginLine = beginLine;
        this.endLine = endLine;

        lineStatuses = new LineStatus[endLine - beginLine + 1];
        for (int i = 0; i < lineStatuses.length; i++) {
            lineStatuses[i] = LineStatus.EMPTY;
        }
    }

    public LineStatus getStatus(int lineNumber) {
        Preconditions.checkArgument(lineNumber >= beginLine, "%s < %s", lineNumber, beginLine);
        Preconditions.checkArgument(lineNumber <= endLine, "%s > %s", lineNumber, endLine);

        return lineStatuses[lineNumber - beginLine];
    }

    public void setStatus(int lineNumber, LineStatus status) {
        Preconditions.checkArgument(lineNumber >= beginLine, "%s < %s", lineNumber, beginLine);
        Preconditions.checkArgument(lineNumber <= endLine, "%s > %s", lineNumber, endLine);

        lineStatuses[lineNumber - beginLine] = status;
    }

    public void setStatus(int[] lineNumbers, LineStatus status) {
        for (int lineNumber : lineNumbers) {
            setStatus(lineNumber, status);
        }
    }
}
