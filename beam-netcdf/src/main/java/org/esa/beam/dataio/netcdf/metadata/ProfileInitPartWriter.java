package org.esa.beam.dataio.netcdf.metadata;

import org.esa.beam.dataio.netcdf.ProfileWriteContext;
import org.esa.beam.framework.datamodel.Product;

import java.io.IOException;

public interface ProfileInitPartWriter {
    void writeProductBody(ProfileWriteContext ctx, Product product) throws IOException;

}
