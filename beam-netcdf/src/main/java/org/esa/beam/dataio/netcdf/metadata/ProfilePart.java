package org.esa.beam.dataio.netcdf.metadata;

import org.esa.beam.framework.datamodel.Product;
import ucar.nc2.NetcdfFileWriteable;

import java.io.IOException;

/**
 * An I/O part of a metadata profile.
 */
public abstract class ProfilePart {

    public abstract void read(Profile ctx, Product p) throws IOException;

    public abstract void define(Profile ctx, Product p, NetcdfFileWriteable ncFile) throws IOException;

    public void write(Profile ctx, Product p, NetcdfFileWriteable ncFile) throws IOException {        
    }
}
