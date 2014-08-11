package org.esa.beam.framework.gpf.internal;

/**
 * command line Progress monitor
 */
public final class StdOutProgressMonitor {
    private final int minorStep = 2;
    private final int majorStep = 10;
    private int lastMinPercentComplete = minorStep;
    private int lastMajPercentComplete = majorStep;
    private final int max;
    private boolean percentWritten = false;

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
                percentWritten = true;
            } else {
                System.out.print(".");
            }
        }
    }

    public void done() {
        if(percentWritten) {
            System.out.println("100%");
        }
    }

}
