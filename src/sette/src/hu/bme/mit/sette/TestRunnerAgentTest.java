package hu.bme.mit.sette;

import hu.bme.mit.sette.core.tasks.testsuiterunner.TestSuiteRunnerForkAgent;

public class TestRunnerAgentTest {
    public static void main(String[] args) {
        String a = "D:/SETTE/sette-snippets/java/sette-snippets ";
        a += "D:/SETTE/sette-results ";
        a += "hu.bme.mit.sette.tools.randoop.RandoopTool|Randoop|D:/SETTE/sette-tool/test-generator-tools/randoop ";
        a += "run-01-30sec ";
        a += "O1_fullCoverage ";
        a += "hu.bme.mit.sette.snippets._3_objects.O1_Simple_fullCoverage_Test.RegressionTest2 ";
        a += "test003";

        a = "D:/SETTE/sette-snippets/java/sette-snippets-extra D:/SETTE/sette-results/sette-snippets-extra___randoop___run-01-30sec hu.bme.mit.sette.tools.randoop.RandoopTool|Randoop|D:/SETTE/sette-tool/test-generator-tools/randoop run-01-30sec Env2_createDir hu.bme.mit.sette.snippets._7_environment.Env2_FileIO_createDir_Test.RegressionTest0 test1";
        
        TestSuiteRunnerForkAgent.main(a.split(" "));
    }
}
