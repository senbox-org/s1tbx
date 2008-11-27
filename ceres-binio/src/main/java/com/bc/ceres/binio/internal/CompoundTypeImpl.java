package com.bc.ceres.binio.internal;

import com.bc.ceres.binio.CompoundMember;
import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.Type;

public final class CompoundTypeImpl extends AbstractType implements CompoundType {
    private final String name;
    private final CompoundMember[] members;
    private Object metadata;
    private int size;

    public CompoundTypeImpl(String name, CompoundMember[] members) {
        this(name, members, null);
    }

    public CompoundTypeImpl(String name, CompoundMember[] members, Object metadata) {
        this.name = name;
        this.members = members.clone();
        this.metadata = metadata;
        updateSize();
    }

    public Object getMetadata() {
        return metadata;
    }

    public void setMetadata(Object metadata) {
        this.metadata = metadata;
    }

    public CompoundMember[] getMembers() {
        return members.clone();
    }

    public int getMemberCount() {
        return members.length;
    }

    public int getMemberIndex(String name) {
        // todo - OPT: trivial implementation, optimize using a Map<String, int>
        for (int i = 0; i < members.length; i++) {
            CompoundMember member = members[i];
            if (name.equalsIgnoreCase(member.getName())) {
                return i;
            }
        }
        return -1;
    }

    public CompoundMember getMember(int memberIndex) {
        return members[memberIndex];
    }

    public void setMember(int memberIndex, CompoundMember member) {
        members[memberIndex] = member;
        updateSize();
    }

    public String getMemberName(int memberIndex) {
        return getMember(memberIndex).getName();
    }

    public Type getMemberType(int memberIndex) {
        return getMember(memberIndex).getType();
    }

    public int getMemberSize(int memberIndex) {
        return getMember(memberIndex).getType().getSize();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public final boolean isCollectionType() {
        return true;
    }

    @Override
    public boolean isCompoundType() {
        return true;
    }

    private void updateSize() {
        int size = 0;
        for (CompoundMember member : members) {
            final int memberSize = member.getType().getSize();
            if (memberSize >= 0 && size >= 0) {
                size += memberSize;
            } else {
                size = -1;
                break;
            }
        }
        this.size = size;
    }

}