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

package com.bc.ceres.util;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;
import java.util.Map;

/**
 * A template reader replaces any occurences of <code>${<i>key</i>}</code> or <code>$<i>key</i></code>
 * in the underlying stream with the string representations of any non-null value returned by a given
 * resolver for that key.
 *
 * @author Norman Fomferra
 */
public class TemplateReader extends FilterReader {

    private static final char DEFAULT_KEY_INDICATOR = '$';
    private static final int EOF = -1;
    private Resolver resolver;
    private IntBuffer buffer;
    private char keyIndicator = DEFAULT_KEY_INDICATOR;

    /**
     * Constructs a template reader for the given reader stream and a resolver given by a {@link Map}.
     *
     * @param in  the underlying reader
     * @param map the map to serve as resolver
     */
    public TemplateReader(Reader in, Map map) {
        this(in, new KeyValueResolver(map));
    }

    /**
     * Constructs a template reader for the given reader stream and the given resolver.
     *
     * @param in       the underlying reader
     * @param resolver the resolver
     */
    public TemplateReader(Reader in, Resolver resolver) {
        super(in);
        if (resolver == null) {
            throw new NullPointerException("resolver");
        }
        this.resolver = resolver;
    }

    /**
     * Gets the key indicator.
     *
     * @return the key indicator, defaults to '$'.
     */
    public char getKeyIndicator() {
        return keyIndicator;
    }

    /**
     * Sets the key indicator.
     *
     * @param keyIndicator the key indicator, must not be a digit, letter or whitespace.
     */
    public void setKeyIndicator(char keyIndicator) {
        if (Character.isWhitespace(keyIndicator) || Character.isLetterOrDigit(keyIndicator)) {
            throw new IllegalArgumentException();
        }
        this.keyIndicator = keyIndicator;
    }

    /**
     * Reads all content.
     * @return the content
     * @throws IOException if an I/O error occurs
     */
    public String readAll() throws IOException {
        StringBuilder sb = new StringBuilder(16 * 1024);
        while (true) {
            int i = read();
            if (i == EOF) {
                break;
            }
            sb.append((char) i);
        }
        return sb.toString();
    }

    /**
     * Read a single character.
     *
     * @throws IOException If an I/O error occurs
     */
    @Override
    public int read() throws IOException {
        synchronized (lock) {

            if (buffer != null && buffer.ready()) {
                return buffer.next();
            }

            int c = in.read();
            if (c != keyIndicator) {
                return c;
            }

            if (buffer == null) {
                buffer = new IntBuffer();
            }
            buffer.reset();
            buffer.append(keyIndicator);

            c = readAndBuffer();
            if (c != EOF) {
                int keyType = 0;
                if (c == '{') {  // ${key}?
                    do {
                        c = readAndBuffer();
                        if (c == '}') {
                            keyType = 1;
                            break;
                        }
                    } while (c != EOF);
                } else if (Character.isJavaIdentifierStart(c)) { // $key?
                    keyType = 2;
                    do {
                        c = readAndBuffer();
                        if (!(Character.isJavaIdentifierPart(c) || c == '.')) {
                            break;
                        }
                    } while (c != EOF);
                }

                if (keyType != 0) {
                    String key;
                    if (keyType == 1) {  // ${key}
                        key = buffer.substring(2, buffer.length() - 1);
                    } else { // $key
                        key = buffer.substring(1, buffer.length() - 1);
                    }

                    Object value = resolver.resolve(key);
                    if (value != null) {
                        String s = value.toString();
                        int last;
                        if (keyType == 1) { // ${key}
                            last = in.read();
                            buffer.reset();
                            buffer.append(s);
                        } else { // $key
                            last = buffer.charAt(buffer.length() - 1);
                            buffer.reset();
                            buffer.append(s);
                        }
                        buffer.append(last); // last can also be EOF!
                    }
                }
            }
            return buffer.next();
        }
    }

    /**
     * Read characters into an array.  This method will block until some input
     * is available, an I/O error occurs, or the end of the stream is reached.
     *
     * @param cbuf Destination buffer
     * @return The number of characters read, or -1
     *         if the end of the stream
     *         has been reached
     * @throws IOException If an I/O error occurs
     */
    @Override
    public int read(char cbuf[]) throws IOException {
        return read(cbuf, 0, cbuf.length);
    }

