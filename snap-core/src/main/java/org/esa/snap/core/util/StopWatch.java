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


/**
 * The <code>StopWatch</code> class is a (very) simple utility class that allows to measure the time passed between two
 * user defined events.
 * <p>
 * Here is an example:
 * <pre>
 *      ...
 *      private final StopWatch stopWatch = new StopWatch();
 *      ...
 *      void doATimeConsumingThing() {
 *          stopWatch.start();
 *          ...
 *          stopWatch.end();
 *          Debug.trace("Time needed: " + stopWatch);
 *      }
 *      ...
 * </pre>
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class StopWatch {

    /**
     * Milliseconds per second
     */
    static final int MILLIS_PER_SECOND = 1000;
    /**
     * Milliseconds per minute
     */
    static final int MILLIS_PER_MINUTE = 60 * MILLIS_PER_SECOND;
    /**
     * Milliseconds per hour
     */
    static final int MILLIS_PER_HOUR = 60 * MILLIS_PER_MINUTE;

    /**
     * The user defined start time.
     */
    private long _startTime;

    /**
     * The user defined end time.
     */
    private long _endTime;

    /**
     * Construct a new stop watch object. The constructor simply calls <code>start()</code>.
     */
    public StopWatch() {
        start();
    }

    /**
     * Defines the start time by calling <code>java.lang.System.currentTimeMillis()</code>.
     */
    public void start() {
        _startTime =
        _endTime = System.currentTimeMillis();
    }

    /**
     * Defines the end time by calling <code>java.lang.System.currentTimeMillis()</code>.
     */
    public void stop() {
        _endTime = System.currentTimeMillis();
    }

    /**
     * Gets the user defined start time in milliseconds (the one that was set when the <code>start()</code> method was
     * called).
     */
    public long getStartTime() {
        return _startTime;
    }

    /**
     * Gets the user defined end time in milliseconds (the one that was set when the <code>stop()</code> method was
     * called).
     */
    public long getEndTime() {
        return _endTime;
    }

    /**
     * Gets the elapsed time between start time and end time.
     */
    public long getTimeDiff() {
        return _endTime - _startTime;
    }

    /**
     * Returns the elapsed time between start and end as a string in the format "<i>hours</i>:<i>minutes</i>:<i>seconds</i>",
     * with <i>seconds</i> given as a decimal floating point number.
     *
     * @return the elapsed time as a string
     */
    public String getTimeDiffString() {
        return getTimeString(getTimeDiff());
    }

    /**
     * Utility method that converts a given time in milliseconds in the format "<i>hours</i>:<i>minutes</i>:<i>seconds</i>",
     * with <i>seconds</i> given as a decimal floating point number.
     *
     * @param millis the time in milliseconds
     *
     * @return the time as a string
     */
    public static String getTimeString(long millis) {
        long hours = millis / MILLIS_PER_HOUR;
        millis -= hours * MILLIS_PER_HOUR;
        long minutes = millis / MILLIS_PER_MINUTE;
        millis -= minutes * MILLIS_PER_MINUTE;
        long seconds = millis / MILLIS_PER_SECOND;
        millis -= seconds * MILLIS_PER_SECOND;

        StringBuffer sb = new StringBuffer();
        if (hours < 10) {
            sb.append('0');
        }
        sb.append(hours);
        sb.append(':');
        if (minutes < 10) {
            sb.append('0');
        }
        sb.append(minutes);
        sb.append(':');
        if (seconds < 10) {
            sb.append('0');
        }
        sb.append(seconds);
        sb.append('.');
        if (millis < 100) {
            sb.append('0');
        }
        if (millis < 10) {
            sb.append('0');
        }
        sb.append(millis);
        return sb.toString();
    }

    /**
     * Return a string representation of the elapsed time by calling <code>getTimeDiffString()</code>.
     */
    @Override
    public String toString() {
        return getTimeDiffString();
    }

    public void stopAndTrace(String label) {
        stop();
        trace(label);
    }

    public void trace(String label) {
        if (Debug.isEnabled()) {
            Debug.trace(label + ": " + getTimeDiffString());
        }
    }
}
