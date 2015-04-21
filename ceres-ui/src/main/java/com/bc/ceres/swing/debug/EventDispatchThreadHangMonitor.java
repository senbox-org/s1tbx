/*
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package com.bc.ceres.swing.debug;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Monitors the AWT event dispatch thread for events that take longer than
 * a certain time to be dispatched.
 * <p>
 * The principle is to record the time at which we start processing an event,
 * and have another thread check frequently to see if we're still processing.
 * If the other thread notices that we've been processing a single event for
 * too long, it prints a stack trace showing what the event dispatch thread
 * is doing, and continues to time it until it finally finishes.
 * <p>
 * This is useful in determining what code is causing your Java application's
 * GUI to be unresponsive.
 * 
 * <p>The original blog can be found here<br>  
 * <a href="http://elliotth.blogspot.com/2005/05/automatically-detecting-awt-event.html">
 * Automatically detecting AWT event dispatch thread hangs</a>
 *
 *
 * @author Elliott Hughes &lt;enh@jessies.org&gt;
 * 
 * Advice, bug fixes, and test cases from
 * Alexander Potochkin and Oleg Sukhodolsky.
 * 
 * https://swinghelper.dev.java.net/
 */
public final class EventDispatchThreadHangMonitor extends EventQueue {
    private static final EventDispatchThreadHangMonitor INSTANCE = new EventDispatchThreadHangMonitor();

    // Time to wait between checks that the event dispatch thread isn't hung.
    private static final long CHECK_INTERVAL_MS = 100;

    // Maximum time we won't warn about. This used to be 500 ms, but 1.5 on
    // late-2004 hardware isn't really up to it; there are too many parts of
    // the JDK that can go away for that long (often code that has to be
    // called on the event dispatch thread, like font loading).
    private static final long UNREASONABLE_DISPATCH_DURATION_MS = 1000;

    // Help distinguish multiple hangs in the log, and match start and end too.
    // Only access this via getNewHangNumber.
    private static int hangCount = 0;

    // Prevents us complaining about hangs during start-up, which are probably
    // the JVM vendor's fault.
    private boolean haveShownSomeComponent = false;

    // The currently outstanding event dispatches. The implementation of
    // modal dialogs is a common cause for multiple outstanding dispatches.
    private final LinkedList<DispatchInfo> dispatches = new LinkedList<>();

    private static class DispatchInfo {
        // The last-dumped hung stack trace for this dispatch.
        private StackTraceElement[] lastReportedStack;
        // If so; what was the identifying hang number?
        private int hangNumber;

        // The EDT for this dispatch (for the purpose of getting stack traces).
        // I don't know of any API for getting the event dispatch thread,
        // but we can assume that it's the current thread if we're in the
        // middle of dispatching an AWT event...
        // We can't cache this because the EDT can die and be replaced by a
        // new EDT if there's an uncaught exception.
        private final Thread eventDispatchThread = Thread.currentThread();

        // The last time in milliseconds at which we saw a dispatch on the above thread.
        private long lastDispatchTimeMillis = System.currentTimeMillis();

        public DispatchInfo() {
            // All initialization is done by the field initializers.
        }

        public void checkForHang() {
            if (timeSoFar() > UNREASONABLE_DISPATCH_DURATION_MS) {
                examineHang();
            }
        }

        // We can't use StackTraceElement.equals because that insists on checking the filename and line number.
        // That would be version-specific.
        private static boolean stackTraceElementIs(StackTraceElement e, String className, String methodName, boolean isNative) {
            return e.getClassName().equals(className) && e.getMethodName().equals(methodName) && e.isNativeMethod() == isNative;
        }

        // Checks whether the given stack looks like it's waiting for another event.
        // This relies on JDK implementation details.
        private boolean isWaitingForNextEvent(StackTraceElement[] currentStack) {
            return stackTraceElementIs(currentStack[0], "java.lang.Object", "wait", true) && stackTraceElementIs(currentStack[1], "java.lang.Object", "wait", false) && stackTraceElementIs(currentStack[2], "java.awt.EventQueue", "getNextEvent", false);
        }

        private void examineHang() {
            StackTraceElement[] currentStack = eventDispatchThread.getStackTrace();

            if (isWaitingForNextEvent(currentStack)) {
                // Don't be fooled by a modal dialog if it's waiting for its next event.
                // As long as the modal dialog's event pump doesn't get stuck, it's okay for the outer pump to be suspended.
                return;
            }

            if (stacksEqual(lastReportedStack, currentStack)) {
                // Don't keep reporting the same hang every time the timer goes off.
                return;
            }

            hangNumber = getNewHangNumber();
            String stackTrace = stackTraceToString(currentStack);
            lastReportedStack = currentStack;
            Log.warn("(hang #" + hangNumber + ") event dispatch thread stuck processing event for " + timeSoFar() + " ms:" + stackTrace);
            checkForDeadlock();
        }

