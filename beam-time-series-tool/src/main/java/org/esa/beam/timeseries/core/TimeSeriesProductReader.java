/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.timeseries.core;

import org.esa.beam.dataio.dimap.DimapProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.timeseries.core.timeseries.datamodel.TimeSeriesFactory;

import java.io.IOException;

import static org.esa.beam.timeseries.core.timeseries.datamodel.AbstractTimeSeries.TIME_SERIES_PRODUCT_TYPE;

class TimeSeriesProductReader extends DimapProductReader {

    public TimeSeriesProductReader(ProductReaderPlugIn productReaderPlugIn) {
        super(productReaderPlugIn);
    }

    @Override
    protected Product processProduct(Product product) throws IOException {
        Product readProduct = super.processProduct(product);
        if (readProduct.getProductType().equals(TIME_SERIES_PRODUCT_TYPE)) {
            TimeSeriesFactory.create(readProduct);
        }
        return readProduct;
    }

    @Override
    public void close() throws IOException {
        super.close();
        Product product = getProduct();
        if (product.getProductType().equals(TIME_SERIES_PRODUCT_TYPE)) {
            TimeSeriesMapper.getInstance().remove(product);
        }
    }
}
