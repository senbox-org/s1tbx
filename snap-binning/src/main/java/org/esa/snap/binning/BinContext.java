/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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


package org.esa.snap.binning;

/**
 * A context is valid for a single bin.
 *
 * @author Norman
 */
public interface BinContext {

    /**
     * @return The bin's unique index.
     */
    long getIndex();

    /**
     * Gets a named value from a temporary context. The context is valid for a
     * single bin. Values are shared between all aggregators operating on that bin.
     *
     * @param name The value name.
     * @param <T>  The value's type.
     * @return The value, may be {@code null}.
     */
    <T> T get(String name);

    /**
     * Sets a named value in a temporary context. The context is valid for a
     * single bin. Values are shared between all aggregators operating on that bin.
     *
     * @param name  The value name.
     * @param value The value.
     */
    void put(String name, Object value);
}
