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

package com.bc.ceres.core.runtime.internal;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

public class HrefParser {
    private Reader reader;

    public HrefParser(Reader reader) {
        this.reader = reader;
    }

    public String[] parse() throws IOException {
        final ArrayList<String> list = new ArrayList<String>(32);
        parse(new Handler() {
            public void onValueSeen(String value) {
                list.add(value);
            }
        });
        return list.toArray(new String[0]);
    }

    public void parse(Handler handler) throws IOException {
        M m = M.TEXT;
        StringBuilder buffer = new StringBuilder(1024);
        while (true) {
            final int i = reader.read();
            if (i == -1) {
                break;
            }
            char c = (char) i;
            buffer.append(Character.isWhitespace(c) ? ' ' : c);

            if (m == M.TEXT) {
                if (endsWith(buffer, "<!--")) {
                    buffer.setLength(0);
                    m = M.COMMENT;
                } else if (endsWith(buffer, " href")) {
                    buffer.setLength(0);
                    m = M.HREF_SEEN;
                } else if (c == '>') {
                    buffer.setLength(0);
                }
            } else if (m == M.COMMENT) {
                if (endsWith(buffer, "-->")) {
                    buffer.setLength(0);
                    m = M.TEXT;
                }
            } else if (m == M.HREF_SEEN) {
                if (c == '=') {
                    m = M.HREF_EQ_SEEN;
                } else if (!Character.isWhitespace(c)) {
                    m = M.TEXT;
                }
                buffer.setLength(0);
            } else if (m == M.HREF_EQ_SEEN) {
                if (c == '"') {
                    m = M.HREF_VALUE;
                } else if (c == '\'') {
                    m = M.HREF_VALUE;
                } else if (!Character.isWhitespace(c)) {
                    m = M.HREF_VALUE;
                } else {
                    buffer.setLength(0);
                }
            } else if (m == M.HREF_VALUE) {
                if (startsWith(buffer, "\"")) {
                    if (c == '"') {
                        handler.onValueSeen(buffer.substring(1, buffer.length() - 1));
                        buffer.setLength(0);
                        m = M.TEXT;
                    }
                } else if (startsWith(buffer, "'")) {
                    if (c == '\'') {
                        handler.onValueSeen(buffer.substring(1, buffer.length() - 1));
                        buffer.setLength(0);
                        m = M.TEXT;
                    }
                } else {
                    if (Character.isWhitespace(c) || c == '>' || c == '/') {
                        handler.onValueSeen(buffer.substring(0, buffer.length() - 1));
                        buffer.setLength(0);
                        m = M.TEXT;
                    }
                }
            } else {
                throw new IllegalStateException();
            }
        }

    }

    boolean startsWith(StringBuilder buffer, String token) {
        final int n = token.length();
        if (buffer.length() < n) {
            return false;
        }
        for (int i = 0; i < n; i++) {
            final char tc = token.charAt(i);
            final char bc = buffer.charAt(i);
            if (Character.toUpperCase(tc) !=
                    Character.toUpperCase(bc)) {
                return false;
            }
        }
        return true;
    }

    boolean endsWith(StringBuilder buffer, String token) {
        final int n = token.length();
        if (buffer.length() < n) {
            return false;
        }
        for (int i = 0; i < n; i++) {
            final char tc = token.charAt(i);
            final char bc = buffer.charAt(buffer.length() - n + i);
            if (Character.toUpperCase(tc) !=
                    Character.toUpperCase(bc)) {
                return false;
            }
        }
        return true;
    }

    private enum M {
        TEXT,
        COMMENT,
        HREF_SEEN,
        HREF_EQ_SEEN,
        HREF_VALUE,
    }

    public static interface Handler {
        void onValueSeen(String value);
    }
}
