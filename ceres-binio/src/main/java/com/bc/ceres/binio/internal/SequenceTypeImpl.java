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

import com.bc.ceres.binio.SequenceType;
import com.bc.ceres.binio.Type;
import com.bc.ceres.core.Assert;

/**
 * Represents a data type that is composed of a sequence of zero or more elements
 * all having the same data type.
 */
public final class SequenceTypeImpl extends AbstractType implements SequenceType {
    private final String name;
    private final Type elementType;
    private final int elementCount;
    private final int size;

    public SequenceTypeImpl(Type elementType, int elementCount) {
        Assert.notNull(elementType, "elementCount");
        Assert.argument(elementCount >= 0, "elementCount");
        this.name = elementType.getName() + "[" + elementCount + "]";
        this.elementType = elementType;
        this.elementCount = elementCount;
        this.size = elementCount * elementType.getSize();
    }

    @Override
    public final Type getElementType() {
        return elementType;
    }

    @Override
    public final int getElementCount() {
        return elementCount;
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
    public final boolean isCollectionType() {
        return true;
    }

    @Override
    public final boolean isSequenceType() {
        return true;
    }
}