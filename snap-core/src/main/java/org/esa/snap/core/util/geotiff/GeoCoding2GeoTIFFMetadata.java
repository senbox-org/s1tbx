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

package org.esa.snap.core.util.geotiff;

import com.sun.media.imageio.plugins.tiff.GeoTIFFTagSet;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.MapGeoCoding;
import org.esa.snap.core.datamodel.PixelPos;
import org.geotools.coverage.grid.io.imageio.geotiff.CRS2GeoTiffMetadataAdapter;
import org.geotools.coverage.grid.io.imageio.geotiff.GeoKeyEntry;
import org.geotools.coverage.grid.io.imageio.geotiff.GeoTiffException;
import org.geotools.coverage.grid.io.imageio.geotiff.GeoTiffIIOMetadataEncoder;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import java.awt.geom.AffineTransform;

public class GeoCoding2GeoTIFFMetadata {

    private GeoCoding2GeoTIFFMetadata() {
    }

    public static GeoTIFFMetadata createGeoTIFFMetadata(GeoCoding geoCoding, int width, int height) {
        GeoTIFFMetadata metadata = null;
        if (geoCoding instanceof CrsGeoCoding || geoCoding instanceof MapGeoCoding) {
            metadata = createProjectedGeoTIFFMetadata(geoCoding.getMapCRS(), geoCoding.getImageToMapTransform());
        } else if (geoCoding != null) {
            metadata = createFallbackGeoTIFFMetada(geoCoding, width, height);
        }
        return metadata;
    }

    public static GeoTIFFMetadata createProjectedGeoTIFFMetadata(CoordinateReferenceSystem mapCRS,
                                                                 MathTransform imageToMapTransform) {
        GeoTIFFMetadata metadata = new GeoTIFFMetadata();
        try {
            final Integer epsgCode = CRS.lookupEpsgCode(mapCRS, true);
            if (epsgCode != null) {
                mapCRS = CRS.decode("EPSG:" + epsgCode);
            }
            final CRS2GeoTiffMetadataAdapter metadataAdapter = new CRS2GeoTiffMetadataAdapter(mapCRS);
            final GeoTiffIIOMetadataEncoder encoder = metadataAdapter.parseCoordinateReferenceSystem();
            final int numGeoKeyEntries = encoder.getNumGeoKeyEntries();
            for (int i = 1; i < numGeoKeyEntries; i++) {  // skip version tag
                final GeoKeyEntry entry = encoder.getGeoKeyEntryAt(i);
                final int id = entry.getKeyID();
                if (entry.getTiffTagLocation() == GeoTIFFTagSet.TAG_GEO_ASCII_PARAMS) {
                    metadata.addGeoAscii(id, encoder.getGeoAsciiParam(id));
                } else if (entry.getTiffTagLocation() == GeoTIFFTagSet.TAG_GEO_DOUBLE_PARAMS) {
                    if (entry.getCount() > 1) {
                        final double[] doubles = encoder.getGeoDoubleParams(id);
                        metadata.addGeoDoubleParams(id, doubles);
                    } else {
                        final double value = encoder.getGeoDoubleParam(id);
                        metadata.addGeoDoubleParam(id, value);
                    }
                } else {
                    final int value = encoder.getGeoShortParam(id);
                    metadata.addGeoShortParam(id, value);
                }
            }
        } catch (FactoryException e) {
            e.printStackTrace();
        } catch (GeoTiffException e) {
            e.printStackTrace();
        }
        if (!metadata.hasGeoKeyEntry(GeoTIFFCodes.PCSCitationGeoKey)) {
            metadata.addGeoAscii(GeoTIFFCodes.PCSCitationGeoKey, mapCRS.getName().getCode());
        }
        if (imageToMapTransform instanceof AffineTransform) {
            final AffineTransform transform = (AffineTransform) imageToMapTransform;
            final double[] transformationMatrix = new double[16];
            transformationMatrix[0] = transform.getScaleX();
            transformationMatrix[1] = transform.getShearX();
            transformationMatrix[3] = transform.getTranslateX();
            transformationMatrix[4] = transform.getShearY();
            transformationMatrix[5] = transform.getScaleY();
            transformationMatrix[7] = transform.getTranslateY();
            transformationMatrix[15] = 1;

            metadata.setModelTransformation(transformationMatrix);
        }
        metadata.addGeoShortParam(GeoTIFFCodes.GTRasterTypeGeoKey, GeoTIFFCodes.RasterPixelIsArea);
        metadata.addGeoAscii(GeoTIFFCodes.GTCitationGeoKey, mapCRS.getName().getCode());
        return metadata;
    }


    public static GeoTIFFMetadata createFallbackGeoTIFFMetada(GeoCoding geoCoding, int width, int height) {
        GeoTIFFMetadata metadata = new GeoTIFFMetadata();
        metadata.addGeoShortParam(GeoTIFFCodes.GTModelTypeGeoKey, GeoTIFFCodes.ModelTypeGeographic);
        metadata.addGeoShortParam(GeoTIFFCodes.GTRasterTypeGeoKey, GeoTIFFCodes.RasterPixelIsArea);
        metadata.addGeoShortParam(GeoTIFFCodes.GeographicTypeGeoKey, EPSGCodes.GCS_WGS_84);
        final int numTotMax = 128;
        int numHor = (int) Math.sqrt(numTotMax * ((double) width / (double) height));
        if (numHor < 2) {
            numHor = 2;
        }
        int numVer = numTotMax / numHor;
        if (numVer < 2) {
            numVer = 2;
        }
        final GeoPos geoPos = new GeoPos();
        final PixelPos pixelPos = new PixelPos();
        for (int y = 0; y < numVer; y++) {
            for (int x = 0; x < numHor; x++) {
                pixelPos.setLocation(((width - 1) * (double) x / (numHor - 1.0f)) + 0.5,
                                     ((height - 1) * (double) y / (numVer - 1.0f)) + 0.5);
                geoCoding.getGeoPos(pixelPos, geoPos);
                metadata.addModelTiePoint(pixelPos.x, pixelPos.y, geoPos.lon, geoPos.lat);
            }
        }

        GeoPos geoPos1 = geoCoding.getGeoPos(new PixelPos(0.5f, 0.5f), null);
        GeoPos geoPos2 = geoCoding.getGeoPos(new PixelPos(1.5f, 1.5f), null);
        final double scaleX = Math.abs(geoPos2.lon - geoPos1.lon);
        final double scaleY = Math.abs(geoPos2.lat - geoPos1.lat);
        metadata.setModelPixelScale(scaleX, scaleY);
        return metadata;
    }

}
