/*
 * $Id$
 *
 * Copyright (C) 2010 by Brockmann Consult (info@brockmann-consult.de)
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
package org.esa.beam.dataio.netcdf4.convention.beam;

import org.esa.beam.dataio.netcdf4.Nc4ReaderParameters;
import org.esa.beam.dataio.netcdf4.convention.HeaderDataWriter;
import org.esa.beam.dataio.netcdf4.convention.ModelPart;
import org.esa.beam.dataio.netcdf4.convention.cf.CfGeocodingPart;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGeoCoding;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.util.StringUtils;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFileWriteable;

import java.io.IOException;

public class BeamGeocodingPart implements ModelPart {

    public static final String TIEPOINT_COORDINATES = "tiepoint_coordinates";

    private final int lonIndex = 0;
    private final int latIndex = 1;

    @Override
    public void read(Product p, Nc4ReaderParameters rp) throws IOException {
        final Attribute tpCoordinatesAtt = rp.getNetcdfFile().findGlobalAttribute(TIEPOINT_COORDINATES);
        if (tpCoordinatesAtt == null) {
            CfGeocodingPart.readGeocoding(p, rp);
        }
        final String[] tpGridNames = tpCoordinatesAtt.getStringValue().split(" ");
        if (tpGridNames.length != 2
            || !p.containsTiePointGrid(tpGridNames[lonIndex])
            || !p.containsTiePointGrid(tpGridNames[latIndex])) {
            throw new ProductIOException("Illegal values in global attribute '" + TIEPOINT_COORDINATES + "'");
        }
        final TiePointGrid lon = p.getTiePointGrid(tpGridNames[lonIndex]);
        final TiePointGrid lat = p.getTiePointGrid(tpGridNames[latIndex]);
        final TiePointGeoCoding tiePointGeoCoding = new TiePointGeoCoding(lat, lon);
        p.setGeoCoding(tiePointGeoCoding);
    }

    @Override
    public void write(Product p, NetcdfFileWriteable ncFile, HeaderDataWriter hdw) throws IOException {
        final GeoCoding geoCoding = p.getGeoCoding();
        if (geoCoding instanceof TiePointGeoCoding) {
            final TiePointGeoCoding tpGC = (TiePointGeoCoding) geoCoding;
            final String[] names = new String[2];
            names[lonIndex] = tpGC.getLonGrid().getName();
            names[latIndex] = tpGC.getLatGrid().getName();
            final String value = StringUtils.arrayToString(names, " ");
            ncFile.addAttribute(null, new Attribute(TIEPOINT_COORDINATES, value));
        }
    }
}
