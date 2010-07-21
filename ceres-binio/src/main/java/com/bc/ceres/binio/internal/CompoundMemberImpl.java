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
import com.bc.ceres.binio.MetadataAware;
import com.bc.ceres.binio.Type;


public final class CompoundMemberImpl implements MetadataAware, CompoundMember {
    private final String name;
    private final Type type;
    private final long size;
    private Object metadata;

    public CompoundMemberImpl(String name, Type type) {
        this(name, type, null);
    }

    public CompoundMemberImpl(String name, Type type, Object metadata) {
        this(name, type, type.getSize(), metadata);
    }

    public CompoundMemberImpl(String name, Type type, long size, Object metadata) {
        this.name = name;
        this.type = type;
        this.size = size;
        this.metadata = metadata;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public long getSize() {
        return size == -1 ? type.getSize() : size;
    }

    @Override
    public Object getMetadata() {
        return metadata;
    }

    @Override
    public void setMetadata(Object metadata) {
        this.metadata = metadata;
    }
}
