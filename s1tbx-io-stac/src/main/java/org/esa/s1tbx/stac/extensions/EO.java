/*
 * Copyright (C) 2020 Skywatch Space Applications Inc. https://www.skywatch.com
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
package org.esa.s1tbx.stac.extensions;

import org.esa.snap.core.dataio.dimap.spi.DimapPersistable;
import org.esa.snap.core.dataio.dimap.spi.DimapPersistence;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.StringUtils;
import org.json.simple.JSONObject;

import java.awt.geom.AffineTransform;

// Electro-Optical Extension
@SuppressWarnings("unchecked")
public class EO {

    public final static String eo = "eo";

    //This is a list of the available bands where each item is a Band Object.
    public final static String bands = "eo:bands";

    //Estimate of cloud cover as a percentage (0-100) of the entire scene. If not available the field should not be provided.
    public final static String cloud_cover = "eo:cloud_cover";

    public final static String name = "name";
    public final static String common_name = "common_name";
    public final static String description = "description";
    public final static String center_wavelength = "center_wavelength";
    public final static String full_width_half_max = "full_width_half_max";

    //Common band names
    public final static String coastal = "coastal";
    public final static String blue = "blue";
    public final static String green = "green";
    public final static String red = "red";
    public final static String yellow = "yellow";
    public final static String pan = "pan";
    public final static String rededge = "rededge";
    public final static String nir = "nir";
    public final static String nir08 = "nir08";
    public final static String nir09 = "nir09";
    public final static String cirrus = "cirrus";
    public final static String swir16 = "swir16";
    public final static String swir22 = "swir22";
    public final static String lwir = "lwir";
    public final static String lwir11 = "lwir11";
    public final static String lwir12 = "lwir12";

    public static class KeyWords {
        public final static String earth_observation = "earth observation";
        public final static String satellite = "satellite";
        public final static String optical = "optical";
    }

    public static String getCommonName(final String name) {
        switch (name) {
            case "coastal":
                return coastal;
            case "red":
                return red;
            case "green":
                return green;
            case "blue":
                return blue;
            case "yellow":
                return yellow;
            case "pan":
            case "panchromatic":
                return pan;
            case "rededge":
            case "red_edge":
                return rededge;
            case "nir":
            case "near-infrared":
            case "nearinfrared":
                return nir;
            case "nir08":
                return nir08;
            case "nir09":
                return nir09;
            case "cirrus":
                return cirrus;
            case "swir16":
            case "swir1":
                return swir16;
            case "swir22":
            case "swir2":
                return swir22;
            case "lwir":
                return lwir;
            case "lwir11":
                return lwir11;
            case "lwir12":
                return lwir12;
            default:
                return name;
        }
    }

    public static JSONObject writeBand(final Band band) {
        final JSONObject bandJSON = new JSONObject();
        bandJSON.put(EO.name, band.getName());
        bandJSON.put(EO.common_name, EO.getCommonName(band.getName()));
        if(band.getDescription() != null) {
            bandJSON.put(EO.description, band.getDescription());
        }

        bandJSON.put(SNAP.raster_width, band.getRasterWidth());
        bandJSON.put(SNAP.raster_height, band.getRasterHeight());
        bandJSON.put(SNAP.data_type, ProductData.getTypeString(band.getDataType()));
        final String unit = band.getUnit();
        if (unit != null && unit.length() > 0) {
            bandJSON.put(SNAP.unit, unit);
        }
      /*  bandJSON.put(TAG_SOLAR_FLUX, band.getSolarFlux());
        if (band.getSpectralBandIndex() > -1) {
            bandJSON.put(TAG_SPECTRAL_BAND_INDEX, band.getSpectralBandIndex());
        }
        bandJSON.put(TAG_BAND_WAVELEN, band.getSpectralWavelength());
        bandJSON.put(TAG_BANDWIDTH, band.getSpectralBandwidth());
        final FlagCoding flagCoding = band.getFlagCoding();
        if (flagCoding != null) {
            bandJSON.put(TAG_FLAG_CODING_NAME, flagCoding.getName());
        }
        final IndexCoding indexCoding = band.getIndexCoding();
        if (indexCoding != null) {
            bandJSON.put(TAG_INDEX_CODING_NAME, indexCoding.getName());
        }
        bandJSON.put(TAG_SCALING_FACTOR, band.getScalingFactor());
        bandJSON.put(TAG_SCALING_OFFSET, band.getScalingOffset());
        bandJSON.put(TAG_SCALING_LOG_10, band.isLog10Scaled());*/
        bandJSON.put(SNAP.no_data_value_used, band.isNoDataValueUsed());
        bandJSON.put(SNAP.no_data_value, band.getNoDataValue());
        if (band instanceof VirtualBand) {
            final VirtualBand vb = (VirtualBand) band;
            bandJSON.put(SNAP.virtual_band, true);
            bandJSON.put(SNAP.expression, vb.getExpression());
        }
        final String validMaskExpression = band.getValidPixelExpression();
        if (validMaskExpression != null) {
            bandJSON.put(SNAP.valid_mask_term, validMaskExpression);
        }
        writeImageToModelTransform(band, bandJSON);

        return bandJSON;
    }

    public static Band readBand(final JSONObject bandJSON) {
        Band band = null;
        final String bandName = (String)bandJSON.get(name);

        final int rasterWidth = ((Long)bandJSON.get(SNAP.raster_width)).intValue();
        final int rasterHeight = ((Long)bandJSON.get(SNAP.raster_height)).intValue();

        final String descriptionStr = (String)bandJSON.get(description);
        final int type = ProductData.getType((String)bandJSON.get(SNAP.data_type));
        if (type == ProductData.TYPE_UNDEFINED) {
            return null;
        }
        if (bandJSON.containsKey(SNAP.virtual_band)) {
            final String expression = (String)bandJSON.get(SNAP.expression);
            final VirtualBand virtualBand = new VirtualBand(bandName, type, rasterWidth, rasterHeight, expression);
            virtualBand.setNoDataValue((Double)bandJSON.get(SNAP.no_data_value));
            virtualBand.setNoDataValueUsed((Boolean)bandJSON.get(SNAP.no_data_value_used));
            band = virtualBand;
        } else if (bandJSON.containsKey(SNAP.filter_band_info)) {
            final DimapPersistable persistable = DimapPersistence.getPersistable(bandJSON);
            if (persistable != null) {
                //band = (Band) persistable.createObjectFromXml(bandJSON, product);
                // currently it can be null if the operator of filtered band is of type
                // GeneralFilterBand.STDDEV or GeneralFilterBand.RMS
            }
        } else {
            band = new Band(bandName, type, rasterWidth, rasterHeight);
        }
        if (band != null) {
            band.setDescription(descriptionStr);
        }
        return band;
    }

    private static void writeImageToModelTransform(final RasterDataNode rasterDataNode, final JSONObject bandJSON) {
        final AffineTransform imageToModelTransform = rasterDataNode.getImageToModelTransform();
        if (!imageToModelTransform.isIdentity()) {
            final double[] matrix = new double[6];
            imageToModelTransform.getMatrix(matrix);
            bandJSON.put(SNAP.image_to_model_transform, StringUtils.arrayToCsv(matrix));
        }
    }
}
