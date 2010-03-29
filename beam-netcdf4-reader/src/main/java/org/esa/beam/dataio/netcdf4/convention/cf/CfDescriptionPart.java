package org.esa.beam.dataio.netcdf4.convention.cf;

import org.esa.beam.dataio.netcdf4.Nc4AttributeMap;
import org.esa.beam.dataio.netcdf4.Nc4Constants;
import org.esa.beam.dataio.netcdf4.Nc4ReaderParameters;
import org.esa.beam.dataio.netcdf4.convention.HeaderDataWriter;
import org.esa.beam.dataio.netcdf4.convention.ModelPart;
import org.esa.beam.framework.datamodel.Product;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFileWriteable;

import java.io.IOException;

public class CfDescriptionPart implements ModelPart {

    public static final String DESCRIPTION = "description";
    public static final String TITLE = "title";
    public static final String COMMENT = "comment";

    @Override
    public void read(Product p, Nc4ReaderParameters rp) throws IOException {
        p.setDescription(getProductDescription(rp));

    }

    @Override
    public void write(Product p, NetcdfFileWriteable ncFile, HeaderDataWriter hdw) throws IOException {
        final String description = p.getDescription();
        if (description != null && description.trim().length() > 0) {
            ncFile.addAttribute(null, new Attribute(TITLE, description));
        }
    }

    public static String getProductDescription(final Nc4ReaderParameters rv) {
        final Nc4AttributeMap attributesMap = rv.getGlobalAttributes();
        final String[] attribNames = new String[]{DESCRIPTION, TITLE, COMMENT};
        for (String attribName : attribNames) {
            final String description = attributesMap.getStringValue(attribName);
            if (description != null) {
                return description;
            }
        }
        return Nc4Constants.FORMAT_DESCRIPTION;
    }
}
