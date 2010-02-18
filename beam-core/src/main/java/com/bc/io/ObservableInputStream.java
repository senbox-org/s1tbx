/*
 * $Id: ObservableInputStream.java,v 1.1.1.1 2006/09/11 08:16:43 norman Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package com.bc.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;


/**
 * An input stream which can be used to observer progress of reading from some other input stream.
 *
 * @author Norman Fomferra
 * @version
 */
public class ObservableInputStream extends FilterInputStream {
    private InputStreamObserver observer;
    private long numBytesRead;
    private long numBytesTotal;

    /**
     * Constructs an object to observer the progress of an input stream.
     *
     * @param in The input stream to be monitored.
     */
    public ObservableInputStream(InputStream in, InputStreamObserver listener) {
        super(in);
        try {
            numBytesTotal = in.available();
        } catch (IOException ioe) {
        }
        this.observer = listener;
    }

    /**
     * Constructs an object to observer the progress of an input stream.
     *
     * @param in The input stream to be monitored.
     */
    public ObservableInputStream(InputStream in, long size, InputStreamObserver listener) {
        super(in);
        this.numBytesTotal = size;
        this.observer = listener;
    }

    public long getNumBytesTotal() {
        return numBytesTotal;
    }

    public long getNumBytesRead() {
        return numBytesRead;
    }


    public InputStreamObserver getObserver() {
        return observer;
    }

    /**
     * Overrides <code>FilterInputStream.read</code>
     * to update the progress observer after the read.
     */
    @Override
    public int read() throws IOException {
        int c = in.read();
        if (c >= 0) {
            fireProgress(1);
        }
        return c;
    }


    /**
     * Overrides <code>FilterInputStream.read</code>
     * to update the progress observer after the read.
     */
    @Override
    public int read(byte b[]) throws IOException {
        int nr = in.read(b);
        if (nr > 0) {
            fireProgress(nr);
        }
        return nr;
    }


    /**
     * Overrides <code>FilterInputStream.read</code>
     * to update the progress observer after the read.
     */
    @Override
    public int read(byte b[], int off, int len) throws IOException {
        int nr = in.read(b, off, len);
        if (nr > 0) {
            fireProgress(nr);
        }
        return nr;
    }


    /**
     * Overrides <code>FilterInputStream.skip</code>
     * to update the progress observer after the skip.
     */
    @Override
    public long skip(long n) throws IOException {
        long nr = in.skip(n);
        if (nr > 0) {
            fireProgress(nr);
        }
        return nr;
    }

    /**
     * Overrides <code>FilterInputStream.close</code>
     * to close the progress observer as well as the stream.
     */
    @Override
    public void close() throws IOException {
        in.close();
        if (numBytesRead != numBytesTotal) {
            observer.onReadEnded();
        }
    }

    /**
     * Overrides <code>FilterInputStream.reset</code>
     * to reset the progress observer as well as the stream.
     */
    @Override
    public synchronized void reset() throws IOException {
        in.reset();
        fireProgress(numBytesTotal - in.available() - numBytesRead);
    }

    private void fireProgress(long delta) throws IOException {
        if (numBytesRead == 0) {
            observer.onReadStarted(numBytesTotal);
        }
        numBytesRead += delta;
        observer.onReadProgress(numBytesRead);
        if (numBytesRead == numBytesTotal) {
            observer.onReadEnded();
        }
        if (observer.isReadingCanceled()) {
            InterruptedIOException exc =
                    new InterruptedIOException("Reading has been interrupted.");
            exc.bytesTransferred = (int)numBytesRead;
            throw exc;
        }
    }

}
