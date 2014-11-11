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

package com.bc.ceres.binio;

public final class SimpleType implements Type {
    public final static SimpleType BYTE = new SimpleType("byte", 1);
    public final static SimpleType UBYTE = new SimpleType("ubyte", 1);
    public final static SimpleType SHORT = new SimpleType("short", 2);
    public final static SimpleType USHORT = new SimpleType("ushort", 2);
    public final static SimpleType INT = new SimpleType("int", 4);
    public final static SimpleType UINT = new SimpleType("uint", 4);
    public final static SimpleType LONG = new SimpleType("long", 8);
    public final static SimpleType ULONG = new SimpleType("ulong", 8);
    public final static SimpleType FLOAT = new SimpleType("float", 4);
    public final static SimpleType DOUBLE = new SimpleType("double", 8);

    private final String name;
    private final int size;

    private SimpleType(String name, int size) {
        this.name = name;
        this.size = size;
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public final int getSize() {
        return size;
    }

    @Override
    public final boolean isSizeKnown() {
        return true;
    }

    @Override
    public final boolean isSimpleType() {
        return true;
    }

    @Override
    public final boolean isCollectionType() {
        return false;
    }

    @Override
    public final boolean isSequenceType() {
        return false;
    }

    @Override
    public final boolean isCompoundType() {
        return false;
    }
}
