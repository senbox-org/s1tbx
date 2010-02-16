/*
 * $Id: ModisSpectralInfo.java,v 1.1 2008/05/28 12:18:03 sabinee Exp $
 *
 * Copyright (C) 2002,2003  by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
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
