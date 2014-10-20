package hu.bme.mit.sette.tools.jpet.xmlparser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class TestCase {
    private final List<DataOrRef> argsIn;
    private final Map<String, HeapElement> heapIn;
    private final Map<String, HeapElement> heapOut;
    private String exceptionFlag = null;

    public TestCase() {
        argsIn = new ArrayList<>();
        heapIn = new HashMap<>();
        heapOut = new HashMap<>();
    }

    public List<DataOrRef> argsIn() {
        return argsIn;
    }

    public Map<String, HeapElement> heapIn() {
        return heapIn;
    }

    public Map<String, HeapElement> heapOut() {
        return heapOut;
    }

    public String getExceptionFlag() {
        return exceptionFlag;
    }

    public void setExceptionFlag(String exceptionFlag) {
        this.exceptionFlag = exceptionFlag;
    }
}
