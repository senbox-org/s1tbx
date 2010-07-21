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

import java.io.IOException;


final class VarCompound extends AbstractCompound {
    private int maxResolvedIndex;
    private long size;

    public VarCompound(DataContext context, CollectionData parent, CompoundType compoundType, long position) {
        super(context, parent, compoundType, position);
        this.maxResolvedIndex = -1;

        // todo - OPT: do this in resolve()
        // todo - OPT: do this for all subsequntial fixed-size member types

        // Determine first members up to maxMemberIndex which have known size
        int maxMemberIndex = FixCompound.getMemberIndexWithinSizeLimit(compoundType, Segment.getSegmentSizeLimit());
        if (maxMemberIndex >= 0) {
            int segmentSize = 0;
            for (int i = 0; i <= maxMemberIndex; i++) {
                segmentSize += compoundType.getMember(i).getType().getSize();
            }
            Segment segment = new Segment(position, segmentSize);
            int segmentOffset = 0;
            for (int i = 0; i <= maxMemberIndex; i++) {
                final Type memberType = compoundType.getMember(i).getType();
                final MemberInstance fixMember = InstanceFactory.createFixMember(context, this, memberType,
                                                                                 segment,
                                                                                 segmentOffset);
                setMemberInstance(i, fixMember);
                size += fixMember.getSize();
                segmentOffset += memberType.getSize();
            }
        }

        maxResolvedIndex = maxMemberIndex;
    }

    @Override
    public long getSize() {
        return isSizeResolved() ? size : -1;
    }

    @Override
    public boolean isSizeResolved() {
        return isSizeResolved(getMemberCount() - 1);
    }

    @Override
    public boolean isSizeResolved(int index) {
        return index <= maxResolvedIndex;
    }

    @Override
    public void resolveSize() throws IOException {
        resolveSize(getElementCount() - 1);
    }

    @Override
    public void resolveSize(int index) throws IOException {
        if (isSizeResolved(index)) {
            return;
        }
        for (int i = maxResolvedIndex + 1; i <= index; i++) {
            MemberInstance memberInstance = getMemberInstance(i);
            if (!memberInstance.isSizeResolved()) {
                memberInstance.resolveSize();
            }
            size += memberInstance.getSize();
            maxResolvedIndex = i;
        }
    }

    @Override
    protected MemberInstance getMemberInstance(int i) throws IOException {
        MemberInstance memberInstance = super.getMemberInstance(i);
        if (memberInstance == null) {
            memberInstance = createMemberInstance(i);
            setMemberInstance(i, memberInstance);
        }
        return memberInstance;
    }

    private MemberInstance createMemberInstance(int index) throws IOException {
        final DataContext context = getContext();
        final Type memberType = getType().getMemberType(index);
        final long position;
        if (index > 0) {
            final MemberInstance prevMember = getMemberInstance(index - 1);
            if (!prevMember.isSizeResolved()) {
                prevMember.resolveSize();
            }
            position = prevMember.getPosition() + prevMember.getSize();
        } else {
            position = getPosition();
        }
        return InstanceFactory.createMember(context, this, memberType, position,
                                            getContext().getFormat().getByteOrder());
    }

}