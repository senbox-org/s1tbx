package com.bc.ceres.core;

import org.junit.Ignore;

/**
 * An executable used by {@link ProcessObserverTest}.
 * The executable has two arguments:
 * <ol>
 * <li>&lt;seconds&gt; = the overall process duration</li>
 * <li>&lt;steps&gt; = the number of single steps.</li>
 * </ol>
 * After each step,a progress indicating line is written to stdout.
 * <p>
 * The executable has 3 exit states:
 * <ul>
 * <li>0=Normal termintation,output only on stdout</li>
 * <li>1= #arguments!=2,output to stderr</li>
 * <li>2=InterruptedException,output to stderr</li>
 * </ul>
 *
 * @author Norman Fomferra
 */
@Ignore
public class TestExecutable {
    /**
     * Runs the test executable.
     *
     * @param args Array of two arguments: &lt;seconds&gt; = the overall process duration, &lt;steps&gt; = the number of single steps
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: TestExecutable <seconds> <steps>");
            System.exit(1);
        }
        int seconds = Integer.parseInt(args[0]);
        int steps = Integer.parseInt(args[1]);
        System.out.println("Start");
        long period = (seconds * 1000L) / steps;
        for (int i = 0; i < steps; i++) {
            try {
                Thread.sleep(period);
            } catch (InterruptedException e) {
                System.err.println("Process interrupted");
                System.exit(2);
            }
            System.out.println("Progress " + ((i + 1) * 100) / steps + "%");
        }
        System.out.println("Done");
        System.out.flush();
    }
}
