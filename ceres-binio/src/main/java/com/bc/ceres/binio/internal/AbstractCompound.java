package com.bc.ceres.binio.internal;

import com.bc.ceres.binio.*;

import java.io.IOException;


abstract class AbstractCompound extends AbstractCollection implements CompoundInstance {
    private final CompoundType compoundType;
    private final long position;
    private final MemberInstance[] members;

    protected AbstractCompound(IOContext context, CollectionData parent, CompoundType compoundType, long position) {
        super(context, parent);
        this.compoundType = compoundType;
        this.position = position;
        this.members = new MemberInstance[compoundType.getMemberCount()];
    }

    public final CompoundType getCompoundType() {
        return compoundType;
    }

    public final int getElementCount() {
        return compoundType.getMemberCount();
    }

    public final int getMemberCount() {
        return compoundType.getMemberCount();
    }

    public final long getPosition() {
        return position;
    }

    protected MemberInstance getMemberInstance(int index) throws IOException {
        ensureSizeResolved(index - 1);
        return members[index];
    }

    protected void setMemberInstance(int index, MemberInstance memberInstance) {
        members[index] = memberInstance;
    }

    public SequenceInstance getSequence() {
        throw new DataAccessException();
    }

    public CompoundInstance getCompound() {
        return this;
    }

    public byte getByte(int index) throws IOException {
        final MemberInstance memberInstance = getMemberInstance(index);
        return memberInstance.getByte();
    }

    public short getShort(int index) throws IOException {
        final MemberInstance memberInstance = getMemberInstance(index);
        return memberInstance.getShort();
    }

    public int getInt(int index) throws IOException {
        final MemberInstance memberInstance = getMemberInstance(index);
        return memberInstance.getInt();
    }

    public long getLong(int index) throws IOException {
        final MemberInstance memberInstance = getMemberInstance(index);
        return memberInstance.getLong();
    }

    public float getFloat(int index) throws IOException {
        final MemberInstance memberInstance = getMemberInstance(index);
        return memberInstance.getFloat();
    }

    public double getDouble(int index) throws IOException {
        final MemberInstance memberInstance = getMemberInstance(index);
        return memberInstance.getDouble();
    }

    public void setByte(int index, byte value) throws IOException {
        final MemberInstance memberInstance = getMemberInstance(index);
        memberInstance.setByte(value);
    }

    public void setShort(int index, short value) throws IOException {
        final MemberInstance memberInstance = getMemberInstance(index);
        memberInstance.setShort(value);
    }

    public void setInt(int index, int value) throws IOException {
        final MemberInstance memberInstance = getMemberInstance(index);
        memberInstance.setInt(value);
    }

    public void setLong(int index, long value) throws IOException {
        final MemberInstance memberInstance = getMemberInstance(index);
        memberInstance.setLong(value);
    }

    public void setFloat(int index, float value) throws IOException {
        final MemberInstance memberInstance = getMemberInstance(index);
        memberInstance.setFloat(value);
    }

    public void setDouble(int index, double value) throws IOException {
        final MemberInstance memberInstance = getMemberInstance(index);
        memberInstance.setDouble(value);
    }

    public SequenceData getSequence(int index) throws IOException {
        final MemberInstance memberInstance = getMemberInstance(index);
        return memberInstance.getSequence();
    }

    public CompoundData getCompound(int index) throws IOException {
        final MemberInstance memberInstance = getMemberInstance(index);
        return memberInstance.getCompound();
    }

    public int getMemberIndex(String name) {
        return getCompoundType().getMemberIndex(name);
    }

    public byte getByte(String name) throws IOException {
        return getByte(getMemberIndexSafe(name));
    }

    public int getUByte(String name) throws IOException {
        return getUByte(getMemberIndexSafe(name));
    }

    public short getShort(String name) throws IOException {
        return getShort(getMemberIndexSafe(name));
    }

    public int getUShort(String name) throws IOException {
        return getUShort(getMemberIndexSafe(name));
    }

    public int getInt(String name) throws IOException {
        return getInt(getMemberIndexSafe(name));
    }

    public long getUInt(String name) throws IOException {
        return getUInt(getMemberIndexSafe(name));
    }

    public long getLong(String name) throws IOException {
        return getLong(getMemberIndexSafe(name));
    }

    public float getFloat(String name) throws IOException {
        return getFloat(getMemberIndexSafe(name));
    }

    public double getDouble(String name) throws IOException {
        return getDouble(getMemberIndexSafe(name));
    }

    public void setByte(String name, byte value) throws IOException {
        setByte(getMemberIndexSafe(name), value);
    }

    public void setUByte(String name, int value) throws IOException {
        setUByte(getMemberIndexSafe(name), value);
    }

    public void setShort(String name, short value) throws IOException {
        setShort(getMemberIndexSafe(name), value);
    }

    public void setUShort(String name, int value) throws IOException {
        setUShort(getMemberIndexSafe(name), value);
    }

    public void setInt(String name, int value) throws IOException {
        setInt(getMemberIndexSafe(name), value);
    }

    public void setUInt(String name, long value) throws IOException {
        setUInt(getMemberIndexSafe(name), value);
    }

    public void setLong(String name, long value) throws IOException {
        setLong(getMemberIndexSafe(name), value);
    }

    public void setFloat(String name, float value) throws IOException {
        setFloat(getMemberIndexSafe(name), value);
    }

    public void setDouble(String name, double value) throws IOException {
        setDouble(getMemberIndexSafe(name), value);
    }

    public SequenceData getSequence(String name) throws IOException {
        final int index = getMemberIndexSafe(name);
        return getSequence(index);
    }

    public CompoundData getCompound(String name) throws IOException {
        final int index = getMemberIndexSafe(name);
        return getCompound(index);
    }

    private int getMemberIndexSafe(String name) {
        final int index = getMemberIndex(name);
        if (index == -1) {
            throw new DataAccessException("'" + name + "' is not a member of compound '" + getType().getName() + "'.");
        }
        return index;
    }

    public void flush() throws IOException {
        for (MemberInstance member : members) {
            member.flush();
        }
    }
}