/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.binning.operator;

import org.esa.snap.binning.DataPeriod;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.SystemUtils;

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
            GeoCoding geoCoding = product.getSceneGeoCoding();
            ProductData.UTC firstScanLineTime = ProductUtils.getScanLineTime(product, 0);
            ProductData.UTC lastScanLineTime = ProductUtils.getScanLineTime(product, product.getSceneRasterHeight() - 1);
            if (firstScanLineTime == null || lastScanLineTime == null) {
                String message = String.format("not accepting product '%s': missing time coding", product.getName());
                setReason(message);
                return false;
            }
            double firstLon = geoCoding.getGeoPos(new PixelPos(0, 0), null).lon;
            DataPeriod.Membership topLeft = dataPeriod.getObservationMembership(firstLon, firstScanLineTime.getMJD());
            double lastLon = geoCoding.getGeoPos(new PixelPos(product.getSceneRasterWidth() - 1, 0), null).lon;
            DataPeriod.Membership topRight = dataPeriod.getObservationMembership(lastLon, firstScanLineTime.getMJD());


            firstLon = geoCoding.getGeoPos(new PixelPos(0, product.getSceneRasterHeight() - 1), null).lon;
            DataPeriod.Membership bottomLeft = dataPeriod.getObservationMembership(firstLon, lastScanLineTime.getMJD());
            lastLon = geoCoding.getGeoPos(new PixelPos(product.getSceneRasterWidth() - 1, product.getSceneRasterHeight() - 1), null).lon;
            DataPeriod.Membership bottomRight = dataPeriod.getObservationMembership(lastLon, lastScanLineTime.getMJD());

            String message = String.format("accepting product '%s': " +
                                           "topLeftMembership=%s, topRightMembership=%s, " +
                                           "bottomLeftMembership=%s, bottomRightMembership=%s, " +
                                           "startTime=%s, endTime=%s",
                                           product.getName(),
                                           topLeft, topRight,
                                           bottomLeft, bottomRight,
                                           product.getStartTime(),
                                           product.getEndTime());

            if (topLeft == topRight &&
                topRight == bottomLeft &&
                bottomLeft == bottomRight &&
                topLeft != DataPeriod.Membership.CURRENT_PERIOD) {
                final String msg = "not " + message;
                SystemUtils.LOG.finer(msg);
                setReason(msg);
                return false;
            }
        SystemUtils.LOG.finer(message);

            return true;
        }
}
