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

import com.bc.ceres.binio.*;
import com.bc.ceres.core.Assert;

import java.io.IOException;

public abstract class VarElementCountSequenceType extends AbstractType implements VarSequenceType {
    private final String name;
    private final Type elementType;

    protected VarElementCountSequenceType(Type elementType) {
        this(elementType.getName() + "[]", elementType);
    }

    protected VarElementCountSequenceType(String name, Type elementType) {
        Assert.notNull(name, "name");
        Assert.notNull(elementType, "elementType");
        this.name = name;
        this.elementType = elementType;
    }

    @Override
    public SequenceType resolve(CollectionData parent) throws IOException {
        int elementCount = resolveElementCount(parent);
        return TypeBuilder.SEQUENCE(elementType, elementCount);
    }

    protected abstract int resolveElementCount(CollectionData parent) throws IOException;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public final int getSize() {
        return -1;
    }

    @Override
    public final Type getElementType() {
        return elementType;
    }

    @Override
    public final int getElementCount() {
        return -1;
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
