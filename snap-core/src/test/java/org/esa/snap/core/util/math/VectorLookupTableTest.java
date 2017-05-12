/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.core.util.math;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Tests for class {@link VectorLookupTable}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class VectorLookupTableTest extends TestCase {

    public void testArrayInterpolation1D() {
        final double[] dimension = new double[]{0, 1};
        final double[] values = new double[]{0, 2, 1, 3};

        final VectorLookupTable lut = new VectorLookupTable(2, values, dimension);
        assertEquals(1, lut.getDimensionCount());

        assertEquals(0.0, lut.getDimension(0).getMin(), 0.0);
        assertEquals(1.0, lut.getDimension(0).getMax(), 0.0);

        assertArrayEquals(new double[]{0.0, 2.0}, lut.getValues(0.0), 1.0e-6);
        assertArrayEquals(new double[]{1.0, 3.0}, lut.getValues(1.0), 1.0e-6);
        assertArrayEquals(new double[]{0.5, 2.5}, lut.getValues(0.5), 1.0e-6);
    }

    public void testArrayInterpolation2D() {
        final double[][] dimensions = new double[][]{{0, 1}, {0, 1}};
        final double[] values = new double[]{0, 4, 1, 5, 2, 6, 3, 7};

        final VectorLookupTable lut = new VectorLookupTable(2, values, dimensions);
        assertEquals(2, lut.getDimensionCount());

        assertEquals(0.0, lut.getDimension(0).getMin(), 0.0);
        assertEquals(1.0, lut.getDimension(0).getMax(), 0.0);
        assertEquals(0.0, lut.getDimension(1).getMin(), 0.0);
        assertEquals(1.0, lut.getDimension(1).getMax(), 0.0);

        assertArrayEquals(new double[]{0.0, 4.0}, lut.getValues(0.0, 0.0), 1.0e-6);
        assertArrayEquals(new double[]{1.0, 5.0}, lut.getValues(0.0, 1.0), 1.0e-6);
        assertArrayEquals(new double[]{2.0, 6.0}, lut.getValues(1.0, 0.0), 1.0e-6);
        assertArrayEquals(new double[]{3.0, 7.0}, lut.getValues(1.0, 1.0), 1.0e-6);

        assertArrayEquals(new double[]{0.5, 4.5}, lut.getValues(0.0, 0.5), 1.0e-6);
        assertArrayEquals(new double[]{1.5, 5.5}, lut.getValues(0.5, 0.5), 1.0e-6);
        assertArrayEquals(new double[]{2.5, 6.5}, lut.getValues(1.0, 0.5), 1.0e-6);
    }

    public void testArrayInterpolationWithFloatValues() {
        final float[][] dimensions = new float[][]{{0, 1}, {0, 1}};
        final float[] values = new float[]{0, 4, 1, 5, 2, 6, 3, 7};

        final VectorLookupTable lut = new VectorLookupTable(2, values, dimensions);
        assertArrayEquals(new double[]{0.0, 4.0}, lut.getValues(0.0, 0.0), 1.0e-6);
        assertArrayEquals(new double[]{1.0, 5.0}, lut.getValues(0.0, 1.0), 1.0e-6);
        assertArrayEquals(new double[]{2.0, 6.0}, lut.getValues(1.0, 0.0), 1.0e-6);
        assertArrayEquals(new double[]{3.0, 7.0}, lut.getValues(1.0, 1.0), 1.0e-6);

        assertArrayEquals(new double[]{0.5, 4.5}, lut.getValues(0.0, 0.5), 1.0e-6);
        assertArrayEquals(new double[]{1.5, 5.5}, lut.getValues(0.5, 0.5), 1.0e-6);
        assertArrayEquals(new double[]{2.5, 6.5}, lut.getValues(1.0, 0.5), 1.0e-6);
    }

    public void testThreadSafety() throws InterruptedException {
        final float[][] dimensions = new float[][]{{0, 1}, {0, 1}};
        final float[] values = new float[]{0, 4, 1, 5, 2, 6, 3, 7};

        final VectorLookupTable lut = new VectorLookupTable(2, values, dimensions);
        ArrayList<Runnable> runnables = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            runnables.add(new TestTask(lut));
        }

        assertConcurrent("VectorLookupTableTest", runnables, 3);
    }

    // method taken from https://github.com/junit-team/junit4/wiki/Multithreaded-code-and-concurrency
    private static void assertConcurrent(final String message, final List<? extends Runnable> runnables, final int maxTimeoutSeconds) throws InterruptedException {
        final int numThreads = runnables.size();
        final List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<Throwable>());
        final ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        try {
            final CountDownLatch allExecutorThreadsReady = new CountDownLatch(numThreads);
            final CountDownLatch afterInitBlocker = new CountDownLatch(1);
            final CountDownLatch allDone = new CountDownLatch(numThreads);
            for (final Runnable submittedTestRunnable : runnables) {
                threadPool.submit(() -> {
                    allExecutorThreadsReady.countDown();
                    try {
                        afterInitBlocker.await();
                        submittedTestRunnable.run();
                    } catch (final Throwable e) {
                        exceptions.add(e);
                    } finally {
                        allDone.countDown();
                    }
                });
            }
            // wait until all threads are ready
            assertTrue("Timeout initializing threads! Perform long lasting initializations before passing runnables to assertConcurrent", allExecutorThreadsReady.await(runnables.size() * 10, TimeUnit.MILLISECONDS));
            // start all test runners
            afterInitBlocker.countDown();
            assertTrue(message + " timeout! More than" + maxTimeoutSeconds + "seconds", allDone.await(maxTimeoutSeconds, TimeUnit.SECONDS));
        } finally {
            threadPool.shutdownNow();
        }
        assertTrue(message + "failed with exception(s)" + exceptions, exceptions.isEmpty());
    }

    private static class TestTask implements Runnable {

        private final VectorLookupTable lut;

        TestTask(VectorLookupTable lut) {
            this.lut = lut;
        }

        @Override
        public void run() {
            assertArrayEquals(new double[]{0.0, 4.0}, lut.getValues(0.0, 0.0), 1.0e-6);
            assertArrayEquals(new double[]{1.0, 5.0}, lut.getValues(0.0, 1.0), 1.0e-6);
            assertArrayEquals(new double[]{2.0, 6.0}, lut.getValues(1.0, 0.0), 1.0e-6);
            assertArrayEquals(new double[]{3.0, 7.0}, lut.getValues(1.0, 1.0), 1.0e-6);

            assertArrayEquals(new double[]{0.5, 4.5}, lut.getValues(0.0, 0.5), 1.0e-6);
            assertArrayEquals(new double[]{1.5, 5.5}, lut.getValues(0.5, 0.5), 1.0e-6);
            assertArrayEquals(new double[]{2.5, 6.5}, lut.getValues(1.0, 0.5), 1.0e-6);
        }
    }

}
