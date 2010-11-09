package org.esa.beam.dataio.netcdf;

import org.esa.beam.dataio.netcdf.metadata.ProfilePartWriter;
import org.esa.beam.dataio.netcdf.metadata.ProfileWriteContext;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.Product;

class NullProfilePartWriter implements ProfilePartWriter {
    @Override
    public void preEncode(ProfileWriteContext ctx, Product p) throws ProductIOException {
    }

    @Override
    public void encode(ProfileWriteContext ctx, Product p) throws ProductIOException {
    }

    @Override
    public void postEncode(ProfileWriteContext ctx, Product p) throws ProductIOException {
    }
}
