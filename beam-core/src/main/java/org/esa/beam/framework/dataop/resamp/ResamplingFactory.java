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

package org.esa.beam.framework.dataop.resamp;

/**
 * A factory class for creating instances of {@link Resampling}
 *
 * @author Marco Peters
 */
public final class ResamplingFactory {

    public static final String NEAREST_NEIGHBOUR_NAME = "NEAREST_NEIGHBOUR";
    public static final String BILINEAR_INTERPOLATION_NAME = "BILINEAR_INTERPOLATION";
    public static final String CUBIC_CONVOLUTION_NAME = "CUBIC_CONVOLUTION";


    /**
     * Creates an instance of {@link Resampling} by using the given name.
     *
     * @param resamplingName the name of the resampling
     *
     * @return an instance of {@link Resampling}, or <code>null</code> if the given name is unknown.
     *
     * @see ResamplingFactory#NEAREST_NEIGHBOUR_NAME
     * @see ResamplingFactory#BILINEAR_INTERPOLATION_NAME
     * @see ResamplingFactory#CUBIC_CONVOLUTION_NAME
     */
    public static Resampling createResampling(final String resamplingName) {

        if (resamplingName.equals(NEAREST_NEIGHBOUR_NAME)) {
            return Resampling.NEAREST_NEIGHBOUR;
        } else if (resamplingName.equals(BILINEAR_INTERPOLATION_NAME)) {
            return Resampling.BILINEAR_INTERPOLATION;
        } else if (resamplingName.equals(CUBIC_CONVOLUTION_NAME)) {
            return Resampling.CUBIC_CONVOLUTION;
        } else {
            return null;
        }
    }

}
