/*
 * $Id$
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.dataio.obpg.hdf;

import java.lang.reflect.Array;
import java.util.Arrays;

public class SdsInfo {

    private final int sdsID;
    private final String name;
    private final int hdfDataType;
    private final int numAttributes;
    private final int[] dimensions;

    public SdsInfo(final int sdsID, final String name, final int hdfDataType, final int numAttributes,
                   final int numDimensions, final int[] dimensions) {
        this.sdsID = sdsID;
        this.name = name;
        this.hdfDataType = hdfDataType;
        this.numAttributes = numAttributes;
        this.dimensions = Arrays.copyOf(dimensions,numDimensions);
    }

    public int getSdsID() {
        return sdsID;
    }

    public String getName() {
        return name;
    }

    public int getHdfDataType() {
        return hdfDataType;
    }

    public int getNumAttributes() {
        return numAttributes;
    }

    public int getNumDimensions() {
        return dimensions.length;
    }

    public int[] getDimensions() {
        return Arrays.copyOf(dimensions, dimensions.length);
    }
}
