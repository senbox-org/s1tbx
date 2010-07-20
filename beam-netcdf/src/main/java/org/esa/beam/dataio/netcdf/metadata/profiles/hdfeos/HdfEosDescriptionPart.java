package org.esa.beam.dataio.netcdf.metadata.profiles.hdfeos;

import org.esa.beam.dataio.netcdf.metadata.ProfilePart;
import org.esa.beam.dataio.netcdf.metadata.ProfileReadContext;
import org.esa.beam.dataio.netcdf.metadata.ProfileWriteContext;
import org.esa.beam.framework.datamodel.Product;
import org.jdom.Element;

import java.io.IOException;


public class HdfEosDescriptionPart extends ProfilePart {

    @Override
    public void read(ProfileReadContext ctx, Product p) throws IOException {
        Element element = (Element) ctx.getProperty(HdfEosUtils.ARCHIVE_METADATA);
        if (element != null) {
            p.setDescription(HdfEosUtils.getValue(element, "ARCHIVEDMETADATA", "MASTERGROUP", "LONGNAME", "VALUE"));
        }
    }

    @Override
    public void define(ProfileWriteContext ctx, Product p) throws IOException {
        throw new IllegalStateException();
    }
}
