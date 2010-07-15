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
package org.esa.beam.dataio.netcdf4.convention.cf;

import org.esa.beam.dataio.netcdf4.Nc4Constants;
import org.esa.beam.dataio.netcdf4.Nc4ReaderParameters;
import org.esa.beam.dataio.netcdf4.Nc4ReaderUtils;
import org.esa.beam.dataio.netcdf4.convention.HeaderDataWriter;
import org.esa.beam.dataio.netcdf4.convention.Model;
import org.esa.beam.dataio.netcdf4.convention.ModelPart;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFileWriteable;

import java.io.IOException;

public class CfEndTimePart implements ModelPart {

    @Override
    public void read(Product p, Model model) throws IOException {
        p.setEndTime(getSceneRasterStopTime(model.getReaderParameters()));
    }

    @Override
    public void write(Product p, NetcdfFileWriteable ncFile, HeaderDataWriter hdw, Model model) throws IOException {
        final ProductData.UTC utc = p.getEndTime();
        if (utc != null) {
            ncFile.addAttribute(null, new Attribute(Nc4Constants.STOP_DATE_ATT_NAME, utc.format()));
        }
    }

    public static ProductData.UTC getSceneRasterStopTime(final Nc4ReaderParameters rv) {
        return Nc4ReaderUtils.getSceneRasterTime(rv.getGlobalAttributes(),
                                                 Nc4Constants.STOP_DATE_ATT_NAME,
                                                 Nc4Constants.STOP_TIME_ATT_NAME);
    }
}
