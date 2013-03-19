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

import org.esa.beam.dataio.dimap.DimapProductWriter;
import org.esa.beam.framework.dataio.ProductWriterPlugIn;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.timeseries.core.timeseries.datamodel.AbstractTimeSeries;

class TimeSeriesProductWriter extends DimapProductWriter {

    public TimeSeriesProductWriter(ProductWriterPlugIn productWriterPlugIn) {
        super(productWriterPlugIn);
    }

    @Override
    public boolean shouldWrite(ProductNode node) {
        boolean shouldWrite = super.shouldWrite(node);
        if (shouldWrite && node.getProduct().getProductType().equals(AbstractTimeSeries.TIME_SERIES_PRODUCT_TYPE)) {
            return !(node instanceof RasterDataNode);
        }
        return shouldWrite;
    }
}
