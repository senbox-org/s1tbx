/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.analysis.rcp.toolviews.timeseries;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.gpf.StackUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**

 */
public class TimeSeriesTimes {

    private final ProductData.UTC[] utcTimes;

    public TimeSeriesTimes(final ProductData.UTC[] times) {
        this.utcTimes = sortTimes(times);
    }

    public int length() {
        return utcTimes.length;
    }

    public ProductData.UTC getTimeAt(final int index) {
        return utcTimes[index];
    }

    public int getIndex(final Band band) {
        final Product product = band.getProduct();
        if (StackUtils.isCoregisteredStack(product)) {
            ProductData.UTC secondaryTime = getSecondaryTime(product, band);
            if(secondaryTime != null) {
                return getIndex(secondaryTime);
            }
            if (foundInReference(product, band)) {
                return 0;
            }
        }
        return getIndex(product.getStartTime());
    }

    private static boolean foundInReference(final Product product, final Band band) {
        final String bandName = band.getName();
        final boolean isIntensity = bandName.startsWith("Intensity");
        final String[] refBandNames = StackUtils.getMasterBandNames(product);
        for (String refBand : refBandNames) {
            if (bandName.startsWith(refBand))
                return true;
            if (isIntensity && refBand.startsWith("i_")) {
                refBand = refBand.replace("i_", "Intensity_");
                if (refBand.equals(bandName))
                    return true;
            }
        }
        return false;
    }

    public static ProductData.UTC getSecondaryTime(final Product sourceProduct, final Band secBand) {
        final MetadataElement secondaryMetadataRoot = sourceProduct.getMetadataRoot().getElement(
                AbstractMetadata.SLAVE_METADATA_ROOT);
        if (secondaryMetadataRoot != null) {
            final String secBandName = secBand.getName();
            final boolean isIntensity = secBandName.startsWith("Intensity");
            for (MetadataElement elem : secondaryMetadataRoot.getElements()) {
                final String secBandNames = elem.getAttributeString(AbstractMetadata.SLAVE_BANDS, "");
                if (secBandNames.contains(secBandName))
                    return elem.getAttributeUTC(AbstractMetadata.first_line_time);
                if (isIntensity) {
                    final String iName = secBandName.replace("Intensity_", "i_");
                    if (secBandNames.contains(iName))
                        return elem.getAttributeUTC(AbstractMetadata.first_line_time);
                }
            }
        }
        return null;
    }

    public int getIndex(final ProductData.UTC time) {
        int i = 0;
        for (ProductData.UTC utc : utcTimes) {
            if (utc.getMJD() == time.getMJD())
                return i;
            ++i;
        }
        return -1;
    }

    public ProductData.UTC[] getUTCTimes() {
        return utcTimes;
    }

    private static ProductData.UTC[] sortTimes(final ProductData.UTC[] times) {
        final List<ProductData.UTC> timesList = new ArrayList<>(100);
        Collections.addAll(timesList, times);

        timesList.sort((t1, t2) -> {
            if (t1 == null || t2 == null)
                return 0;
            return t1.getAsDate().compareTo(t2.getAsDate());
        });

        return timesList.toArray(new ProductData.UTC[0]);
    }
}
