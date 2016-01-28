/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.engine_utilities.gpf;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages how many threads are working concurrently
 */
public class ThreadManager {

    private int numConsecutiveThreads = Runtime.getRuntime().availableProcessors();
    private final List<Thread> threadList = new ArrayList<>(numConsecutiveThreads);

    public ThreadManager() {
    }

    public void add(final Thread worker) throws InterruptedException {
        threadList.add(worker);
        worker.start();

        if (threadList.size() >= numConsecutiveThreads) {
            for (Thread t : threadList) {
                t.join();
            }
            threadList.clear();
        }
    }

    public void finish() throws InterruptedException {
        if (!threadList.isEmpty()) {
            for (Thread t : threadList) {
                t.join();
            }
        }
    }

    public int getNumConsecutiveThreads() {
        return numConsecutiveThreads;
    }

    public void setNumConsecutiveThreads(int numThreads) {
        numConsecutiveThreads = numThreads;
    }
}
