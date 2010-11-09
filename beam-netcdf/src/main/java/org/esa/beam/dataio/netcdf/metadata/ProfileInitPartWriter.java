package org.esa.beam.dataio.netcdf.metadata;

import org.esa.beam.dataio.netcdf.ProfileWriteContext;
import org.esa.beam.framework.datamodel.Product;

import java.io.IOException;

/**
 * Implementations write the basic structure of a {@link Product Product} to a NetCDF-file. They are provided to the framework by implementations
 * of {@link org.esa.beam.dataio.netcdf.AbstractNetCdfWriterPlugIn AbstractNetCdfWriterPlugIn}.
 */
public interface ProfileInitPartWriter {
    /**
     * Writes the basic product body.
     *
     * @param ctx the context for writing the product body
     * @param product the product to write
     * @throws IOException if an IO-Error occurs
     */
    void writeProductBody(ProfileWriteContext ctx, Product product) throws IOException;

}
