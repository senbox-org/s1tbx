package org.esa.nest.util;

/**
 * command line Progress monitor
 */
public final class StdOutProgressMonitor {
    private final int minorStep = 2;
    private final int majorStep = 10;
    private int lastMinPercentComplete = minorStep;
    private int lastMajPercentComplete = majorStep;
    private final int max;

    public StdOutProgressMonitor(final int max) {
        this.max = max;
    }

    public void worked(final int tileY) {
        final int percentComplete = (int)((tileY / (float)max) * 100.0f);
        if(percentComplete > lastMinPercentComplete) {
            lastMinPercentComplete = ((percentComplete / minorStep) * minorStep) + minorStep;
            if(percentComplete > lastMajPercentComplete) {
                System.out.print(lastMajPercentComplete + "%");
                lastMajPercentComplete = ((percentComplete / majorStep) * majorStep) + majorStep;
            } else {
                System.out.print(".");
            }
        }
    }

    public void done() {
        System.out.println("100%");
    }

}
