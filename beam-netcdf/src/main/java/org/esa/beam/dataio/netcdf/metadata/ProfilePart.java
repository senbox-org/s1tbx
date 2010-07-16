package org.esa.beam.dataio.netcdf.metadata;

import org.esa.beam.framework.datamodel.Product;

import java.io.IOException;

/**
 * An I/O part of a metadata profile.
 */
public abstract class ProfilePart {

    public abstract void read(ProfileReadContext ctx, Product p) throws IOException;

    public abstract void define(ProfileWriteContext ctx, Product p) throws IOException;

    public void write(ProfileWriteContext ctx, Product p) throws IOException {
    }
}
