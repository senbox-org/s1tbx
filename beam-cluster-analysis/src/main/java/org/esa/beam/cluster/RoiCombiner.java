/*
 * $Id: $
 *
 * Copyright (C) 2008 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.cluster;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.StringUtils;

import javax.media.jai.ROI;


class RoiCombiner {

    private ROI combinedRoi;

    RoiCombiner(Band[] sourceBands, Band roiBand) {
        createRoi(roiBand);
        createValidMaskRoi(sourceBands);
    }

    public ROI getCombinedRoi() {
        return combinedRoi;
    }

    private void createRoi(Band roiBand) {
        if (roiBand != null && roiBand.isROIUsable()) {
            combinedRoi = new ROI(ImageManager.getInstance().createRoiMaskImage(roiBand, 0), 1);
        }
    }

    private void createValidMaskRoi(Band[] sourceBands) {
        for (Band band : sourceBands) {
            if (StringUtils.isNotNullAndNotEmpty(band.getValidMaskExpression())) {
                ROI noDataRoi = new ROI(band.getValidMaskImage(), 1);
                if (combinedRoi == null) {
                    combinedRoi = noDataRoi;
                } else {
                    combinedRoi = combinedRoi.intersect(noDataRoi);
                }
            }
        }
    }

}
