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


package org.esa.beam.util.logging;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import org.esa.beam.util.Guardian;


/**
 * This class is a special message handle that caches all logging messages in memory for later use. It is needed in most
 * beam alpplications, when the logging starts before the user defined logging files are selected.
 */
public class CacheHandler extends Handler {

    private List _cache;

    /**
     * Constructs the object with default parameter.
     */
    public CacheHandler() {
        _cache = new ArrayList();
    }

    /**
     * Publish a <tt>LogRecord</tt>.
     * <p/>
     * The logging request was made initially to a <tt>Logger</tt> object, which initialized the <tt>LogRecord</tt> and
     * forwarded it here.
     * <p/>
     * The <tt>Handler</tt>  is responsible for formatting the message, when and if necessary.  The formatting should
     * include localization.
     *
     * @param record description of the logging event
     */
    @Override
    public void publish(LogRecord record) {
        _cache.add(record);
    }

    /**
     * Removes all records from the vector.
     */
    @Override
    public void flush() {
        _cache.clear();
    }

    /**
     * Close the <tt>Handler</tt> and free all associated resources.
     * <p/>
     * The close method will perform a <tt>flush</tt> and then close the <tt>Handler</tt>.   After close has been called
     * this <tt>Handler</tt> should no longer be used.  Method calls may either be silently ignored or may throw runtime
     * exceptions.
     *
     * @throws java.lang.SecurityException if a security manager exists and if the caller does not have
     *                                     <tt>LoggingPermission("control")</tt>.
     */
    @Override
    public void close() throws SecurityException {
    }

    /**
     * Transfers all logging records chached to the handler passed in.
     *
     * @param target the target handler to publish the cache content
     */
    public void transferRecords(Handler target) {
        Guardian.assertNotNull("target", target);

        for (int n = 0; n < _cache.size(); n++) {
            target.publish((LogRecord) _cache.get(n));
        }
    }
}
