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
import com.bc.ceres.binio.Type;
import com.bc.ceres.binio.SequenceType;

import java.io.IOException;

/**
 * Represents a data type that is composed of a sequence of zero or more elements
 * all having the same data type.
 */
public final class GrowableSequenceTypeImpl extends AbstractType implements SequenceType {
    private final String name;
    private final Type elementType;
    private int elementCount;
    private int size;

    public GrowableSequenceTypeImpl(Type elementType) {
        name = elementType.getName() + "[]";
        this.elementType = elementType;
        this.elementCount = 0;
        this.size = 0;
    }

    @Override
    public final Type getElementType() {
        return elementType;
    }

    @Override
    public final synchronized int getElementCount() {
        return elementCount;
    }

    public final synchronized void incElementCount() {
        this.elementCount++;
        this.size += elementType.getSize();
    }

    @Override
    public final synchronized String getName() {
        return name;
    }

    @Override
    public final synchronized int getSize() {
        return size;
    }

    @Override
    public final boolean isCollectionType() {
        return true;
    }

    @Override
    public final boolean isSequenceType() {
        return true;
    }
}