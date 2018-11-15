package org.esa.s1tbx.commons.utils;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadExecutor {

    private final ThreadPoolExecutor executor;

    public ThreadExecutor() {
        this(Runtime.getRuntime().availableProcessors());
    }

    public ThreadExecutor(final int maxThreads) {
        this.executor = new ThreadPoolExecutor(maxThreads, maxThreads, 300, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>());
    }

    public void execute(final Runnable runnable) {
        executor.execute(runnable);
    }

    public void complete() {
        executor.shutdown();
        while (!executor.isTerminated()) {
        }
    }
}
