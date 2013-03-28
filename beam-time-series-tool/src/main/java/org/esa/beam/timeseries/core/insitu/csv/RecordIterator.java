package org.esa.beam.timeseries.core.insitu.csv;

import org.esa.beam.timeseries.core.insitu.Record;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An iterator used in various implementations of the {@link org.esa.beam.timeseries.core.insitu.RecordSource} interface.
 *
 * @author Norman
 */
abstract class RecordIterator implements Iterator<Record> {
    private Record next;
    private boolean nextValid;

    RecordIterator() {
    }

    @Override
    public boolean hasNext() {
        ensureValidNext();
        return next != null;
    }

    @Override
    public Record next() {
        ensureValidNext();
        if (next == null) {
            throw new NoSuchElementException();
        }
        nextValid = false;
        return next;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    private void ensureValidNext() {
        if (!nextValid) {
            next = getNextRecord();
            if(next != null) {
                nextValid = true;
            }
        }
    }

    /**
     * @return The next record, or {@code null} if there is no next record.
     */
    protected abstract Record getNextRecord();
}
