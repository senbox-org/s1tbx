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
import org.esa.beam.dataio.netcdf.util.ReaderUtils;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.IndexCoding;
import org.esa.beam.framework.datamodel.Product;
import ucar.ma2.Array;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.IOException;

public class CfIndexCodingPart extends ProfilePart {

    private static final String FLAG_VALUES = "flag_values";
    private static final String FLAG_MEANINGS = "flag_meanings";


    @Override
    public void read(ProfileReadContext ctx, Product p) throws IOException {
        final Band[] bands = p.getBands();
        for (Band band : bands) {
            Variable variable = ctx.getNetcdfFile().findVariable(NetcdfFile.escapeName(band.getName()));
            final IndexCoding indexCoding = readIndexCoding(variable, band.getName());
            if (indexCoding != null) {
                p.getIndexCodingGroup().add(indexCoding);
                band.setSampleCoding(indexCoding);
            }
        }
    }

    @Override
    public void define(ProfileWriteContext ctx, Product p) throws IOException {
        NetcdfFileWriteable writeable = ctx.getNetcdfFileWriteable();
        for (Band band : p.getBands()) {
            IndexCoding indexCoding = band.getIndexCoding();
            if (indexCoding != null) {
                String variableName = ReaderUtils.getVariableName(band);
                Variable variable = writeable.getRootGroup().findVariable(variableName);
                writeIndexCoding(indexCoding, variable);
            }
        }
    }

    public static void writeIndexCoding(IndexCoding indexCoding, Variable variable) {
        final String[] indexNames = indexCoding.getIndexNames();
        final int[] indexValues = new int[indexNames.length];
        final StringBuffer meanings = new StringBuffer();
        for (int i = 0; i < indexValues.length; i++) {
            String name = indexNames[i];
            meanings.append(name).append(" ");
            indexValues[i] = indexCoding.getIndexValue(name);
        }
        variable.addAttribute(new Attribute(FLAG_MEANINGS, meanings.toString().trim()));
        variable.addAttribute(new Attribute(FLAG_VALUES, Array.factory(indexValues)));
    }

    public static IndexCoding readIndexCoding(Variable variable, String indexCodingName) {
        final Attribute flagValuesAtt = variable.findAttribute(FLAG_VALUES);
        final int[] flagValues;
        if (flagValuesAtt != null) {
            flagValues = new int[flagValuesAtt.getLength()];
            for (int i = 0; i < flagValues.length; i++) {
                flagValues[i] = flagValuesAtt.getNumericValue(i).intValue();
            }
        } else {
            flagValues = null;
        }

        final Attribute flagMeanings = variable.findAttribute(FLAG_MEANINGS);
        final String[] flagNames;
        if (flagMeanings != null) {
            flagNames = flagMeanings.getStringValue().split(" ");
        } else {
            flagNames = null;
        }

        return createIndexCoding(indexCodingName, flagValues, flagNames);
    }

    private static IndexCoding createIndexCoding(String indexCodingName, int[] flagValues, String[] flagNames) {
        if (flagValues != null && flagNames != null && flagValues.length == flagNames.length) {
            final IndexCoding coding = new IndexCoding(indexCodingName);
            for (int i = 0; i < flagValues.length; i++) {
                final String sampleName = flagNames[i];
                final int sampleValue = flagValues[i];
                coding.addSample(sampleName, sampleValue, "");
            }
            if (coding.getNumAttributes() > 0) {
                return coding;
            }
        }
        return null;
    }
}
