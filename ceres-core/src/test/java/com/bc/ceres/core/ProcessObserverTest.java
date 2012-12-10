package com.bc.ceres.core;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Norman Fomferra
 */
public class ProcessObserverTest {
    public static final String CP = "target/test-classes";

    @Test
    public void testJavaProcessOk() throws Exception {
        final String commandLine = String.format("java -cp %s %s 2 10", CP, TestExecutable.class.getName());
        final Process process = Runtime.getRuntime().exec(commandLine);
        final MyHandler handler = new MyHandler();
        new ProcessObserver(process).setHandler(handler).start();
        assertTrue(handler.started);
        assertEquals("Start\n" +
                             "Progress 10%\n" +
                             "Progress 20%\n" +
                             "Progress 30%\n" +
                             "Progress 40%\n" +
                             "Progress 50%\n" +
                             "Progress 60%\n" +
                             "Progress 70%\n" +
                             "Progress 80%\n" +
                             "Progress 90%\n" +
                             "Progress 100%\n" +
                             "Done\n", handler.out);
        assertEquals("", handler.err);
        assertTrue(handler.ended);
        assertEquals(0, handler.exitCode);
    }

    @Test
    public void testJavaProcessMissingArg() throws Exception {
        final String commandLine = String.format("java -cp %s %s 2", CP, TestExecutable.class.getName());
        final Process process = Runtime.getRuntime().exec(commandLine);
        final MyHandler handler = new MyHandler();
        new ProcessObserver(process).setHandler(handler).start();
        assertTrue(handler.started);
        assertEquals("", handler.out);
        assertEquals("Usage: TestExecutable <seconds> <steps>\n", handler.err);
        assertTrue(handler.ended);
        assertEquals(1, handler.exitCode);
    }

    @Test
    public void testJavaProcessCancel() throws Exception {
        final String commandLine = String.format("java -cp %s %s 10 2", CP, TestExecutable.class.getName());
        final Process process = Runtime.getRuntime().exec(commandLine);
        final MyHandler handler = new MyHandler();
        final ProcessObserver.ObservedProcess observedProcess = new ProcessObserver(process)
                .setMode(ProcessObserver.Mode.NON_BLOCKING)
                .setHandler(handler).start();
        Thread.sleep(250);
        observedProcess.cancel();

        assertTrue(handler.started);
        assertEquals("Start\n", handler.out);
        assertEquals("", handler.err);
        assertFalse(handler.ended);
        assertEquals(0, handler.exitCode);
    }

    private static class MyHandler implements ProcessObserver.Handler {
        boolean started;
        String out = "";
        String err = "";
        boolean ended;
        int exitCode;

        @Override
        public void onObservationStarted(ProcessObserver.ObservedProcess process, ProgressMonitor pm) {
            started = true;
        }

        @Override
        public void onStdoutLineReceived(ProcessObserver.ObservedProcess process, String line, ProgressMonitor pm) {
            out += line + "\n";
        }

        @Override
        public void onStderrLineReceived(ProcessObserver.ObservedProcess process, String line, ProgressMonitor pm) {
            err += line + "\n";
        }

        @Override
        public void onObservationEnded(ProcessObserver.ObservedProcess process, Integer exitCode, ProgressMonitor pm) {
            ended = true;
            this.exitCode = exitCode;
        }
    }
}
