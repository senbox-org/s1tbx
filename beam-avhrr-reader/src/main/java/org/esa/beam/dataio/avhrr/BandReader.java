/*
 * $Id: BandReader.java,v 1.3 2007/03/22 09:17:00 ralf Exp $
 *
 * Copyright (C) 2006 by Brockmann Consult (info@brockmann-consult.de)
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
package org.esa.beam.dataio.avhrr;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.ProductData;

import java.io.IOException;

public interface BandReader {

    public String getBandName();

    public String getBandUnit();

    public String getBandDescription();

    public float getScalingFactor();

    public int getDataType();

    public void readBandRasterData(int sourceOffsetX, int sourceOffsetY,
                                   int sourceWidth, int sourceHeight, int sourceStepX,
                                   int sourceStepY, ProductData destBuffer,
                                   ProgressMonitor pm)
            throws IOException;
}
