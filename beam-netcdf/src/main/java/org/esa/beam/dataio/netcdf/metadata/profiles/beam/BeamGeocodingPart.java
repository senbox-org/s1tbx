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
package org.esa.beam.dataio.netcdf.metadata.profiles.beam;

import org.esa.beam.dataio.netcdf.ProfileReadContext;
import org.esa.beam.dataio.netcdf.ProfileWriteContext;
import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfGeocodingPart;
import org.esa.beam.dataio.netcdf.util.Constants;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGeoCoding;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.util.StringUtils;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.IOException;

public class BeamGeocodingPart extends CfGeocodingPart {

    public static final String TIEPOINT_COORDINATES = "tiepoint_coordinates";

    private final int lonIndex = 0;
    private final int latIndex = 1;

    @Override
    public void decode(ProfileReadContext ctx, Product p) throws IOException {
        NetcdfFile netcdfFile = ctx.getNetcdfFile();
        final Attribute tpCoordinatesAtt = netcdfFile.findGlobalAttribute(TIEPOINT_COORDINATES);
        GeoCoding geoCoding = null;
        if (tpCoordinatesAtt != null) {
            final String[] tpGridNames = tpCoordinatesAtt.getStringValue().split(" ");
            if (tpGridNames.length == 2
                    && p.containsTiePointGrid(tpGridNames[lonIndex])
                    && p.containsTiePointGrid(tpGridNames[latIndex])) {
                final TiePointGrid lon = p.getTiePointGrid(tpGridNames[lonIndex]);
                final TiePointGrid lat = p.getTiePointGrid(tpGridNames[latIndex]);
                geoCoding = new TiePointGeoCoding(lat, lon);
            }
        } else {
            final Variable crsVar = netcdfFile.getRootGroup().findVariable("crs");
            if (crsVar != null) {
                final Attribute wktAtt = crsVar.findAttribute("wkt");
                final Attribute i2mAtt = crsVar.findAttribute("i2m");
                if (wktAtt != null && i2mAtt != null) {
                    geoCoding = createGeoCodingFromWKT(p, wktAtt.getStringValue(), i2mAtt.getStringValue());
                    ctx.setProperty(Constants.Y_FLIPPED_PROPERTY_NAME, true);
                }
            }
        }
        if (geoCoding != null) {
            p.setGeoCoding(geoCoding);
        } else {
            super.decode(ctx, p);
        }
    }

    private GeoCoding createGeoCodingFromWKT(Product p, String wktString, String i2mString) {
        try {
            CoordinateReferenceSystem crs = CRS.parseWKT(wktString);
            String[] parameters = StringUtils.csvToArray(i2mString);
            double[] matrix = new double[parameters.length];
            for (int i = 0; i < matrix.length; i++) {
                matrix[i] = Double.valueOf(parameters[i]);
            }
            AffineTransform i2m = new AffineTransform(matrix);
            Rectangle imageBounds = new Rectangle(p.getSceneRasterWidth(), p.getSceneRasterHeight());
            return new CrsGeoCoding(crs, imageBounds, i2m);
        } catch (FactoryException ignore) {
        } catch (TransformException ignore) {
        }
        return null;
    }

    @Override
    public void preEncode(ProfileWriteContext ctx, Product p) throws IOException {
        final GeoCoding geoCoding = p.getGeoCoding();
        if (geoCoding instanceof TiePointGeoCoding) {
            final TiePointGeoCoding tpGC = (TiePointGeoCoding) geoCoding;
            final String[] names = new String[2];
            names[lonIndex] = tpGC.getLonGrid().getName();
            names[latIndex] = tpGC.getLatGrid().getName();
            final String value = StringUtils.arrayToString(names, " ");
            ctx.getNetcdfFileWriteable().addAttribute(null, new Attribute(TIEPOINT_COORDINATES, value));
        } else {
            super.preEncode(ctx, p);
            if (geoCoding instanceof CrsGeoCoding) {
                addWktAsVariable(ctx.getNetcdfFileWriteable(), geoCoding);
            }
        }
    }

    @Override
    public void encode(ProfileWriteContext ctx, Product p) throws IOException {
        final GeoCoding geoCoding = p.getGeoCoding();
        if (!(geoCoding instanceof TiePointGeoCoding)) {
            super.encode(ctx, p);
        }
    }

    private void addWktAsVariable(NetcdfFileWriteable ncFile, GeoCoding geoCoding) {
        final CoordinateReferenceSystem crs = geoCoding.getMapCRS();
        final double[] matrix = new double[6];
        final MathTransform transform = geoCoding.getImageToMapTransform();
        if (transform instanceof AffineTransform) {
            ((AffineTransform) transform).getMatrix(matrix);
        }
        final Variable crsVariable = ncFile.addVariable(null, "crs", DataType.INT, null);
        crsVariable.addAttribute(new Attribute("wkt", crs.toWKT()));
        crsVariable.addAttribute(new Attribute("i2m", StringUtils.arrayToCsv(matrix)));
    }
}
