/*
 * $Id: $
 *
 * Copyright (C) 2007 by Brockmann Consult (info@brockmann-consult.de)
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
package org.esa.beam.collocation;

import org.esa.beam.framework.dataop.resamp.Resampling;

public enum ResamplingType {
    NEAREST_NEIGHBOUR(Resampling.NEAREST_NEIGHBOUR), 
    BILINEAR_INTERPOLATION(Resampling.BILINEAR_INTERPOLATION), 
    CUBIC_CONVOLUTION(Resampling.CUBIC_CONVOLUTION);
    
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
