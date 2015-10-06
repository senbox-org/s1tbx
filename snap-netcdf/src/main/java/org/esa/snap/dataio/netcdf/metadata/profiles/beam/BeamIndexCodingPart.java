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
import org.esa.snap.core.datamodel.IndexCoding;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.dataio.netcdf.ProfileReadContext;
import org.esa.snap.dataio.netcdf.ProfileWriteContext;
import org.esa.snap.dataio.netcdf.metadata.ProfilePartIO;
import org.esa.snap.dataio.netcdf.metadata.profiles.cf.CfIndexCodingPart;
import org.esa.snap.dataio.netcdf.nc.NFileWriteable;
import org.esa.snap.dataio.netcdf.nc.NVariable;
import org.esa.snap.dataio.netcdf.util.ReaderUtils;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;

import java.io.IOException;

public class BeamIndexCodingPart extends ProfilePartIO {

    public static final String INDEX_DESCRIPTIONS = "index_descriptions";
    public static final String DESCRIPTION_SEPARATOR = "\t";
    public static final String INDEX_CODING_NAME = "index_coding_name";

    @Override
    public void decode(ProfileReadContext ctx, Product p) throws IOException {
        final Band[] bands = p.getBands();
        for (Band band : bands) {
            String variableName = ReaderUtils.getVariableName(band);
            final Variable variable = ctx.getNetcdfFile().getRootGroup().findVariable(variableName);
            final IndexCoding indexCoding = readIndexCoding(variable, band.getName() + "_index_coding");
            if (indexCoding != null) {
                p.getIndexCodingGroup().add(indexCoding);
                band.setSampleCoding(indexCoding);
            }
        }
    }

    @Override
    public void preEncode(ProfileWriteContext ctx, Product p) throws IOException {
        NFileWriteable writeable = ctx.getNetcdfFileWriteable();
        for (Band band : p.getBands()) {
            IndexCoding indexCoding = band.getIndexCoding();
            if (indexCoding != null) {
                String variableName = ReaderUtils.getVariableName(band);
                NVariable variable = writeable.findVariable(variableName);
                writeIndexCoding(indexCoding, variable);
            }
        }
    }

    private void writeIndexCoding(IndexCoding indexCoding, NVariable variable) throws IOException {
        CfIndexCodingPart.writeIndexCoding(indexCoding, variable);
        final String[] indexNames = indexCoding.getIndexNames();
        final StringBuilder descriptions = new StringBuilder();
        for (String indexName : indexNames) {
            final MetadataAttribute index = indexCoding.getIndex(indexName);
            if (index != null) {
                final String description = index.getDescription();
                if (description != null) {
                    descriptions.append(description);
                }
            }
            descriptions.append(DESCRIPTION_SEPARATOR);
        }
        variable.addAttribute(INDEX_DESCRIPTIONS, descriptions.toString().trim());
        variable.addAttribute(INDEX_CODING_NAME, indexCoding.getName());
    }

    private static IndexCoding readIndexCoding(Variable variable, String indexCodingName) throws ProductIOException {
        final IndexCoding indexCoding = CfIndexCodingPart.readIndexCoding(variable, indexCodingName);

        if (indexCoding != null) {
            final Attribute descriptionsAtt = variable.findAttributeIgnoreCase(INDEX_DESCRIPTIONS);
            if (descriptionsAtt != null) {
                final String[] descriptions = descriptionsAtt.getStringValue().split(DESCRIPTION_SEPARATOR);
                if (indexCoding.getNumAttributes() == descriptions.length) {
                    for (int i = 0; i < descriptions.length; i++) {
                        indexCoding.getAttributeAt(i).setDescription(descriptions[i]);
                    }
                }
            }
            final Attribute nameAtt = variable.findAttributeIgnoreCase(INDEX_CODING_NAME);
            if (nameAtt != null) {
                indexCoding.setName(nameAtt.getStringValue());
            }
        }
        return indexCoding;
    }
}
