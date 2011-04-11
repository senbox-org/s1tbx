package org.esa.beam.pixex.output;

import org.esa.beam.framework.datamodel.Product;

import java.awt.image.Raster;

public interface RasterNamesFactory {

String[] getRasterNames(final Product product);
}
