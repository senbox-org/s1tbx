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
package org.esa.snap.collocation;

import org.esa.snap.core.dataop.resamp.Resampling;

public enum ResamplingType {
    NEAREST_NEIGHBOUR(Resampling.NEAREST_NEIGHBOUR), 
    BILINEAR_INTERPOLATION(Resampling.BILINEAR_INTERPOLATION), 
    CUBIC_CONVOLUTION(Resampling.CUBIC_CONVOLUTION),
    BISINC_CONVOLUTION(Resampling.BISINC_5_POINT_INTERPOLATION),
    BICUBIC_CONVOLUTION(Resampling.BICUBIC_INTERPOLATION);

    private Resampling resampling;
    
    private ResamplingType(Resampling resampling) {
        this.resampling = resampling;
    }
    
    public Resampling getResampling() {
        return resampling;
    }
    
    @Override
    public String toString() {
        return resampling.toString();
    }

    public static String[] valuesAsString() {
        ResamplingType[] values = values();
        String[] stringValues = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            stringValues[i] = values[i].toString();
        }
        return stringValues;
    }
}
