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

import com.bc.ceres.binio.*;

import java.io.IOException;


final class VarSequenceOfSimples extends AbstractSequenceOfSimples {
    private SequenceType resolvedSequenceType;
    private Segment segment;
    private final long position;

    public VarSequenceOfSimples(DataContext context, CollectionData parent, SequenceType sequenceType, long position) {
        super(context, parent, sequenceType);
        this.position = position;
    }

    @Override
    protected Segment getSegment() {
        return segment;
    }

    @Override
    protected int getSegmentOffset() {
        return 0;
    }

    @Override
    public SequenceType getType() {
        return resolvedSequenceType;
    }

    @Override
    public long getPosition() {
        return position;
    }

    @Override
    public long getSize() {
        return resolvedSequenceType != null ? resolvedSequenceType.getSize() : -1L;
    }

    @Override
    public int getElementCount() {
        return resolvedSequenceType != null ? resolvedSequenceType.getElementCount() : -1;
    }

    // todo - code duplication: see VarSequenceOfFixCollections.resolveSize()
    @Override
    public boolean isSizeResolved(int index) {
        return resolvedSequenceType != null;
    }

    // todo - code duplication: see VarSequenceOfFixCollections.resolveSize()
    @Override
    public void resolveSize(int index) throws IOException {
        resolveSize();
    }

    // todo - code duplication: see VarSequenceOfFixCollections.resolveSize()
    @Override
    public void resolveSize() throws IOException {
        if (resolvedSequenceType == null) {
            resolvedSequenceType = resolveSequenceType();
            if (!resolvedSequenceType.isSizeKnown()) {
                throw new DataAccessException(toString());
            }
        }
    }

    @Override
    public boolean isDataAccessible() {
        return segment != null && segment.isDataAccessible();
    }

    @Override
    public void makeDataAccessible() throws IOException {
        if (!isDataAccessible()) {
            segment = new Segment(position, resolvedSequenceType.getSize());
            segment.makeDataAccessible(getContext());
        }
    }

    @Override
    public void flush() throws IOException {
        if (isDataAccessible()) {
            segment.flushData(getContext());
        }
    }
}