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

import com.bc.ceres.binio.Type;

public abstract class AbstractType implements Type {

    protected AbstractType() {
    }

    @Override
    public boolean isSizeKnown() {
        return getSize() >= 0;
    }

    @Override
    public boolean isSimpleType() {
        return false;
    }

    @Override
    public boolean isCollectionType() {
        return isCompoundType() || isSequenceType();
    }

    @Override
    public boolean isSequenceType() {
        return false;
    }

    @Override
    public boolean isCompoundType() {
        return false;
    }

    @Override
    public final int hashCode() {
        return super.hashCode();
    }

    @Override
    public final boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return getClass().getName() + "[name=" + getName() + ",size=" + getSize() + "]";
    }
}
