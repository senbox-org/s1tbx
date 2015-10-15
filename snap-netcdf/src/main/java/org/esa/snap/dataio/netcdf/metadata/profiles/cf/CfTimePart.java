/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.dataio.netcdf.metadata.profiles.cf;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.dataio.netcdf.ProfileReadContext;
import org.esa.snap.dataio.netcdf.ProfileWriteContext;
import org.esa.snap.dataio.netcdf.metadata.ProfilePartIO;
import org.esa.snap.dataio.netcdf.nc.NFileWriteable;
import org.esa.snap.dataio.netcdf.util.Constants;
import org.esa.snap.dataio.netcdf.util.TimeUtils;
import ucar.nc2.NetcdfFile;

import java.io.IOException;

public class CfTimePart extends ProfilePartIO {

    @Override
    public void decode(ProfileReadContext ctx, Product p) throws IOException {
        NetcdfFile ncFile = ctx.getNetcdfFile();
        p.setStartTime(
                TimeUtils.getSceneRasterTime(ncFile, Constants.START_DATE_ATT_NAME, Constants.START_TIME_ATT_NAME));
        p.setEndTime(TimeUtils.getSceneRasterTime(ncFile, Constants.STOP_DATE_ATT_NAME, Constants.STOP_TIME_ATT_NAME));
    }

    @Override
    public void preEncode(ProfileWriteContext ctx, Product p) throws IOException {
        ProductData.UTC utc = p.getStartTime();
        NFileWriteable writeable = ctx.getNetcdfFileWriteable();
        if (utc != null) {
            writeable.addGlobalAttribute(Constants.START_DATE_ATT_NAME, utc.format());
        }
        utc = p.getEndTime();
        if (utc != null) {
            writeable.addGlobalAttribute(Constants.STOP_DATE_ATT_NAME, utc.format());
        }
    }
}
