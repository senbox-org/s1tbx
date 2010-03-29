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

import org.esa.beam.dataio.netcdf4.Nc4Constants;
import org.esa.beam.dataio.netcdf4.Nc4ReaderParameters;
import org.esa.beam.dataio.netcdf4.convention.HeaderDataWriter;
import org.esa.beam.dataio.netcdf4.convention.ModelPart;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ColorPaletteDef;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.Product;
import ucar.ma2.Array;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.awt.Color;
import java.io.IOException;
import java.util.List;

import static org.esa.beam.dataio.netcdf4.Nc4ReaderUtils.*;

public class BeamImageInfoPart implements ModelPart {

    public static final String COLOR_TABLE_SAMPLE_VALUES = "color_table_sample_values";
    public static final String COLOR_TABLE_RED_VALUES = "color_table_red_values";
    public static final String COLOR_TABLE_GREEN_VALUES = "color_table_green_values";
    public static final String COLOR_TABLE_BLUE_VALUES = "color_table_blue_values";

    @Override
    public void read(Product p, Nc4ReaderParameters rp) throws IOException {
        final List<Variable> variableList = rp.getNetcdfFile().getVariables();
        for (Variable variable : variableList) {
            readImageInfo(p, variable);
        }
    }

    @Override
    public void write(Product p, NetcdfFileWriteable ncFile, HeaderDataWriter hdw) throws IOException {
        final Band[] bands = p.getBands();
        for (Band band : bands) {
            writeImageInfo(ncFile, band);
        }
    }

    public static void writeImageInfo(NetcdfFileWriteable ncFile, Band band) {
        final ImageInfo imageInfo = band.getImageInfo();
        if (imageInfo != null) {
            final Variable variable = ncFile.getRootGroup().findVariable(band.getName());
            final ColorPaletteDef.Point[] points = imageInfo.getColorPaletteDef().getPoints();
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
            variable.addAttribute(new Attribute(COLOR_TABLE_SAMPLE_VALUES, Array.factory(sampleValues)));
            variable.addAttribute(new Attribute(COLOR_TABLE_RED_VALUES, Array.factory(redValues)));
            variable.addAttribute(new Attribute(COLOR_TABLE_GREEN_VALUES, Array.factory(greenValues)));
            variable.addAttribute(new Attribute(COLOR_TABLE_BLUE_VALUES, Array.factory(blueValues)));
        }
    }

    public static void readImageInfo(Product p, Variable variable) throws ProductIOException {
        final Attribute sampleValues = variable.findAttributeIgnoreCase(COLOR_TABLE_SAMPLE_VALUES);
        final Attribute redValues = variable.findAttributeIgnoreCase(COLOR_TABLE_RED_VALUES);
        final Attribute greenValues = variable.findAttributeIgnoreCase(COLOR_TABLE_GREEN_VALUES);
        final Attribute blueValues = variable.findAttributeIgnoreCase(COLOR_TABLE_BLUE_VALUES);
        if (sampleValues == null && redValues == null && greenValues == null && blueValues == null) {
            return;
        }
        final Attribute[] attributes = {sampleValues, redValues, greenValues, blueValues};
        if (!allAttributesAreNotNullAndHaveTheSameSize(attributes)) {
            throw new ProductIOException(Nc4Constants.EM_INVALID_COLOR_TABLE);
        }
        final ColorPaletteDef.Point[] points = new ColorPaletteDef.Point[sampleValues.getLength()];
        for (int i = 0; i < points.length; i++) {
            final int red = redValues.getNumericValue(i).intValue();
            final int green = greenValues.getNumericValue(i).intValue();
            final int blue = blueValues.getNumericValue(i).intValue();
            final Color color = new Color(red, green, blue);
            points[i] = new ColorPaletteDef.Point(sampleValues.getNumericValue(i).doubleValue(), color);

        }
        final Band band = p.getBand(variable.getName());
        band.setImageInfo(new ImageInfo(new ColorPaletteDef(points)));
    }
}
