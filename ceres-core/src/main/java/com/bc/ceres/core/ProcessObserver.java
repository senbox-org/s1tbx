package com.bc.ceres.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * An observer that notifies its {@link ProcessObserver.Handler handlers} about lines of characters that have been written
 * by a process to both {@code stdout} and {@code stderr} output streams.
 * <pre>
 * TODO
 *
 * Develop External Process Invocation API (EPIA)
 *
 * - idea: use velocity to generate input files + command-line from current context
 * - address that executables might have different extensions (and paths) on different OS (.exe, .bat, .sh)
 *
 * Process Descriptor
 *
 * - name
 * - description
 *
 * - n input descriptors (descriptor: name + type + description)
 * - n output descriptors (descriptor: name + type + description)
 * - n parameter descriptors (descriptor: name + type + description + attributes)
 * - 1 command-line velocity template
 * - 1 relative working directory template
 * - n environment variables templates
 * - n velocity file templates
 * - n static files
 * - n static archives to unpack
 *
 * Process Invocation
 *
 * - prepare context:
 * set environment variables
 * set inputs, outputs, parameters
 *
 * </pre>
 *
 * @author Norman Fomferra
 */
public class ProcessObserver {

    /**
     * The observation mode.
     */
    public enum Mode {
        /**
         * {@link ProcessObserver#start()} blocks until the process ends.
         */
        BLOCKING,
        /**
         * {@link ProcessObserver#start()} returns immediately.
         */
        NON_BLOCKING,
    }


    private static final String MAIN = "main";
    private static final int STDOUT = 0;
    private static final int STDERR = 1;

    private final Process process;

    private String name;
    private int pollPeriod;
    private ProgressMonitor progressMonitor;
    private Handler handler;
    private Mode mode;
    private ObservedProcessImpl observedProcess;

    /**
     * Constructor.
     *
     * @param process The process to be observed
     */
    public ProcessObserver(final Process process) {
        this.process = process;
        this.name = "process";
        this.pollPeriod = 500;
        this.progressMonitor = new NullProgressMonitor();
        this.handler = new DefaultHandler();
        this.mode = Mode.BLOCKING;
    }

    /**
     * @return A name that represents the process.
     */
    public String getName() {
        return name;
    }

    /**
     * Default is "process".
     *
     * @param name A name that represents the process.
     * @return this
     */
    public ProcessObserver setName(String name) {
        Assert.notNull(name, "name");
        this.name = name;
        return this;
    }

    /**
     * @return A handler.
     */
    public Handler getHandler() {
        return handler;
    }


    /**
     * Default-handler prints to stdout / stderr.
     *
     * @param handler A handler.
     * @return this
     */
    public ProcessObserver setHandler(Handler handler) {
        Assert.notNull(handler, "handler");
        this.handler = handler;
        return this;
    }

    /**
     * @return A progress monitor.
     */
    public ProgressMonitor getProgressMonitor() {
        return progressMonitor;
    }

    /**
     * Default does nothing.
     *
     * @param progressMonitor A progress monitor.
     * @return this
     */
    public ProcessObserver setProgressMonitor(ProgressMonitor progressMonitor) {
        Assert.notNull(progressMonitor, "progressMonitor");
        this.progressMonitor = progressMonitor;
        return this;
    }

    /**
     * @return The observation mode.
     */
    public Mode getMode() {
        return mode;
    }

    /**
     * Default is {@link Mode#BLOCKING}.
     *
     * @param mode The observation mode.
     * @return this
     */
    public ProcessObserver setMode(Mode mode) {
        Assert.notNull(mode, "mode");
        this.mode = mode;
        return this;
    }

    /**
     * @return Time in milliseconds between successive process status queries.
     */
    public int getPollPeriod() {
        return pollPeriod;
    }

    /**
     * Default is 500 milliseconds.
     *
     * @param pollPeriod Time in milliseconds between successive process status queries.
     * @return this
     */
    public ProcessObserver setPollPeriod(int pollPeriod) {
        Assert.notNull(pollPeriod, "pollPeriod");
        this.pollPeriod = pollPeriod;
        return this;
    }

    /**
     * Starts observing the given process.
     */
    public ObservedProcess start() {
        if (observedProcess != null) {
            throw new IllegalStateException("process already observed.");
        }
        observedProcess = new ObservedProcessImpl();
        observedProcess.startObservation();
        return observedProcess;
    }

    /**
     * The observed process.
     */
    public static interface ObservedProcess {
        /**
         * @return The process' name.
         */
        String getName();

        /**
         * Submits a request to cancel an observed process.
         */
        void cancel();
    }

    /**
     * A handler that will be notified during the observation of the process.
     */
    public static interface Handler {
        /**
         * Called if the process is started being observed.
         *
         * @param process The observed process.
         * @param pm      The progress monitor, that is used to monitor the progress of the running process.
         */
        void onObservationStarted(ObservedProcess process, ProgressMonitor pm);

