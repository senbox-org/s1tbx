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
import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.DataAccessException;
import com.bc.ceres.binio.DataContext;
import com.bc.ceres.binio.SequenceData;

import java.io.IOException;


abstract class AbstractCompound extends AbstractCollection implements CompoundInstance {

    private final CompoundType compoundType;
    private final long position;
    private final MemberInstance[] members;

    protected AbstractCompound(DataContext context, CollectionData parent, CompoundType compoundType, long position) {
        super(context, parent);
        this.compoundType = compoundType;
        this.position = position;
        this.members = new MemberInstance[compoundType.getMemberCount()];
    }

    @Override
    public CompoundType getType() {
        return compoundType;
    }

    @Override
    @Deprecated
    public final CompoundType getCompoundType() {
        return getType();
    }

    @Override
    public final int getElementCount() {
        return compoundType.getMemberCount();
    }

    @Override
    public final int getMemberCount() {
        return compoundType.getMemberCount();
    }

    @Override
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

    @Override
    public SequenceInstance getSequence() {
        throw new DataAccessException(getTypeErrorMsg());
    }

    @Override
    public CompoundInstance getCompound() {
        return this;
    }

    @Override
    public byte getByte(int index) throws IOException {
        final MemberInstance memberInstance = getMemberInstance(index);
        return memberInstance.getByte();
    }

    @Override
    public short getShort(int index) throws IOException {
        final MemberInstance memberInstance = getMemberInstance(index);
        return memberInstance.getShort();
    }

    @Override
    public int getInt(int index) throws IOException {
        final MemberInstance memberInstance = getMemberInstance(index);
        return memberInstance.getInt();
    }

    @Override
    public long getLong(int index) throws IOException {
        final MemberInstance memberInstance = getMemberInstance(index);
        return memberInstance.getLong();
    }

    @Override
    public float getFloat(int index) throws IOException {
        final MemberInstance memberInstance = getMemberInstance(index);
        return memberInstance.getFloat();
    }

    @Override
    public double getDouble(int index) throws IOException {
        final MemberInstance memberInstance = getMemberInstance(index);
        return memberInstance.getDouble();
    }

    @Override
    public void setByte(int index, byte value) throws IOException {
        final MemberInstance memberInstance = getMemberInstance(index);
        memberInstance.setByte(value);
    }

    @Override
    public void setShort(int index, short value) throws IOException {
        final MemberInstance memberInstance = getMemberInstance(index);
        memberInstance.setShort(value);
    }

    @Override
    public void setInt(int index, int value) throws IOException {
        final MemberInstance memberInstance = getMemberInstance(index);
        memberInstance.setInt(value);
    }

    @Override
    public void setLong(int index, long value) throws IOException {
        final MemberInstance memberInstance = getMemberInstance(index);
        memberInstance.setLong(value);
    }

    @Override
    public void setFloat(int index, float value) throws IOException {
        final MemberInstance memberInstance = getMemberInstance(index);
        memberInstance.setFloat(value);
    }

    @Override
    public void setDouble(int index, double value) throws IOException {
        final MemberInstance memberInstance = getMemberInstance(index);
        memberInstance.setDouble(value);
    }

    @Override
    public SequenceData getSequence(int index) throws IOException {
        final MemberInstance memberInstance = getMemberInstance(index);
        return memberInstance.getSequence();
    }

    @Override
    public CompoundData getCompound(int index) throws IOException {
        final MemberInstance memberInstance = getMemberInstance(index);
        return memberInstance.getCompound();
    }

    @Override
    public int getMemberIndex(String name) {
        return getType().getMemberIndex(name);
    }

    @Override
    public byte getByte(String name) throws IOException {
        return getByte(getMemberIndexSafe(name));
    }

    @Override
    public int getUByte(String name) throws IOException {
        return getUByte(getMemberIndexSafe(name));
    }

    @Override
    public short getShort(String name) throws IOException {
        return getShort(getMemberIndexSafe(name));
    }

    @Override
    public int getUShort(String name) throws IOException {
        return getUShort(getMemberIndexSafe(name));
    }

    @Override
    public int getInt(String name) throws IOException {
        return getInt(getMemberIndexSafe(name));
    }

    @Override
    public long getUInt(String name) throws IOException {
        return getUInt(getMemberIndexSafe(name));
    }

    @Override
    public long getLong(String name) throws IOException {
        return getLong(getMemberIndexSafe(name));
    }

    @Override
    public float getFloat(String name) throws IOException {
        return getFloat(getMemberIndexSafe(name));
    }

    @Override
    public double getDouble(String name) throws IOException {
        return getDouble(getMemberIndexSafe(name));
    }

    @Override
    public void setByte(String name, byte value) throws IOException {
        setByte(getMemberIndexSafe(name), value);
    }

    @Override
    public void setUByte(String name, int value) throws IOException {
        setUByte(getMemberIndexSafe(name), value);
    }

    @Override
    public void setShort(String name, short value) throws IOException {
        setShort(getMemberIndexSafe(name), value);
    }

    @Override
    public void setUShort(String name, int value) throws IOException {
        setUShort(getMemberIndexSafe(name), value);
    }

    @Override
    public void setInt(String name, int value) throws IOException {
        setInt(getMemberIndexSafe(name), value);
    }

    @Override
    public void setUInt(String name, long value) throws IOException {
        setUInt(getMemberIndexSafe(name), value);
    }

    @Override
    public void setLong(String name, long value) throws IOException {
        setLong(getMemberIndexSafe(name), value);
    }

    @Override
    public void setFloat(String name, float value) throws IOException {
        setFloat(getMemberIndexSafe(name), value);
    }

    @Override
    public void setDouble(String name, double value) throws IOException {
        setDouble(getMemberIndexSafe(name), value);
    }

    @Override
    public SequenceData getSequence(String name) throws IOException {
        final int index = getMemberIndexSafe(name);
        return getSequence(index);
    }

    @Override
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

    @Override
    public void flush() throws IOException {
        for (MemberInstance member : members) {
            if (member != null) {
                member.flush();
            }
        }
    }
}