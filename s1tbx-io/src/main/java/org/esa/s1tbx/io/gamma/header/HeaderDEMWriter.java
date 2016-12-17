/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.io.gamma.header;

import org.esa.s1tbx.io.gamma.GammaProductWriter;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Writer par header
 */
public class HeaderDEMWriter extends HeaderWriter {

    public HeaderDEMWriter(final GammaProductWriter writer, final Product srcProduct, final File userOutputFile) {
        super(writer, srcProduct, userOutputFile);
    }

    @Override
    public void writeParFile() throws IOException {
        final String oldEOL = System.getProperty("line.separator");
        System.setProperty("line.separator", "\n");
        final FileOutputStream out = new FileOutputStream(outputFile);
        try (final PrintStream p = new PrintStream(out)) {

            p.println(GammaConstants.HEADER_KEY_NAME + sep + srcProduct.getName());

            p.println(GammaConstants.HEADER_KEY_WIDTH + sep + srcProduct.getSceneRasterWidth());
            p.println(GammaConstants.HEADER_KEY_NLINES + sep + srcProduct.getSceneRasterHeight());
            p.println(GammaConstants.HEADER_KEY_DATA_FORMAT + sep + getDataType());

            final String demProjection = getDEMProjection();

            p.println(GammaConstants.HEADER_KEY_DEM_PROJECTION + sep + demProjection);
            p.println(GammaConstants.HEADER_KEY_DEM_HGT_OFFSET + sep + "0.0");
            p.println(GammaConstants.HEADER_KEY_DEM_SCALE + sep + "1.0");
            p.println(GammaConstants.HEADER_KEY_DEM_CORNER_NORTH + sep + "");
            p.println(GammaConstants.HEADER_KEY_DEM_CORNER_EAST + sep + "");
            p.println(GammaConstants.HEADER_KEY_DEM_POST_NORTH + sep + "");
            p.println(GammaConstants.HEADER_KEY_DEM_POST_EAST + sep + "");

            p.println(GammaConstants.HEADER_KEY_DATUM_NAME + sep + "");
            p.println(GammaConstants.HEADER_KEY_DATUM_SHIFT_DX + sep + "0.0");
            p.println(GammaConstants.HEADER_KEY_DATUM_SHIFT_DY + sep + "0.0");
            p.println(GammaConstants.HEADER_KEY_DATUM_SHIFT_DZ + sep + "0.0");
            p.println(GammaConstants.HEADER_KEY_DATUM_SCALE + sep + "0.0");
            p.println(GammaConstants.HEADER_KEY_DATUM_ROTATION_ALPHA + sep + "0.0");
            p.println(GammaConstants.HEADER_KEY_DATUM_ROTATION_BETA + sep + "0.0");
            p.println(GammaConstants.HEADER_KEY_DATUM_ROTATION_GAMMA + sep + "0.0");

            p.println(GammaConstants.HEADER_KEY_PROJECTION_NAME + sep + demProjection);
            if(demProjection.equals(GammaConstants.PROJECTION_UTM)) {
                p.println(GammaConstants.HEADER_KEY_PROJECTION_ZONE + sep + getUTMZone());
                p.println(GammaConstants.HEADER_KEY_PROJECTION_FALSE_EASTING + sep + "0.0");
                p.println(GammaConstants.HEADER_KEY_PROJECTION_FALSE_NORTHING + sep + "0.0");
                p.println(GammaConstants.HEADER_KEY_PROJECTION_K0 + sep + "0.0");
            }

            p.flush();
        } catch (Exception e) {
            throw new IOException("GammaWriter unable to write par file " + e.getMessage());
        } finally {
            System.setProperty("line.separator", oldEOL);
        }
    }

    private String getDEMProjection() {
        CoordinateReferenceSystem crs = srcProduct.getSceneCRS();
        ReferenceIdentifier id = crs.getName();
        if(id.getCode().contains(GammaConstants.PROJECTION_UTM)) {
            String wkt = crs.toWKT();
            //String easting = wkt.substring(wkt.indexOf("false_easting")+14);


            return GammaConstants.PROJECTION_UTM;
        }

        return "";
    }

    private String getUTMZone() {
        GeoPos centerPos = srcProduct.getSceneGeoCoding().getGeoPos(new PixelPos(srcProduct.getSceneRasterWidth()/2, srcProduct.getSceneRasterHeight()/2), null);

        int zone = (int) Math.floor(centerPos.getLon()/6+31);

        return String.valueOf(zone);
    }

    @Override
    protected String getDataType() {
        return "REAL*4";
    }
}
