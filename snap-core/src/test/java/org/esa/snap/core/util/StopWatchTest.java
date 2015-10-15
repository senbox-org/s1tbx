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

package org.esa.snap.core.util;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class StopWatchTest extends TestCase {

    public StopWatchTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(StopWatchTest.class);
    }

    /**
     * Tests the constructor functionality
     */
    public void testStopWatch() {
        // check whether the timer really starts at construction time
/*
        StopWatch   watch = new StopWatch();

        try {
            Thread.sleep(10);
        }
        catch(InterruptedException e) {
        }

        watch.stop();
        assertTrue(watch.getStartTime() != watch.getEndTime());
*/
    }

    /**
     * Tests the functionality for getEndTime
     */
    public void testGetEndTime() {
        StopWatch watch = new StopWatch();
        long endTime;

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
        }

        // check if stop time is different from start
        watch.stop();
        endTime = watch.getEndTime();
        assertTrue(0 != endTime);

        // check if end time now is the same (we haven't started the timer agaiin
        assertEquals(endTime, watch.getEndTime());
    }

    /**
     * Tests the functionality for getStartTime()
     */
    public void testGetStartTime() {
/*
        StopWatch   watch = new StopWatch();
        long        startTime;

        // start watch, get the start time, let it run, get start time again
        watch.start();
        startTime = watch.getStartTime();
        try {
            Thread.sleep(10);
        }
        catch(InterruptedException e) {
        }
        assertEquals(startTime, watch.getStartTime());

        // now stop watch and check again
        watch.stop();
        assertEquals(startTime, watch.getStartTime());

        // now start watch again - must be different
        watch.start();
        assertTrue(startTime != watch.getStartTime());
*/
    }

    /**
     * Tests the functionality of getTimeDiff
     */
    public void testGetTimeDiff() {
        StopWatch watch = new StopWatch();
        long startTime;
        long endTime;

        startTime = watch.getStartTime();
        watch.stop();
        endTime = watch.getEndTime();
        assertEquals(endTime - startTime, watch.getTimeDiff());
    }

    /**
     * Tests the functionality of getTimeDiffString()
     */
    public void testGetTimeDiffString() {
        StopWatch watch = new StopWatch();

        watch.stop();
        // just check that we don't get an empty string
        assertTrue("" != watch.getTimeDiffString());
    }

    /**
     * Tests the functionality of getTimeString
     */
    public void testGetTimeString() {
        // just test that we don't get an empty string
        assertTrue("" != StopWatch.getTimeString(12));
    }

    /**
     * Tests the functionality of start
     */
    public void testStart() {
/*
        StopWatch   watch = new StopWatch();
        long        startTime = watch.getStartTime();

        startTime = watch.getStartTime();
        try {
            Thread.sleep(10);
        }
        catch(InterruptedException e) {
        }
        watch.stop();
        watch.start();
        assertTrue(startTime != watch.getStartTime());
*/
    }

    /**
     * tests the functionality of stop
     */
    public void testStop() {
/*
        StopWatch   watch = new StopWatch();
        long        endTime;

        watch.stop();
        endTime = watch.getEndTime();

        watch.start();
        try {
            Thread.sleep(10);
        }
        catch(InterruptedException e) {
        }
        watch.stop();
        assertTrue(endTime != watch.getEndTime());
*/
    }

    /**
     * Tests the functionality of toString()
     */
    public void testToString() {
        StopWatch watch = new StopWatch();

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
        }
        watch.stop();
        assertEquals(watch.getTimeDiffString(), watch.toString());
    }
}
