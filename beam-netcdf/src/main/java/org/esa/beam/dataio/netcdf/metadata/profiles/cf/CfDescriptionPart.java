package org.esa.beam.dataio.netcdf.metadata.profiles.cf;

import org.esa.beam.dataio.netcdf.metadata.ProfilePart;
import org.esa.beam.dataio.netcdf.metadata.ProfileReadContext;
import org.esa.beam.dataio.netcdf.metadata.ProfileWriteContext;
import org.esa.beam.dataio.netcdf.util.Constants;
import org.esa.beam.framework.datamodel.Product;
import ucar.nc2.Attribute;

import java.io.IOException;

public class CfDescriptionPart extends ProfilePart {

    private static final String DESCRIPTION = "description";
    private static final String TITLE = "title";
    private static final String COMMENT = "comment";
    private static final String[] DESCRIPTION_ATTRIBUTE_NAMES = new String[]{DESCRIPTION, TITLE, COMMENT};

    @Override
    public void read(ProfileReadContext ctx, Product p) throws IOException {
        for (String attribName : DESCRIPTION_ATTRIBUTE_NAMES) {
            Attribute attribute = ctx.getNetcdfFile().findGlobalAttribute(attribName);
            if (attribute != null) {
                final String description = attribute.getStringValue();
                if (description != null) {
                    p.setDescription(description);
                    return;
                }
            }
        }
        p.setDescription(Constants.FORMAT_DESCRIPTION);
    }

    @Override
    public void define(ProfileWriteContext ctx, Product p) throws IOException {
        final String description = p.getDescription();
        if (description != null && description.trim().length() > 0) {
            ctx.getNetcdfFileWriteable().addAttribute(null, new Attribute(TITLE, description));
        }
    }
}
