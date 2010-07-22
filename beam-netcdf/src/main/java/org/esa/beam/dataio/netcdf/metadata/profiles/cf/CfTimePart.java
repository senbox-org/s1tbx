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
package org.esa.beam.dataio.netcdf.metadata.profiles.cf;

import org.esa.beam.dataio.netcdf.metadata.ProfilePart;
import org.esa.beam.dataio.netcdf.metadata.ProfileReadContext;
import org.esa.beam.dataio.netcdf.metadata.ProfileWriteContext;
import org.esa.beam.dataio.netcdf.util.Constants;
import org.esa.beam.dataio.netcdf.util.TimeUtils;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;

import java.io.IOException;

public class CfTimePart extends ProfilePart {

    @Override
    public void read(ProfileReadContext ctx, Product p) throws IOException {
        NetcdfFile ncFile = ctx.getNetcdfFile();
        p.setStartTime(TimeUtils.getSceneRasterTime(ncFile, Constants.START_DATE_ATT_NAME, Constants.START_TIME_ATT_NAME));
        p.setEndTime(TimeUtils.getSceneRasterTime(ncFile, Constants.STOP_DATE_ATT_NAME, Constants.STOP_TIME_ATT_NAME));
    }

    @Override
    public void define(ProfileWriteContext ctx, Product p) throws IOException {
        ProductData.UTC utc = p.getStartTime();
        NetcdfFileWriteable writeable = ctx.getNetcdfFileWriteable();
        if (utc != null) {
            writeable.addAttribute(null, new Attribute(Constants.START_DATE_ATT_NAME, utc.format()));
        }
        utc = p.getEndTime();
        if (utc != null) {
            writeable.addAttribute(null, new Attribute(Constants.STOP_DATE_ATT_NAME, utc.format()));
        }
    }
}