        /**
         * Called if a new text line that has been received from {@code stdout}.
         *
         * @param process The observed process.
         * @param line    The line.
         * @param pm      The progress monitor, that is used to monitor the progress of the running process.
         */
        void onStdoutLineReceived(ObservedProcess process, String line, ProgressMonitor pm);

        /**
         * Called if a new text line that has been received from {@code stderr}.
         *
         * @param process The observed process.
         * @param line    The line.
         * @param pm      The progress monitor, that is used to monitor the progress of the running process.
         */
        void onStderrLineReceived(ObservedProcess process, String line, ProgressMonitor pm);

        /**
         * Called if the process is no longer being observed.
         *
         * @param process  The observed process.
         * @param exitCode The exit code, may be {@code null} if unknown.
         * @param pm       The progress monitor, that is used to monitor the progress of the running process.
         */
        void onObservationEnded(ObservedProcess process, Integer exitCode, ProgressMonitor pm);
    }

    /**
     * Default implementation of {@link Handler}, which simply prints observations to the console.
     */
    public static class DefaultHandler implements Handler {
        @Override
        public void onObservationStarted(ObservedProcess process, ProgressMonitor pm) {
            System.out.println(process.getName() + " started");
        }

        @Override
        public void onStdoutLineReceived(ObservedProcess process, String line, ProgressMonitor pm) {
            System.out.println(process.getName() + ": " + line);
        }

        @Override
        public void onStderrLineReceived(ObservedProcess process, String line, ProgressMonitor pm) {
            System.err.println(process.getName() + ": " + line);
        }

        @Override
        public void onObservationEnded(ObservedProcess process, Integer exitCode, ProgressMonitor pm) {
            System.out.println(process.getName() + " ended, exit code " + exitCode);
        }
    }

    private class ObservedProcessImpl implements ObservedProcess {
        private ThreadGroup threadGroup;
        private Thread stdoutThread;
        private Thread stderrThread;
        private boolean cancellationRequested;
        private boolean cancelled;

        ObservedProcessImpl() {
            this.threadGroup = new ThreadGroup(name);
            this.stdoutThread = new LineReaderThread(threadGroup, STDOUT);
            this.stderrThread = new LineReaderThread(threadGroup, STDERR);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void cancel() {
            cancellationRequested = true;
        }

        private void startObservation() {
            handler.onObservationStarted(observedProcess, progressMonitor);
            stdoutThread.start();
            stderrThread.start();
            if (mode == Mode.BLOCKING) {
                awaitTermination();
            } else /*if (mode == Mode.NON_BLOCKING)*/ {
                Thread mainThread = new Thread(threadGroup,
                                               this::awaitTermination,
                                               name + "-" + MAIN);
                mainThread.start();
            }
        }

        private void awaitTermination() {
            while (true) {

                if ((progressMonitor.isCanceled() || cancellationRequested) && !cancelled) {
                    cancelled = true;
                    // todo - parametrise what is best done now:
                    //        1. just leave, and let the process be unattended
                    //      * 2. destroy the process (current impl.)
                    //        3. throw a checked ProcessObserverException
                    process.destroy();
                    handler.onObservationEnded(this, null, progressMonitor);
                    break;
                }

                if (!stdoutThread.isAlive() && !stderrThread.isAlive()) {
                    try {
                        final int exitCode = process.exitValue();
                        handler.onObservationEnded(this, exitCode, progressMonitor);
                        break;
                    } catch (IllegalThreadStateException e) {
                        // process has not yet terminated, so continue observing
                    }
                }

                try {
                    Thread.sleep(pollPeriod);
                } catch (InterruptedException e) {
                    // todo - parametrise what is best done now:
                    //      * 1. just leave, and let the process be unattended (current impl.)
                    //        2. destroy the process
                    //        3. throw a checked ProcessObserverException
                    handler.onObservationEnded(this, null, progressMonitor);
                    break;
                }
            }
        }
    }

    private class LineReaderThread extends Thread {
        private final int type;

        public LineReaderThread(ThreadGroup threadGroup, int type) {
            super(threadGroup, name + "-" + type);
            this.type = type;
        }

        @Override
        public void run() {
            try {
                read();
            } catch (IOException e) {
                // cannot be handled in a meaningful way, but thread will end anyway
            }
        }

        private void read() throws IOException {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(type == STDOUT ? process.getInputStream() : process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    fireLineReceived(line);
                }
            }
        }

        private void fireLineReceived(String line) {
            if (type == STDOUT) {
                handler.onStdoutLineReceived(observedProcess, line, progressMonitor);
            } else {
                handler.onStderrLineReceived(observedProcess, line, progressMonitor);
            }
        }
    }

}