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
import org.esa.beam.dataio.netcdf.util.ReaderUtils;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import ucar.nc2.Attribute;

import java.io.IOException;

public class CfStartTimePart extends ProfilePart {

    @Override
    public void read(ProfileReadContext ctx, Product p) throws IOException {
        p.setStartTime(getSceneRasterStartTime(ctx));
    }

    @Override
    public void define(ProfileWriteContext ctx, Product p) throws IOException {
        final ProductData.UTC utc = p.getStartTime();
        if (utc != null) {
            ctx.getNetcdfFileWriteable().addAttribute(null, new Attribute(Constants.START_DATE_ATT_NAME, utc.format()));
        }
    }

    public static ProductData.UTC getSceneRasterStartTime(final ProfileReadContext ctx) {
        return ReaderUtils.getSceneRasterTime(ctx.getNetcdfFile(),
                                              Constants.START_DATE_ATT_NAME,
                                              Constants.START_TIME_ATT_NAME);
    }
}
