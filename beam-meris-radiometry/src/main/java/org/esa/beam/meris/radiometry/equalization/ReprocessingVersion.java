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

package org.esa.beam.meris.radiometry.equalization;

import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.StringUtils;

@SuppressWarnings({"UnusedDeclaration"})
public enum ReprocessingVersion {


    AUTO_DETECT(-1),
    REPROCESSING_2(2),
    REPROCESSING_3(3);

    private static final String ELEM_NAME_DSD = "DSD";
    private static final String ATTRIBUTE_FILE_NAME = "FILE_NAME";
    private static final String REDUCED_RESOLUTION_PREFIX = "MER_R";
    private static final int REPRO2_RR_START_DATE = 20050607;
    private static final int REPRO2_FR_START_DATE = 20050708;
    private static final int REPRO3_RR_START_DATE = 20091008;
    private static final int REPRO3_FR_START_DATE = 20091008;

    private int version;

    ReprocessingVersion(int version) {
        this.version = version;
    }

    public int getVersion() {
        return version;
    }

    public static ReprocessingVersion autoDetect(Product product) {
        final MetadataElement dsdElement = product.getMetadataRoot().getElement(ELEM_NAME_DSD);
        if (dsdElement != null) {
            MetadataElement[] dsdElements = dsdElement.getElements();
            for (MetadataElement element : dsdElements) {
                String datasetName = element.getAttributeString("DATASET_NAME", "").toLowerCase();
                if (datasetName.startsWith("radiometric") && datasetName.contains("calibration")) {
                    final String calibrationFileName = element.getAttributeString(ATTRIBUTE_FILE_NAME);
                    if (StringUtils.isNotNullAndNotEmpty(calibrationFileName)) {
                        final boolean reduced = product.getProductType().startsWith(REDUCED_RESOLUTION_PREFIX);
                        final ReprocessingVersion version = detectReprocessingVersion(calibrationFileName, reduced);
                        if (!ReprocessingVersion.AUTO_DETECT.equals(version)) {
                            return version;
                        }
                    }
                }
            }
        }
        return ReprocessingVersion.AUTO_DETECT;
    }

    static ReprocessingVersion detectReprocessingVersion(String calibrationFileName, boolean isReduced) {
        if (StringUtils.isNullOrEmpty(calibrationFileName)) {
            return ReprocessingVersion.AUTO_DETECT;
        }
        final String parsedDate = calibrationFileName.substring(14, 22);
        final int date = Integer.parseInt(parsedDate);
        if (isReduced) {
            return getReprocessingVersion(date, REPRO2_RR_START_DATE, REPRO3_RR_START_DATE);
        } else {
            return getReprocessingVersion(date, REPRO2_FR_START_DATE, REPRO3_FR_START_DATE);
        }
    }

    private static ReprocessingVersion getReprocessingVersion(int date, int repro2RrStartDate, int repro3FrStartDate) {
        if (date >= repro2RrStartDate && date < repro3FrStartDate) {
            return ReprocessingVersion.REPROCESSING_2;
        } else if (date >= repro3FrStartDate) {
            return ReprocessingVersion.REPROCESSING_3;
        } else {
            return ReprocessingVersion.AUTO_DETECT;
        }
    }

}
