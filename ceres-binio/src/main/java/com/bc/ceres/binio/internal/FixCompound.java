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

    public Type getType() {
        return getCompoundType();
    }

    public long getSize() {
        return getCompoundType().getSize();
    }

    @Override
    public boolean isSizeResolved() {
        return true;
    }

    public boolean isSizeResolved(int index) {
        return true;
    }

    public void resolveSize() {
        // ok
    }

    public void resolveSize(int index) {
        // ok
    }

    public static int getMemberIndexWithinSizeLimit(CompoundType compoundType, int sizeLimit) {
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

    public static boolean isCompoundTypeWithinSizeLimit(CompoundType compoundType, int sizeLimit) {
        return getMemberIndexWithinSizeLimit(compoundType, sizeLimit) == compoundType.getMemberCount() - 1;
    }
}