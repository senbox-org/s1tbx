/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.dataio.geotiff;

import com.sun.media.jai.codec.TIFFField;
import it.geosolutions.imageio.plugins.tiff.GeoTIFFTagSet;
import org.esa.snap.core.datamodel.FilterBand;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.util.geotiff.GeoTIFFMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class Utils {

    public static final int PRIVATE_BEAM_TIFF_TAG_NUMBER = 65000;

    public static List<TIFFField> createGeoTIFFFields(GeoTIFFMetadata geoTIFFMetadata) {
        final List<TIFFField> list = new ArrayList<TIFFField>(6);
        // Geo Key Directory
        final int numGeoKeyEntries = geoTIFFMetadata.getNumGeoKeyEntries();
        if (numGeoKeyEntries > 0) {
            final GeoTIFFMetadata.KeyEntry[] keyEntries = geoTIFFMetadata.getGeoKeyEntries();
            final char[] values = new char[keyEntries.length * 4];
            for (int i = 0; i < keyEntries.length; i++) {
                final int[] data = keyEntries[i].getData();
                for (int j = 0; j < data.length; j++) {
                    values[i * 4 + j] = (char) data[j];
                }
            }
            final TIFFField geoKeyDirectory = new TIFFField(GeoTIFFTagSet.TAG_GEO_KEY_DIRECTORY,
                                                            TIFFField.TIFF_SHORT,
                                                            values.length,
                                                            values);
            list.add(geoKeyDirectory);
        }


        // Geo Double Params
        final double[] geoDoubleParams = geoTIFFMetadata.getGeoDoubleParams();
        if (geoDoubleParams != null && geoDoubleParams.length>0){
            list.add(new TIFFField(GeoTIFFTagSet.TAG_GEO_DOUBLE_PARAMS,
                                   TIFFField.TIFF_DOUBLE,
                                   geoDoubleParams.length,
                                   geoDoubleParams));
        }

        // Geo ASCII Params
        final String geoAsciiString = geoTIFFMetadata.getGeoAsciiParams();
        if (geoAsciiString != null && geoAsciiString.length() > 0) {
            final StringTokenizer tokenizer = new StringTokenizer(geoAsciiString, "|");
            final String[] geoAsciiStrings = new String[tokenizer.countTokens()];
            for (int i = 0; i < geoAsciiStrings.length; i++) {
                geoAsciiStrings[i] = tokenizer.nextToken().concat("|");
            }
            list.add(new TIFFField(GeoTIFFTagSet.TAG_GEO_ASCII_PARAMS,
                                   TIFFField.TIFF_ASCII,
                                   geoAsciiStrings.length,
                                   geoAsciiStrings));
        }

        // Model Pixel Scale
        final double[] modelPixelScale = geoTIFFMetadata.getModelPixelScale();
        if (Utils.isValidModelPixelScale(modelPixelScale)) {
            list.add(new TIFFField(GeoTIFFTagSet.TAG_MODEL_PIXEL_SCALE,
                                   TIFFField.TIFF_DOUBLE,
                                   modelPixelScale.length,
                                   modelPixelScale));
        }

        // Model Tie Point
        final int numModelTiePoints = geoTIFFMetadata.getNumModelTiePoints();
        if (numModelTiePoints > 0) {
            final double[] tiePointValues = new double[numModelTiePoints * 6];
            for (int i = 0; i < numModelTiePoints; i++) {
                final GeoTIFFMetadata.TiePoint tiePoint = geoTIFFMetadata.getModelTiePointAt(i);
                final double[] data = tiePoint.getData();
                System.arraycopy(data, 0, tiePointValues, i * data.length, data.length);
            }

            list.add(new TIFFField(GeoTIFFTagSet.TAG_MODEL_TIE_POINT,
                                   TIFFField.TIFF_DOUBLE,
                                   tiePointValues.length,
                                   tiePointValues));
        }

        // Model Transformation
        final double[] modelTransformation = geoTIFFMetadata.getModelTransformation();

        if (isValidModelTransformation(modelTransformation)) {
            list.add(new TIFFField(GeoTIFFTagSet.TAG_MODEL_TRANSFORMATION,
                                   TIFFField.TIFF_DOUBLE,
                                   modelTransformation.length,
                                   modelTransformation));
        }

        return list;
    }

    public static boolean isValidModelTransformation(double[] modelTransformation) {
        final double[] defaultValues = new double[16];
        return isValidData(modelTransformation, defaultValues);
    }

    public static boolean isValidModelPixelScale(double[] modelTransformation) {
        final double[] defaultValues = {1, 1, 0};
        return isValidData(modelTransformation, defaultValues);
    }

    private static boolean isValidData(double[] modelTransformation, double[] defaultValues) {
        if (modelTransformation != null && modelTransformation.length == defaultValues.length) {
            for (int i = 0; i < modelTransformation.length; i++) {
                double v = modelTransformation[i];
                final double dv = defaultValues[i];
                if (v != dv) {
                    return true;
                }
            }
        }
        return false;
    }

    static String[] findSuitableLatLonNames(Product product) {
        final String[] latNames = {"latitude", "latitude_tpg", "lat", "lat_tpg"};
        final String[] lonNames = {"longitude", "longitude_tpg", "lon", "lon_tpg"};
        String[] names = new String[2];
        for (int i = 0; i < latNames.length; i++) {
            String latName = latNames[i];
            String lonName = lonNames[i];
            if (!product.containsRasterDataNode(latName) && !product.containsRasterDataNode(lonName)) {
                names[0] = latName;
                names[1] = lonName;
                return names;
            }
        }
        String lonName = lonNames[0] + "_";
        String latName = latNames[0] + "_";
        int index = 1;
        while (product.containsRasterDataNode(latName + index) || product.containsRasterDataNode(lonName + index)) {
            index++;
        }
        return new String[]{latName + index, lonName + index};
    }

    public static boolean shouldWriteNode(ProductNode node) {
        if (node instanceof VirtualBand) {
            return false;
        } else if (node instanceof FilterBand) {
            return false;
        }
        return true;
    }
}
