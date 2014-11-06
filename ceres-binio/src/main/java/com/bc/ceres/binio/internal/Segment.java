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

package com.bc.ceres.binio.internal;

import com.bc.ceres.binio.DataContext;

import java.io.IOException;


final class Segment {
    private final long position;
    private final int size;
    private byte[] data;
    private boolean dirty;

    static final String SEGMENT_SIZE_LIMIT_PROPERTY = "ceres.binio.segmentSizeLimit";

    private static long segmentSizeLimit = 16 * 1024L;

    static {
        final String value = System.getProperty(SEGMENT_SIZE_LIMIT_PROPERTY);
        if (value != null) {
            try {
                segmentSizeLimit = Long.parseLong(value);
            } catch (NumberFormatException e) {
                // ignored
            }
        }
    }

    static long getSegmentSizeLimit() {
        return segmentSizeLimit;
    }

    public Segment(long position, int size) {
        this.position = position;
        this.size = size;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public long getPosition() {
        return position;
    }

    public int getSize() {
        return size;
    }

    public byte[] getData() {
        return data;
    }

    public boolean isDataAccessible() {
        return data != null;
    }

    public void makeDataAccessible(DataContext context) throws IOException {
        if (data == null) {
            data = new byte[size];
            setDirty(false);
            context.getHandler().read(context, data, position);
        }
    }

    public synchronized void flushData(DataContext context) throws IOException {
        if (isDirty()) {
            context.getHandler().write(context, data, position);
            setDirty(false);
        }
    }
}
