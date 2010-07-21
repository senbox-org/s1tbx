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

/**
 * A sequence of elements of same type.
 */
public interface SequenceData extends CollectionData {

    /**
     * @return The resolved instance type of the sequence.
     */
    @Override
    SequenceType getType();

    /**
     * @return The resolved instance type of the sequence.
     *
     * @deprecated since ceres 0.10; use {@link #getType()} instead.
     */
    @Deprecated
    SequenceType getSequenceType();

    // todo - get<type>s(index, array, off, len)
    // todo - set<type>s(index, array, off, len)
}
