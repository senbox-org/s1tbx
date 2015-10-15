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

package org.esa.snap.dataio.netcdf.metadata.profiles.hdfeos;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.dataio.netcdf.ProfileReadContext;
import org.esa.snap.dataio.netcdf.ProfileWriteContext;
import org.esa.snap.dataio.netcdf.metadata.ProfilePartIO;
import org.esa.snap.dataio.netcdf.util.TimeUtils;
import org.jdom2.Element;

import java.io.IOException;


public class HdfEosTimePart extends ProfilePartIO {

    @Override
    public void decode(ProfileReadContext ctx, Product p) throws IOException {
        Element element = (Element) ctx.getProperty(HdfEosUtils.CORE_METADATA);
        if (element != null) {
            p.setStartTime(readEosTime(element, "RANGEBEGINNINGDATE", "RANGEBEGINNINGTIME"));
            p.setEndTime(readEosTime(element, "RANGEENDINGDATE", "RANGEENDINGTIME"));
        }
    }

    private ProductData.UTC readEosTime(Element element, String dateElemName, String timeElemName) {
        String date = HdfEosUtils.getValue(element, "INVENTORYMETADATA", "MASTERGROUP", "RANGEDATETIME",
                                           dateElemName, "VALUE");
        String time = HdfEosUtils.getValue(element, "INVENTORYMETADATA", "MASTERGROUP", "RANGEDATETIME",
                                           timeElemName, "VALUE");
        if (date != null && !date.isEmpty() && time != null && !time.isEmpty()) {
            return TimeUtils.parseDateTime(date + " " + time);
        }
        return null;
    }

    @Override
    public void preEncode(ProfileWriteContext ctx, Product p) throws IOException {
        throw new IllegalStateException();
    }
}
