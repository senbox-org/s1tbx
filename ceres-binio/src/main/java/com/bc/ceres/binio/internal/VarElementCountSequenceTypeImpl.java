package com.bc.ceres.binio.internal;

import com.bc.ceres.binio.CollectionData;
import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.Type;

import java.io.IOException;

/**
 * Represents a data type that is composed of a sequence of zero or more elements
 * all having the same data type.
 */
public final class VarElementCountSequenceTypeImpl extends VarElementCountSequenceType {
    private final String memberName;
    private volatile int memberIndex;

    public VarElementCountSequenceTypeImpl(Type elementType, int memberIndex) {
        super(elementType.getName() + "[$" + memberIndex + "]", elementType);
        this.memberName = null;
        this.memberIndex = memberIndex;
    }

    public VarElementCountSequenceTypeImpl(Type elementType, String memberName) {
        super(elementType.getName() + "[$" + memberName + "]", elementType);
        this.memberName = memberName;
        this.memberIndex = -1;
    }

    public String getMemberName() {
        return memberName;
    }

    public int getMemberIndex() {
        return memberIndex;
    }

    @Override
    protected int resolveElementCount(CollectionData parent) throws IOException {
        if (memberIndex == -1) {
            synchronized (this) {
                if (memberIndex == -1) {
                    if (parent instanceof CompoundData) {
                        CompoundData compoundData = (CompoundData) parent;
                        memberIndex = compoundData.getType().getMemberIndex(memberName);
                    }
                }
            }
            if (memberIndex == -1) {
                throw new IllegalArgumentException("parent");
            }
        }
        return parent.getInt(memberIndex);
    }
}