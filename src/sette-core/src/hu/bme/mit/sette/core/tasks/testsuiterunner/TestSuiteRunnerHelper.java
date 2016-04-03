package hu.bme.mit.sette.core.tasks.testsuiterunner;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketImpl;
import java.net.SocketImplFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.google.common.base.Preconditions;

import hu.bme.mit.sette.core.model.runner.ResultType;
import hu.bme.mit.sette.core.model.snippet.Snippet;

final class TestSuiteRunnerHelper {
    private static Logger log = LoggerFactory.getLogger(TestSuiteRunnerHelper.class);
    private static Set<SocketImpl> jvmSockets;

    private TestSuiteRunnerHelper() {
        throw new UnsupportedOperationException("Static class");
    }

    static {
        jvmSockets = Collections
                .synchronizedSet(new HashSet<SocketImpl>());
        try {
            ServerSocket.setSocketFactory(new SocketImplFactory() {
                @Override
                public SocketImpl createSocketImpl() {
                    SocketImpl socket = newSocketImpl();
                    jvmSockets.add(socket);
                    return socket;
                }
            });
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    // track all server sockets
    // https://github.com/orfjackal/jumi/blob/ff51720444cc23d7bfc102ba07a6211cbe9b2f7a/end-to-end-tests/src/test/java/fi/jumi/test/ReleasingResourcesTest.java
    private static SocketImpl newSocketImpl() {
        try {
            Class<?> defaultSocketImpl = Class.forName("java.net.SocksSocketImpl");
            Constructor<?> ctor = defaultSocketImpl.getDeclaredConstructor();
            ctor.setAccessible(true);
            return (SocketImpl) ctor.newInstance();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static Socket getSocket(SocketImpl sock) {
        try {
            Method getSocket = SocketImpl.class.getDeclaredMethod("getSocket");
            getSocket.setAccessible(true);
            return (Socket) getSocket.invoke(sock);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static ServerSocket getServerSocket(SocketImpl sock) {
        try {
            Method getServerSocket = SocketImpl.class.getDeclaredMethod("getServerSocket");
            getServerSocket.setAccessible(true);
            return (ServerSocket) getServerSocket.invoke(sock);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void closeSockets(Set<SocketImpl> socks) {
        System.out.println(socks);
        // close sockets
        for (SocketImpl sock : socks) {
            Socket socket = getSocket(sock);
            while (socket != null && !socket.isClosed()) {
                try {
                    log.info("Closing: " + socket);
                    socket.close();
                    jvmSockets.remove(sock);
                } catch (IOException ex) {
                    log.warn("Cannot close: " + socket);
                }
                socket = getSocket(sock);
            }
            jvmSockets.remove(sock);
        }

        // then servers
        for (SocketImpl sock : socks) {
            ServerSocket socket = getServerSocket(sock);
            while (socket != null && !socket.isClosed()) {
                try {
                    log.info("Closing: " + socket);
                    socket.close();
                } catch (IOException ex) {
                    log.warn("Cannot close: " + socket);
                }
                socket = getServerSocket(sock);
            }
            jvmSockets.remove(sock);
        }
    }

    @SuppressWarnings("deprecation")
    static void invokeMethod(Object testClassInstance, Method method, boolean checkThreads)
            throws Throwable {
        Set<Thread> threadsBeforeTest = Thread.getAllStackTraces().keySet();
        Set<SocketImpl> socketsBeforeTest = new HashSet<>(jvmSockets);

        InvokeMethodThread testCaseThread = new InvokeMethodThread(testClassInstance, method);
        testCaseThread.start();
        // FIXME no more than XX sec per test case
        testCaseThread.join(TestSuiteRunner.TEST_CASE_TIMEOUT_IN_MS);

        if (testCaseThread.isAlive()) {
            // NOTE syserr is also used by the ForkAgent, who is not aware of slf4j !!!
            // FIXME find a better way if possible (e.g. daemon thread, separate jvm, etc.)
            System.err.println("Stopping test: " + method.getName());
            try {
                testCaseThread.stop();

                // close sockets
                Set<SocketImpl> socketsAfterTest = new HashSet<>(jvmSockets);
                socketsAfterTest.removeAll(socketsBeforeTest);

                while (!socketsAfterTest.isEmpty()) {
                    log.warn("Have to close {} sockets", socketsAfterTest.size());
                    closeSockets(socketsAfterTest);

                    socketsAfterTest = new HashSet<>(jvmSockets);
                    socketsAfterTest.removeAll(socketsBeforeTest);
                }

                Set<Thread> threadsAfterTest = Thread.getAllStackTraces().keySet();
                threadsAfterTest.removeAll(threadsBeforeTest);

                if (!threadsAfterTest.isEmpty() && checkThreads) {
                    int cnt = 0;
                    while (!threadsAfterTest.isEmpty()) {
                        if (cnt > 5) {
                            for (Thread thread : threadsAfterTest) {
                                thread.setPriority(Thread.MIN_PRIORITY);
                                thread.suspend();
                                log.warn("Could not stop: " + thread.getName() + " "
                                        + thread.getClass());
                            }
                            throw new RuntimeException("!!! Cannot stop extra threads !!!");
                        }

                        // give a little time to stop
                        Thread.sleep(100);
                        threadsAfterTest.removeIf(t -> !t.isAlive());
                        log.info("Stopping threads" + threadsAfterTest.size());

                        for (Thread thread : threadsAfterTest) {
                            log.info("Stopping thread: " + thread.getName() + " "
                                    + thread.getClass());
                            thread.interrupt();
                            thread.stop();
                        }

                        cnt++;
                    }
                }

                System.err.println("Stopped test: " + method.getName());
            } catch (Throwable ex) {
                System.err.println("Thread Stop failed");
                ex.printStackTrace();
                throw new RuntimeException(ex);
            }
        }

        if (testCaseThread.invokeException != null) {
            throw testCaseThread.invokeException;
        }
    }

    private static class InvokeMethodThread extends Thread {
        private final Object testClassInstance;
        private final Method method;
        private volatile Throwable invokeException;

        public InvokeMethodThread(Object testClassInstance, Method method) {
            this.testClassInstance = testClassInstance;
            this.method = method;
        }

        @Override
        public void run() {
            Thread.currentThread()
                    .setName(testClassInstance.getClass().getSimpleName() + "_" + method.getName());
            try {
                method.invoke(testClassInstance);
            } catch (Throwable ex) {
                // reason for Throwable: ThreadDeath
                invokeException = ex;
            }
        }
    }

    static Pair<ResultType, Double> decideResultType(Snippet snippet, CoverageInfo coverageInfo)
            throws Exception {
        int linesToCover = 0;
        int linesCovered = 0;

        // iterate through files
        for (String relJavaFile : coverageInfo.data.keySet()) {
            // relJavaFile: hu/bme/mit/sette/snippets/_1_basic/B3_loops/B3c_DoWhile.java
            File javaFile = new File(
                    snippet.getContainer().getSnippetProject().getSourceDir().toFile(),
                    relJavaFile);

            // parse file
            log.debug("Parsing with JavaParser: {}", javaFile);
            CompilationUnit compilationUnit = JavaParser.parse(javaFile);
            log.debug("Parsed with JavaParser: {}", javaFile);
            int beginLine = compilationUnit.getBeginLine();
            int endLine = compilationUnit.getEndLine();

            // get "line colours" to variables
            int[] full = coverageInfo.data.get(relJavaFile).getLeft().stream().sorted()
                    .mapToInt(i -> (int) i).sorted().toArray();
            int[] partial = coverageInfo.data.get(relJavaFile).getMiddle().stream().sorted()
                    .mapToInt(i -> (int) i).sorted().toArray();
            int[] not = coverageInfo.data.get(relJavaFile).getRight().stream().sorted()
                    .mapToInt(i -> (int) i).sorted().toArray();

            // validations
            Preconditions.checkState(beginLine >= 1, relJavaFile + " begin line: " + beginLine);
            Preconditions.checkState(endLine > beginLine, relJavaFile + " end line: " + endLine);

            // lines store, set order is very important!
            LineStatuses lines = new LineStatuses(beginLine, endLine);
            lines.setStatus(not, LineStatus.NOT_COVERED);
            lines.setStatus(partial, LineStatus.PARTLY_COVERED);
            lines.setStatus(full, LineStatus.FULLY_COVERED);

            // extract method
            try {
                // if does not fail, it is the source file corresponding to the snippet
                MethodDeclaration methodDecl = getCuMethodDecl(compilationUnit,
                        snippet.getMethod().getName());
                if (methodDecl == null) {
                    throw new NoSuchElementException();
                }

                for (int lineNumber = methodDecl.getBeginLine(); lineNumber <= methodDecl
                        .getEndLine(); lineNumber++) {
                    LineStatus s = lines.getStatus(lineNumber);

                    if (s != LineStatus.EMPTY) {
                        linesToCover++;
                        if (s.countsForStatementCoverage()) {
                            linesCovered++;
                        }
                    }
                }
            } catch (NoSuchElementException ex) {
                // snippet method was not found in the file => included method in dependency
            }

            if (snippet.getIncludedConstructors().isEmpty()
                    && snippet.getIncludedMethods().isEmpty()) {
                // nothing to do
            } else {
                // handle included coverage if:
                // a) method was not found in the file (dependency file)
                // b) there is included method in the same file as the snippet
                List<BodyDeclaration> inclDecls = new ArrayList<>();

                // NOTE this might be not working (ctor)
                for (Constructor<?> ctor : snippet.getIncludedConstructors()) {
                    if (!ctor.getDeclaringClass().getSimpleName()
                            .equals(compilationUnit.getTypes().get(0).getName())) {
                        continue;
                    }

                    BodyDeclaration decl = getCuConstructorDecl(compilationUnit,
                            ctor.getName());
                    // maybe default ctor not present in source
                    if (decl != null) {
                        inclDecls.add(decl);
                    }
                }

                for (Method method : snippet.getIncludedMethods()) {
                    if (!method.getDeclaringClass().getSimpleName()
                            .equals(compilationUnit.getTypes().get(0).getName())) {
                        continue;
                    }

                    BodyDeclaration decl = getCuMethodDecl(compilationUnit, method.getName());
                    // maybe in superclass
                    if (decl != null) {
                        inclDecls.add(decl);
                    }
                }

                for (BodyDeclaration methodDecl : inclDecls) {
                    for (int lineNumber = methodDecl.getBeginLine(); lineNumber <= methodDecl
                            .getEndLine(); lineNumber++) {
                        LineStatus s = lines.getStatus(lineNumber);

                        if (s != LineStatus.EMPTY) {
                            linesToCover++;
                            if (s.countsForStatementCoverage()) {
                                linesCovered++;
                            }
                        }
                    }
                }
            }
        }

        double coverage = (double) 100 * linesCovered / linesToCover;

        if (snippet.getRequiredStatementCoverage() <= coverage + 0.1) {
            return Pair.of(ResultType.C, coverage);
        } else {
            // NOTE only for debug
            // if (snippet.getRequiredStatementCoverage() < 100) {
            // System.out.println(
            // "NOT COVERED: " + snippet.getContainer().getJavaClass().getSimpleName()
            // + " _ " + snippet.getMethod().getName());
            // System.out.println("Covered: " + linesCovered);
            // System.out.println("ToCover: " + linesToCover);
            // System.out.println("Coverage: " + coverage);
            // System.out.println("ReqCoverage: " + snippet.getRequiredStatementCoverage());
            // File htmlFile = RunnerProjectUtils.getSnippetHtmlFile(getRunnerProjectSettings(),
            // snippet);
            // System.out.println("file:///" + htmlFile.getAbsolutePath().replace('\\', '/'));
            // System.out.println("=============================================================");
            // }

            return Pair.of(ResultType.NC, coverage);
        }
    }

    private static MethodDeclaration getCuMethodDecl(CompilationUnit compilationUnit, String name) {
        try {
            return compilationUnit.getTypes().get(0).getMembers().stream()
                    .filter(bd -> bd instanceof MethodDeclaration).map(bd -> (MethodDeclaration) bd)
                    .filter(md -> md.getName().equals(name)).findAny().get();
        } catch (NoSuchElementException ex) {
            // NOTE maybe super class, skip now
            return null;
        }
    }

    private static ConstructorDeclaration getCuConstructorDecl(CompilationUnit compilationUnit,
            String name) {
        // maybe default ctor not present in source
        ConstructorDeclaration[] ctorDecls = compilationUnit.getTypes().get(0).getMembers().stream()
                .filter(bd -> bd instanceof ConstructorDeclaration)
                .map(bd -> (ConstructorDeclaration) bd).toArray(ConstructorDeclaration[]::new);

        if (ctorDecls.length == 0) {
            return null;
        } else {
            for (ConstructorDeclaration ctorDecl : ctorDecls) {
                // FIXME test this part
                if (ctorDecl.getName().equals(name)) {
                    return ctorDecl;
                }
                throw new RuntimeException("SETTE RUNTIME ERROR");
            }
            return null;
        }
    }

    static List<Class<?>> loadTestClasses(JaCoCoClassLoader classLoader,
            String testClassName) {
        List<Class<?>> testClasses = new ArrayList<>();

        Class<?> testClass = classLoader.tryLoadClass(testClassName);

        if (testClass != null) {
            // one class containing the test cases
            testClasses.add(testClass);
        } else {
            // one package containing the test cases

            testClasses.addAll(loadTestClassesForPrefix(classLoader, testClassName, "Test"));

            // randoop
            testClasses
                    .addAll(loadTestClassesForPrefix(classLoader, testClassName, "RegressionTest"));
            testClasses.addAll(loadTestClassesForPrefix(classLoader, testClassName, "ErrorTest"));
        }
        return testClasses;
    }

    private static List<Class<?>> loadTestClassesForPrefix(JaCoCoClassLoader classLoader,
            String packageName, String prefix) {
        List<Class<?>> testClasses = new ArrayList<>();

        String clsBaseName = packageName + "." + prefix;

        // try to load the class for the test suite (it is not used by SETTE)
        classLoader.tryLoadClass(clsBaseName);

        int i = 0;
        while (true) {
            // load the i-th test class (i starts at zero)
            Class<?> testClass = classLoader.tryLoadClass(clsBaseName + i);

            if (testClass != null) {
                testClasses.add(testClass);
            } else {
                // the i-th test class does not exists
                if (classLoader.tryLoadClass(clsBaseName + (i + 1)) != null) {
                    // but the (i+1)-th test class exists -> problem
                    throw new RuntimeException(
                            "i-th does not, but (i+1)-th exists! i=" + i + ": " + testClass);
                } else {
                    // ok, all test classes were found
                    break;
                }
            }

            i++;
        }

        return testClasses;
    }

    static int[] linesToArray(String lines) {
        return Stream.of(lines.split("\\s+")).filter(line -> !StringUtils.isBlank(line))
                .mapToInt(line -> Integer.parseInt(line)).sorted().toArray();
    }
}
