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
import java.util.logging.Handler;
import java.util.logging.LogRecord;


/**
 * This handler collects all exceptions logged by the system logger.
 * It can be added to the {@link java.util.logging.Logger} by using the <code>addhandler(Handler)</code> method.
 *
 * @author Marco Peters
 * @author Andrea Sabine Embacher
 */
public class ExceptionCollector extends Handler {

    private final ArrayList errors = new ArrayList();

    @Override
    public void close() throws SecurityException {
    }

    @Override
    public void flush() {
    }

    @Override
    public void publish(LogRecord record) {
        final Throwable thrown = record.getThrown();
        if (thrown != null) {
            if(thrown instanceof Exception) {
                errors.add(record);
            }
        }
    }

    /**
     * Gets all logged Exceptions and clears the storage.
     *
     * @return Array of {@link LogRecord}s.
     */
    public LogRecord[] getExceptions() {
        return (LogRecord[]) errors.toArray(new LogRecord[errors.size()]);
    }
}