    /**
     * Read characters into a portion of an array.
     *
     * @throws IOException If an I/O error occurs
     */
    @Override
    public int read(char cbuf[], int off, int len) throws IOException {
        synchronized (lock) {
            int i;
            for (i = 0; i < len; i++) {
                int c = read();
                if (c == EOF) {
                    return i == 0 ? EOF : i;
                }
                cbuf[off + i] = (char) c;
            }
            return i;
        }
    }

    /**
     * Attempts to read characters into the specified character buffer.
     * The buffer is used as a repository of characters as-is: the only
     * changes made are the results of a put operation. No flipping or
     * rewinding of the buffer is performed.
     *
     * @param target the buffer to read characters into
     * @return The number of characters added to the buffer, or
     *         -1 if this source of characters is at its end
     * @throws IOException  if an I/O error occurs
     * @throws NullPointerException if target is null
     * @throws java.nio.ReadOnlyBufferException
     *                              if target is a read only buffer
     */
    @Override
    public int read(CharBuffer target) throws IOException {
        synchronized (lock) {
            int len = target.remaining();
            char[] cbuf = new char[len];
            int n = read(cbuf, 0, len);
            if (n > 0) {
                target.put(cbuf, 0, n);
            }
            return n;
        }
    }

    /**
     * Skip characters.
     *
     * @throws IOException If an I/O error occurs
     */
    @Override
    public long skip(long n) throws IOException {
        synchronized (lock) {
            if (n < 0L) {
                throw new IllegalArgumentException("skip value is negative");
            }
            long i;
            for (i = 0; i < n; i++) {
                int c = read();
                if (c == EOF) {
                    break;
                }
            }
            return i;
        }
    }

    /**
     * Tell whether this stream is ready to be read.
     *
     * @throws IOException If an I/O error occurs
     */
    @Override
    public boolean ready() throws IOException {
        return (buffer != null && buffer.ready()) || in.ready();
    }

    /**
     * Tell whether this stream supports the mark() operation.
     */
    @Override
    public boolean markSupported() {
        return false;
    }

    /**
     * Mark the present position in the stream.
     *
     * @throws IOException If an I/O error occurs
     */
    @Override
    public void mark(int readAheadLimit) throws IOException {
        throw new IOException("mark() not supported");
    }

    /**
     * Reset the stream.
     *
     * @throws IOException If an I/O error occurs
     */
    @Override
    public void reset() throws IOException {
        throw new IOException("reset() not supported");
    }

    /**
     * Close the stream.
     *
     * @throws IOException If an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        super.close();
        buffer = null;
    }

    /////////////////////////////////////////////////////////////////////////////

    private int readAndBuffer() throws IOException {
        int c = in.read();
        buffer.append(c);
        return c;
    }

    /////////////////////////////////////////////////////////////////////////////

    public static interface Resolver {

        Object resolve(String reference);
    }


    private static class KeyValueResolver implements Resolver {

        private Map map;

        public KeyValueResolver(Map map) {
            if (map == null) {
                throw new NullPointerException("map");
            }
            this.map = map;
        }

        public Object resolve(String reference) {
            return map.get(reference);
        }
    }


    /**
     * A buffer to which EOF can also be appended.
     */
    private static class IntBuffer {

        private final static int INC = 8192;
        private int[] buffer;
        private int length;
        private int index;

        public IntBuffer() {
            this.buffer = new int[INC];
        }

        public void reset() {
            length = 0;
            index = 0;
        }

        public int next() {
            if (!ready()) {
                throw new IllegalStateException("!ready()");
            }
            return buffer[index++];
        }

        public int charAt(int pos) {
            if (pos < 0) {
                throw new IndexOutOfBoundsException("pos < 0");
            }
            if (pos >= length) {
                throw new IndexOutOfBoundsException("pos >= length");
            }
            return buffer[pos];
        }

        public void append(int c) {
            if (length >= buffer.length) {
                int[] newBuffer = new int[buffer.length + INC];
                System.arraycopy(buffer, 0, newBuffer, 0, length);
                buffer = newBuffer;
            }
            buffer[length++] = c;
        }

        public void append(String s) {
            for (int i = 0; i < s.length(); i++) {
                append((int) s.charAt(i));
            }
        }

        public boolean ready() {
            return index < length;
        }

        public String substring(int start, int end) {
            int n = end - start;
            char[] cbuf = new char[n];
            for (int i = start; i < end; i++) {
                cbuf[i - start] = (char) buffer[i];
            }
            return new String(cbuf);
        }

        public int length() {
            return length;
        }
    }
}
