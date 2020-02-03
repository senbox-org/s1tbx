/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.dataio.netcdf.nc;

import org.esa.snap.core.datamodel.ProductData;
import ucar.ma2.Array;
import ucar.ma2.DataType;

import java.io.IOException;

/**
 * An abstraction of the netcdf3/4 writing API.
 *
 * @author MarcoZ
 */
public interface NVariable {

    String getName();

    DataType getDataType();

    void addAttribute(String name, String value) throws IOException;

    void addAttribute(String name, Number value) throws IOException;

    void addAttribute(String name, Number value, boolean unsigned) throws IOException;

    void addAttribute(String name, Array value) throws IOException;

    void writeFully(Array values) throws IOException;

    void write(int x, int y, int width, int height, boolean isYFlipped, ProductData data) throws IOException;

}
