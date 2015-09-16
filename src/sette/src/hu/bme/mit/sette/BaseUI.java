package hu.bme.mit.sette;

import java.io.BufferedReader;
import java.io.PrintStream;

public interface BaseUI {
    void run(BufferedReader in, PrintStream out) throws Exception;
}
