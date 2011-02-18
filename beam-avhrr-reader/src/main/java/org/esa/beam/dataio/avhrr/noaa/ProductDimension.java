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

package org.esa.beam.dataio.avhrr.noaa;

/**
 * Differentiates between the different product dimensions (HRPT and GAC)
 */
enum ProductDimension {
    HRPT(2001, 2048, 512, 25, 40),
    GAC(401, 409, 104, 5, 8);

    private final int productWidth;
    private final int dataWidth;
    private final int cloudBytes;
    private final int tpTrimX;
    private final int tpSubsampling;

    private ProductDimension(int productWidth, int dataWidth, int cloudBytes, int tpTrimX, int tpSubsampling) {
        this.productWidth = productWidth;
        this.dataWidth = dataWidth;
        this.cloudBytes = cloudBytes;
        this.tpTrimX = tpTrimX;
        this.tpSubsampling = tpSubsampling;
    }

    public int getProductWidth() {
        return productWidth;
    }

    public int getDataWidth() {
        return dataWidth;
    }

    public int getCloudBytes() {
        return cloudBytes;
    }

    public int getTpTrimX() {
        return tpTrimX;
    }

    public int getTpSubsampling() {
        return tpSubsampling;
    }
}
