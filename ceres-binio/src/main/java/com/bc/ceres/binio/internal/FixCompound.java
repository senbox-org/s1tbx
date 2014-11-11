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
import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.DataContext;
import com.bc.ceres.binio.Type;


final class FixCompound extends AbstractCompound {

    FixCompound(DataContext context, CollectionData parent, CompoundType compoundType, long position) {
        this(context, parent, compoundType, new Segment(position, compoundType.getSize()), 0);
    }

    FixCompound(DataContext context, CollectionData parent, CompoundType compoundType, Segment segment, int bufferOffset) {
        super(context, parent, compoundType, segment.getPosition() + bufferOffset);
        for (int i = 0; i < compoundType.getMemberCount(); i++) {
            final Type memberType = compoundType.getMember(i).getType();
            setMemberInstance(i, InstanceFactory.createFixMember(context, this, memberType, segment, bufferOffset));
            bufferOffset += memberType.getSize();
        }
    }

    @Override
    public long getSize() {
        return getType().getSize();
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
    public void resolveSize() {
        // ok
    }

    @Override
    public void resolveSize(int index) {
        // ok
    }

    static int getMemberIndexWithinSizeLimit(CompoundType compoundType, long sizeLimit) {
        int index = -1;
        int segmentSize = 0;
        for (int i = 0; i < compoundType.getMemberCount(); i++) {
            final Type memberType = compoundType.getMember(i).getType();
            if (!memberType.isSizeKnown()
                    || segmentSize + memberType.getSize() > sizeLimit) {
                break;
            }
            index = i;
            segmentSize += memberType.getSize();
        }
        return index;
    }

    static boolean isCompoundTypeWithinSizeLimit(CompoundType compoundType, long sizeLimit) {
        return getMemberIndexWithinSizeLimit(compoundType, sizeLimit) == compoundType.getMemberCount() - 1;
    }
}