package org.esa.beam.util.logging;

import java.util.ArrayList;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * Created by IntelliJ IDEA.
 * User: marco
 * Date: 25.04.2005
 * Time: 15:57:11
 */

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
