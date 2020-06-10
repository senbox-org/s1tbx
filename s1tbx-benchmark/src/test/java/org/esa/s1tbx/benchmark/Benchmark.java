package org.esa.s1tbx.benchmark;

import org.esa.snap.core.util.StopWatch;
import org.esa.snap.core.util.SystemUtils;

public abstract class Benchmark {

    private int iterations = 10;

    public void run() throws Exception {
        SystemUtils.LOG.info("Initial cold start run");
        StopWatch coldStartTimer = new StopWatch();
        this.execute();
        coldStartTimer.stop();
        SystemUtils.LOG.info("Cold start time " + coldStartTimer.getTimeDiffString());
        long totalTime = 0L;

        for(int i = 1; i <= iterations; ++i) {
            SystemUtils.LOG.info("Run " + i + " of " + iterations);
            StopWatch timer = new StopWatch();
            this.execute();
            timer.stop();
            totalTime += timer.getTimeDiff();
            SystemUtils.LOG.info("Run " + i + " of " + iterations + " time " + timer.getTimeDiffString());
        }

        SystemUtils.LOG.warning("Average time " + StopWatch.getTimeString(totalTime / (long)iterations));
    }

    protected abstract void execute() throws Exception;
}
