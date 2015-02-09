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
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.logging.BeamLogManager;

/**
 * Filters out all products that do not overlap with the given data day.
 *
 * @author Thomas Storm
 */
class SpatialDataDaySourceProductFilter extends BinningProductFilter {

    private final DataPeriod dataPeriod;

    public SpatialDataDaySourceProductFilter(BinningProductFilter parent, DataPeriod dataPeriod) {
        setParent(parent);
        this.dataPeriod = dataPeriod;
    }

    @Override
    protected boolean acceptForBinning(Product product) {
        double firstLon = product.getGeoCoding().getGeoPos(new PixelPos(0, 0), null).lon;
        double lastLon = product.getGeoCoding().getGeoPos(new PixelPos(0, product.getSceneRasterHeight() - 1), null).lon;
        ProductData.UTC firstScanLineTime = ProductUtils.getScanLineTime(product, 0);
        ProductData.UTC lastScanLineTime = ProductUtils.getScanLineTime(product, product.getSceneRasterHeight() - 1);
        DataPeriod.Membership firstLineMembership = dataPeriod.getObservationMembership(firstLon, firstScanLineTime.getMJD());
        DataPeriod.Membership lastLineMembership = dataPeriod.getObservationMembership(lastLon, lastScanLineTime.getMJD());

        String message = String.format("accepting product '%s': firstLineMembership=%s, " +
                                       "lastLineMembership=%s, startTime=%s, endTime=%s",
                                       product.getName(),
                                       firstLineMembership,
                                       lastLineMembership,
                                       product.getStartTime(),
                                       product.getEndTime());
        if (firstLineMembership == lastLineMembership && firstLineMembership != DataPeriod.Membership.CURRENT_PERIOD) {
            final String msg = "not " + message;
            BeamLogManager.getSystemLogger().finer(msg);
            setReason(msg);
            return false;
        }
        BeamLogManager.getSystemLogger().finer(message);

        return true;
    }
}
