package org.esa.snap.remote.execution.utils;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by jcoravu on 8/2/2019.
 */
public class ThreadNamePoolExecutor extends ThreadPoolExecutor {

    public ThreadNamePoolExecutor(String poolName, int maxThreads) {
        super(maxThreads, maxThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue(), new ThreadPoolFactory(poolName));
    }

    private static class ThreadPoolFactory implements ThreadFactory {

        private final AtomicInteger counter;
        private final String poolName;

        private ThreadPoolFactory(String poolName) {
            this.poolName = poolName;
            this.counter = new AtomicInteger();
        }

        @Override
        public Thread newThread(Runnable runnable) {
            String threadName = this.poolName + "-" + Integer.toString(this.counter.incrementAndGet());
            return new Thread(runnable, threadName);
        }
    }
}

