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

import java.io.IOException;

/**
 * A factory for the netcdf3/4 writing API.
 *
 * @author MarcoZ
 */
public class NWritableFactory {

    public static NFileWriteable create(String filename, String format) throws IOException {
        if ("netcdf3".equalsIgnoreCase(format)) {
            return N3FileWriteable.create(filename);
        } else if ("netcdf4".equalsIgnoreCase(format)) {
            return N4FileWriteable.create(filename);
        } else {
            throw new IllegalArgumentException("Unsupported format: " +  format);
        }
    }
}
