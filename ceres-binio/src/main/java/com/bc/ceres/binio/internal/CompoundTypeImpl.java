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

import com.bc.ceres.binio.CompoundMember;
import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.Type;
import com.bc.ceres.core.Assert;

import java.util.HashMap;

public final class CompoundTypeImpl extends AbstractType implements CompoundType {
    private final String name;
    private final CompoundMember[] members;
    private volatile HashMap<String, Integer> indices;
    private volatile Object metadata;
    private int size;

    public CompoundTypeImpl(String name, CompoundMember[] members) {
        this(name, members, null);
    }

    public CompoundTypeImpl(String name, CompoundMember[] members, Object metadata) {
        this.name = name;
        this.members = members.clone();
        this.metadata = metadata;
        this.size = -1;
    }

    @Override
    public Object getMetadata() {
        return metadata;
    }

    @Override
    public void setMetadata(Object metadata) {
        this.metadata = metadata;
    }

    @Override
    public CompoundMember[] getMembers() {
        return members.clone();
    }

    @Override
    public int getMemberCount() {
        return members.length;
    }

    @Override
    public int getMemberIndex(String name) {
        if (indices == null) {
            synchronized (this) {
                if (indices == null) {
                    indices = new HashMap<String, Integer>(2 * getMemberCount());
                    for (int i = 0; i < members.length; i++) {
                        CompoundMember member = members[i];
                        indices.put(member.getName(), i);
                    }
                }
            }
        }
        Integer index = indices.get(name);
        return index != null ? index : -1;
    }

    @Override
    public CompoundMember getMember(int memberIndex) {
        return members[memberIndex];
    }

    public void setMember(int memberIndex, CompoundMember member) {
        Assert.notNull(member, "member");
        members[memberIndex] = member;
        size = -1;
    }

    @Override
    public String getMemberName(int memberIndex) {
        return getMember(memberIndex).getName();
    }

    @Override
    public Type getMemberType(int memberIndex) {
        return getMember(memberIndex).getType();
    }

    @Override
    public int getMemberSize(int memberIndex) {
        return getMember(memberIndex).getType().getSize();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getSize() {
        if (size == -1) {
            size = computeSize();
        }
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

    private int computeSize() {
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
        return size;
    }

}