package org.esa.beam.dataio.netcdf4.convention;

import org.esa.beam.dataio.netcdf4.Nc4ReaderParameters;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.Product;
import ucar.nc2.NetcdfFileWriteable;

import java.io.IOException;

public interface InitialisationPart {

    Product readProductBody(String productName, Nc4ReaderParameters rp) throws ProductIOException;

    void writeProductBody(NetcdfFileWriteable writeable, Product p) throws IOException;
}
