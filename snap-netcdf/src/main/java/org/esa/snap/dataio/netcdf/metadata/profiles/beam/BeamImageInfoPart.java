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
package org.esa.snap.dataio.netcdf.metadata.profiles.beam;

import org.esa.snap.core.dataio.ProductIOException;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ColorPaletteDef;
import org.esa.snap.core.datamodel.ImageInfo;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.dataio.netcdf.ProfileReadContext;
import org.esa.snap.dataio.netcdf.ProfileWriteContext;
import org.esa.snap.dataio.netcdf.metadata.ProfilePartIO;
import org.esa.snap.dataio.netcdf.nc.NFileWriteable;
import org.esa.snap.dataio.netcdf.nc.NVariable;
import org.esa.snap.dataio.netcdf.util.ReaderUtils;
import ucar.ma2.Array;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.awt.Color;
import java.io.IOException;

public class BeamImageInfoPart extends ProfilePartIO {

    public static final String COLOR_TABLE_SAMPLE_VALUES = "color_table_sample_values";
    public static final String COLOR_TABLE_RED_VALUES = "color_table_red_values";
    public static final String COLOR_TABLE_GREEN_VALUES = "color_table_green_values";
    public static final String COLOR_TABLE_BLUE_VALUES = "color_table_blue_values";

    @Override
    public void decode(ProfileReadContext ctx, Product p) throws IOException {
        NetcdfFile netcdfFile = ctx.getNetcdfFile();
        for (Band band : p.getBands()) {
            String variableName = ReaderUtils.getVariableName(band);
            Variable variable = netcdfFile.getRootGroup().findVariable(variableName);
            readImageInfo(variable, band);
        }
    }

    @Override
    public void preEncode(ProfileWriteContext ctx, Product p) throws IOException {
        NFileWriteable fileWriteable = ctx.getNetcdfFileWriteable();
        for (Band band : p.getBands()) {
            ImageInfo imageInfo = band.getImageInfo();
            if (imageInfo != null) {
                String variableName = ReaderUtils.getVariableName(band);
                NVariable variable = fileWriteable.findVariable(variableName);
                if (variable != null) {
                    writeImageInfo(imageInfo.getColorPaletteDef().getPoints(), variable);
                }
            }
        }
    }

    private static void writeImageInfo(ColorPaletteDef.Point[] points, NVariable variable) throws IOException {
        final double[] sampleValues = new double[points.length];
        final int[] redValues = new int[points.length];
        final int[] greenValues = new int[points.length];
        final int[] blueValues = new int[points.length];
        for (int i = 0; i < points.length; i++) {
            ColorPaletteDef.Point point = points[i];
            sampleValues[i] = point.getSample();
            redValues[i] = point.getColor().getRed();
            greenValues[i] = point.getColor().getGreen();
            blueValues[i] = point.getColor().getBlue();
        }
        variable.addAttribute(COLOR_TABLE_SAMPLE_VALUES, Array.factory(sampleValues));
        variable.addAttribute(COLOR_TABLE_RED_VALUES, Array.factory(redValues));
        variable.addAttribute(COLOR_TABLE_GREEN_VALUES, Array.factory(greenValues));
        variable.addAttribute(COLOR_TABLE_BLUE_VALUES, Array.factory(blueValues));
    }

    private static void readImageInfo(Variable variable, Band band) throws ProductIOException {
        final Attribute sampleValues = variable.findAttributeIgnoreCase(COLOR_TABLE_SAMPLE_VALUES);
        final Attribute redValues = variable.findAttributeIgnoreCase(COLOR_TABLE_RED_VALUES);
        final Attribute greenValues = variable.findAttributeIgnoreCase(COLOR_TABLE_GREEN_VALUES);
        final Attribute blueValues = variable.findAttributeIgnoreCase(COLOR_TABLE_BLUE_VALUES);
        final Attribute[] attributes = {sampleValues, redValues, greenValues, blueValues};

        if (allAttributesAreNotNullAndHaveTheSameSize(attributes)) {
            final ColorPaletteDef.Point[] points = new ColorPaletteDef.Point[sampleValues.getLength()];
            for (int i = 0; i < points.length; i++) {
                final int red = redValues.getNumericValue(i).intValue();
                final int green = greenValues.getNumericValue(i).intValue();
                final int blue = blueValues.getNumericValue(i).intValue();
                final Color color = new Color(red, green, blue);
                points[i] = new ColorPaletteDef.Point(sampleValues.getNumericValue(i).doubleValue(), color);

            }
            band.setImageInfo(new ImageInfo(new ColorPaletteDef(points)));
        }
    }

    static boolean allElementsAreNotNull(final Object[] array) {
        if (array != null) {
            for (Object o : array) {
                if (o == null) {
                    return false;
                }
            }
        }
        return true;
    }

    static boolean allAttributesAreNotNullAndHaveTheSameSize(final Attribute[] attributes) {
        if (allElementsAreNotNull(attributes)) {
            final Attribute prim = attributes[0];
            for (int i = 1; i < attributes.length; i++) {
                if (prim.getLength() != attributes[i].getLength()) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
