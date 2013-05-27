/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.binning.operator;

import org.esa.beam.binning.DataPeriod;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductFilter;
import org.esa.beam.util.ProductUtils;

/**
 * Filters out all products that do not overlap with the given data day.
 *
 * @author Thomas Storm
 */
class SpatialDataDaySourceProductFilter implements ProductFilter {

    private final DataPeriod dataPeriod;

    public SpatialDataDaySourceProductFilter(DataPeriod dataPeriod) {
        this.dataPeriod = dataPeriod;
    }

    @Override
    public boolean accept(Product product) {
        float firstLon = product.getGeoCoding().getGeoPos(new PixelPos(0, 0), null).lon;
        float lastLon = product.getGeoCoding().getGeoPos(new PixelPos(product.getSceneRasterHeight() - 1, 0), null).lon;
        ProductData.UTC firstScanLineTime = ProductUtils.getScanLineTime(product, 0);
        ProductData.UTC lastScanLineTime = ProductUtils.getScanLineTime(product, product.getSceneRasterHeight() - 1);
        DataPeriod.Membership firstLineMembership = dataPeriod.getObservationMembership(firstLon, firstScanLineTime.getMJD());
        DataPeriod.Membership lastLineMembership = dataPeriod.getObservationMembership(lastLon, lastScanLineTime.getMJD());

        if (firstLineMembership == lastLineMembership && firstLineMembership != DataPeriod.Membership.CURRENT_PERIOD) {
            return false;
        }

        return true;
    }
}
