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
package org.esa.beam.dataio.modis.productdb;

public class ModisSpectralInfo {

    private final String _spectralWavelength;
    private final String _spectralBandwidth;
    private final String _spectralBandIndex;

    public ModisSpectralInfo(final String spectralWavelength,
                    final String spectralBandwidth,
                    final String spectralBandIndex) {
        _spectralWavelength = spectralWavelength;
        _spectralBandwidth = spectralBandwidth;
        _spectralBandIndex = spectralBandIndex;
    }

    public float getSpectralWavelength() {
        return Float.valueOf(_spectralWavelength);
    }

    public float getSpectralBandwidth() {
        return Float.valueOf(_spectralBandwidth);
    }

    public int getSpectralBandIndex() {
        return Integer.valueOf(_spectralBandIndex);
    }
}
