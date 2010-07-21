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

import com.bc.ceres.binio.CollectionData;
import com.bc.ceres.binio.DataContext;
import com.bc.ceres.binio.SequenceType;

import java.io.IOException;


final class FixSequenceOfSimples extends AbstractSequenceOfSimples {
    private final Segment segment;
    private final int segmentOffset;

    public FixSequenceOfSimples(DataContext context, CollectionData parent, SequenceType sequenceType, long position) {
        this(context, parent, sequenceType, new Segment(position, sequenceType.getSize()), 0);
    }

    public FixSequenceOfSimples(DataContext context, CollectionData parent, SequenceType sequenceType, Segment segment, int segmentOffset) {
        super(context, parent, sequenceType);
        this.segment = segment;
        this.segmentOffset = segmentOffset;
    }

    @Override
    protected Segment getSegment() {
        return segment;
    }

    @Override
    protected int getSegmentOffset() {
        return segmentOffset;
    }

    @Override
    public long getPosition() {
        return segment.getPosition();
    }

    @Override
    public long getSize() {
        return getType().getSize();
    }

    @Override
    public int getElementCount() {
        return getType().getElementCount();
    }

    @Override
    public boolean isSizeResolved() {
        return true;
    }

    @Override
    public boolean isSizeResolved(int index) {
        return true;
    }

    @Override
    public void resolveSize(int index) throws IOException {
        // ok
    }

    @Override
    public void resolveSize() throws IOException {
        // ok
    }

    @Override
    public boolean isDataAccessible() {
        return segment.isDataAccessible();
    }

    @Override
    public void makeDataAccessible() throws IOException {
        segment.makeDataAccessible(getContext());
    }

    @Override
    public void flush() throws IOException {
        segment.flushData(getContext());
    }
}
