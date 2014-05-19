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
package org.esa.beam.dataio.avhrr;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.ProductData;

import java.io.IOException;

public interface BandReader {

    public String getBandName();

    public String getBandUnit();

    public String getBandDescription();

    public double getScalingFactor();

    public int getDataType();

    public void readBandRasterData(int sourceOffsetX, int sourceOffsetY,
                                   int sourceWidth, int sourceHeight, int sourceStepX,
                                   int sourceStepY, ProductData destBuffer,
                                   ProgressMonitor pm)
            throws IOException;
}
