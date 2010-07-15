/*
 * $Id$
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
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
package org.esa.beam.dataio.netcdf.metadata.profiles.def;

import org.esa.beam.dataio.netcdf.metadata.ProfilePart;
import org.esa.beam.dataio.netcdf.metadata.ProfileReadContext;
import org.esa.beam.dataio.netcdf.metadata.ProfileWriteContext;
import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfIndexCodingPart;
import org.esa.beam.dataio.netcdf.util.Constants;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.IndexCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.Product;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.IOException;

public class DefaultIndexCodingPart extends ProfilePart {

    public static final String INDEX_DESCRIPTIONS = "index_descriptions";
    public static final String DESCRIPTION_SEPARATOR = "\t";
    public static final String INDEX_CODING_NAME = "index_coding_name";

    @Override
    public void read(ProfileReadContext ctx, Product p) throws IOException {
        final Band[] bands = p.getBands();
        for (Band band : bands) {
            final IndexCoding indexCoding = readIndexCoding(band, ctx);
            if (indexCoding != null) {
                p.getIndexCodingGroup().add(indexCoding);
                band.setSampleCoding(indexCoding);
            }
        }
    }

    @Override
    public void define(ProfileWriteContext ctx, Product p) throws IOException {
        final Band[] bands = p.getBands();
        for (Band band : bands) {
            writeIndexCoding(ctx.getNetcdfFileWriteable(), band);
        }
    }

    private void writeIndexCoding(NetcdfFileWriteable ncFile, Band band) {
        CfIndexCodingPart.writeIndexCoding(ncFile, band);

        final IndexCoding indexCoding = band.getIndexCoding();
        if (indexCoding != null) {
            final Variable variable = ncFile.findVariable(band.getName());

            final String[] indexNames = indexCoding.getIndexNames();
            final StringBuffer descriptions = new StringBuffer();
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
            variable.addAttribute(new Attribute(INDEX_DESCRIPTIONS, descriptions.toString().trim()));

            variable.addAttribute(new Attribute(INDEX_CODING_NAME, indexCoding.getName()));
        }
    }

    public static IndexCoding readIndexCoding(Band band, ProfileReadContext ctx) throws ProductIOException {
        final IndexCoding indexCoding = CfIndexCodingPart.readIndexCoding(band, ctx);

        if (indexCoding != null) {
            final Variable variable = ctx.getGlobalVariablesMap().get(band.getName());

            final Attribute descriptionsAtt = variable.findAttributeIgnoreCase(INDEX_DESCRIPTIONS);
            if (descriptionsAtt != null) {
                final String[] descriptions = descriptionsAtt.getStringValue().split(DESCRIPTION_SEPARATOR);
                if (indexCoding.getNumAttributes() != descriptions.length) {
                    throw new ProductIOException(Constants.EM_INVALID_INDEX_CODING);
                }
                for (int i = 0; i < descriptions.length; i++) {
                    indexCoding.getAttributeAt(i).setDescription(descriptions[i]);
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