        private static boolean stacksEqual(StackTraceElement[] a, StackTraceElement[] b) {
            if (a == null) {
                return false;
            }
            if (a.length != b.length) {
                return false;
            }
            for (int i = 0; i < a.length; ++i) {
                if (!a[i].equals(b[i])) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Returns how long this dispatch has been going on (in milliseconds).
         */
        private long timeSoFar() {
            return (System.currentTimeMillis() - lastDispatchTimeMillis);
        }

        public void dispose() {
            if (lastReportedStack != null) {
                Log.warn("(hang #" + hangNumber + ") event dispatch thread unstuck after " + timeSoFar() + " ms.");
            }
        }
    }

    private EventDispatchThreadHangMonitor() {
        initTimer();
    }

    /**
     * Sets up a timer to check for hangs frequently.
     */
    private void initTimer() {
        final long initialDelayMs = 0;
        final boolean isDaemon = true;
        Timer timer = new Timer("EventDispatchThreadHangMonitor", isDaemon);
        timer.schedule(new HangChecker(), initialDelayMs, CHECK_INTERVAL_MS);
    }

    private class HangChecker extends TimerTask {
        @Override
        public void run() {
            synchronized (dispatches) {
                if (dispatches.isEmpty() || !haveShownSomeComponent) {
                    // Nothing to do.
                    // We don't destroy the timer when there's nothing happening
                    // because it would mean a lot more work on every single AWT
                    // event that gets dispatched.
                    return;
                }
                // Only the most recent dispatch can be hung; nested dispatches
                // by their nature cause the outer dispatch pump to be suspended.
                dispatches.getLast().checkForHang();
            }
        }
    }

    /**
     * Sets up hang detection for the event dispatch thread.
     */
    public static void initMonitoring() {
        Toolkit.getDefaultToolkit().getSystemEventQueue().push(INSTANCE);
    }

    /**
     * Overrides EventQueue.dispatchEvent to call our pre and post hooks either
     * side of the system's event dispatch code.
     */
    @Override
    protected void dispatchEvent(AWTEvent event) {
        try {
            preDispatchEvent();
            super.dispatchEvent(event);
        } finally {
            postDispatchEvent();
            if (!haveShownSomeComponent && 
                    event instanceof WindowEvent && event.getID() == WindowEvent.WINDOW_OPENED) {
                haveShownSomeComponent = true;
            }
        }
    }

    /**
     * Starts tracking a dispatch.
     */
    private synchronized void preDispatchEvent() {
        synchronized (dispatches) {
            dispatches.addLast(new DispatchInfo());
        }
    }

    /**
     * Stops tracking a dispatch.
     */
    private synchronized void postDispatchEvent() {
        synchronized (dispatches) {
            // We've finished the most nested dispatch, and don't need it any longer.
            DispatchInfo justFinishedDispatch = dispatches.removeLast();
            justFinishedDispatch.dispose();

            // The other dispatches, which have been waiting, need to be credited extra time.
            // We do this rather simplistically by pretending they've just been redispatched.
            Thread currentEventDispatchThread = Thread.currentThread();
            for (DispatchInfo dispatchInfo : dispatches) {
                if (dispatchInfo.eventDispatchThread == currentEventDispatchThread) {
                    dispatchInfo.lastDispatchTimeMillis = System.currentTimeMillis();
                }
            }
        }
    }

    private static void checkForDeadlock() {
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        long[] threadIds = threadBean.findMonitorDeadlockedThreads();
        if (threadIds == null) {
            return;
        }
        Log.warn("deadlock detected involving the following threads:");
        ThreadInfo[] threadInfos = threadBean.getThreadInfo(threadIds, Integer.MAX_VALUE);
        for (ThreadInfo info : threadInfos) {
            Log.warn("Thread #" + info.getThreadId() + " " + info.getThreadName() + 
                    " (" + info.getThreadState() + ") waiting on " + info.getLockName() + 
                    " held by " + info.getLockOwnerName() + stackTraceToString(info.getStackTrace()));
        }
    }

    private static String stackTraceToString(StackTraceElement[] stackTrace) {
        StringBuilder result = new StringBuilder();
        // We used to avoid showing any code above where this class gets
        // involved in event dispatch, but that hides potentially useful
        // information when dealing with modal dialogs. Maybe we should
        // reinstate that, but search from the other end of the stack?
        for (StackTraceElement stackTraceElement : stackTrace) {
            String indentation = "    ";
            result.append("\n").append(indentation).append(stackTraceElement);
        }
        return result.toString();
    }

    private synchronized static int getNewHangNumber() {
        return ++hangCount;
    }

    public static void main(String[] args) {
        initMonitoring();
        //special case for deadlock test
        if (args.length > 0 && "deadlock".equals(args[0])) {
            EventDispatchThreadHangMonitor.INSTANCE.haveShownSomeComponent = true;
            Tests.runDeadlockTest();
            return;
        }
        Tests.main(args);
    }

    private static class Tests {
        public static void main(final String[] args) {

            java.awt.EventQueue.invokeLater(new Runnable() {
                public void run() {
                    for (String arg : args) {
                        final JFrame frame = new JFrame();
                        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                        frame.setLocationRelativeTo(null);
                        if (arg.equals("exception")) {
                            runExceptionTest(frame);
                        } else if (arg.equals("focus")) {
                            runFocusTest(frame);
                        } else if (arg.equals("modal-hang")) {
                            runModalTest(frame, true);
                        } else if (arg.equals("modal-no-hang")) {
                            runModalTest(frame, false);
                        } else {
                            System.err.println("unknown regression test '" + arg + "'");
                            System.exit(1);
                        }
                        frame.pack();
                        frame.setVisible(true);
                    }
                }
            });
        }

        private static void runDeadlockTest() {
            class Locker {
                private Locker locker;

                public void setLocker(Locker locker) {
                    this.locker = locker;
                }

                public synchronized void tryToDeadlock() {
                    locker.toString();
                }

                public synchronized String toString() {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return super.toString();
                }
            }
            final Locker one = new Locker();
            final Locker two = new Locker();
            one.setLocker(two);
            two.setLocker(one);

            //Deadlock expected here:
            for (int i = 0; i < 100; i++) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        one.tryToDeadlock();
                    }
                });
                two.tryToDeadlock();
            }
        }

        // If we don't do our post-dispatch activity in a finally block, we'll
        // report bogus hangs.
        private static void runExceptionTest(final JFrame frame) {
            JButton button = new JButton("Throw Exception");
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    // This shouldn't cause us to report a hang.
                    throw new RuntimeException("Nobody expects the Spanish Inquisition!");
                }
            });
            frame.add(button);
        }

        // A demonstration of nested calls to dispatchEvent caused by SequencedEvent.
        private static void runFocusTest(final JFrame frame) {
            final JDialog dialog = new JDialog(frame, "Non-Modal Dialog");
            dialog.add(new JLabel("Close me!"));
            dialog.pack();
            dialog.setLocationRelativeTo(frame);
            dialog.addWindowFocusListener(new WindowAdapter() {
                public void windowGainedFocus(WindowEvent e) {
                    System.out.println("FocusTest.windowGainedFocus");
                    // If you don't cope with nested calls to dispatchEvent, you won't detect this.
                    // See java.awt.SequencedEvent for an example.
                    sleep(2500);
                }
            });
            JButton button = new JButton("Show Non-Modal Dialog");
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    dialog.setVisible(true);
                }
            });
            frame.add(button);
        }

        // A demonstration of the problems of dealing with modal dialogs.
        private static void runModalTest(final JFrame frame, final boolean shouldSleep) {
            System.out.println(shouldSleep ? "Expect hangs!" : "There should be no hangs...");
            JButton button = new JButton("Show Modal Dialog");
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (shouldSleep) {
                        sleep(2500); // This is easy.
                    }
                    JDialog dialog = new JDialog(frame, "Modal dialog", true);
                    dialog.setLayout(new FlowLayout());
                    dialog.add(new JLabel("Close this dialog!"));
                    final JLabel label = new JLabel(" ");
                    dialog.add(label);
                    dialog.pack();
                    dialog.setLocation(frame.getX() - 100, frame.getY());

                    // Make sure the new event pump has some work to do, each unit of which is insufficient to cause a hang.
                    new Thread(new Runnable() {
                        public void run() {
                            for (int i = 0; i <= 100000; ++i) {
                                final int value = i;
                                EventQueue.invokeLater(new Runnable() {
                                    public void run() {
                                        label.setText(Integer.toString(value));
                                    }
                                });
                            }
                        }
                    }).start();

                    dialog.setVisible(true);

                    if (shouldSleep) {
                        sleep(2500); // If you don't distinguish different stack traces, you won't report this.
                    }
                }
            });
            frame.add(button);
        }

        private static void sleep(long ms) {
            try {
                System.out.println("Sleeping for " + ms + " ms on " + Thread.currentThread() + "...");
                Thread.sleep(ms);
                System.out.println("Finished sleeping...");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private static class Log {
        public static void warn(String str) {
            System.out.println(str);
        }
    }
}