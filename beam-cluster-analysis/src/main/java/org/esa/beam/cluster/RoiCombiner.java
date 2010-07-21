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
package org.esa.beam.cluster;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.StringUtils;

import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.media.jai.ROI;
import javax.media.jai.operator.MinDescriptor;


class RoiCombiner {

    private final Set<RenderedImage> maskImages = new HashSet<RenderedImage>();
    private final ROI roi;

    RoiCombiner(Product sourceProduct, Band[] sourceBands, String roiMaskName) {
        handleRoiMask(sourceProduct, roiMaskName);
        handleValidMasks(sourceBands);
        if (maskImages.size() > 0) {
            RenderedImage combinedMaskImage = createCombinedMaskImage();
            if (combinedMaskImage != null) {
                roi = new ROI(combinedMaskImage, 1);
            } else {
                roi = null;
            }
        } else {
            roi = null;
        }
    }


    public ROI getROI() {
        return roi;
    }

    private void handleRoiMask(Product product, String roiMaskName) {
        Mask roiMask = null;
        if (StringUtils.isNotNullAndNotEmpty(roiMaskName)) {
            roiMask = product.getMaskGroup().get(roiMaskName);
        }
        if (roiMask != null) {
            maskImages.add(roiMask.getSourceImage());
        }
    }

    private void handleValidMasks(Band[] sourceBands) {
        for (Band band : sourceBands) {
            if (StringUtils.isNotNullAndNotEmpty(band.getValidMaskExpression())) {
                maskImages.add(band.getValidMaskImage());
            }
        }
    }

    private RenderedImage createCombinedMaskImage() {
        if (maskImages.size() <= 0) {
            return null;
        }
        List<RenderedImage> imageList = new ArrayList<RenderedImage>(maskImages);
        RenderedImage combinedImage = imageList.get(0);

        for (int i = 1; i < imageList.size(); i++) {
            RenderedImage roiImage2 = imageList.get(i);
            combinedImage = MinDescriptor.create(combinedImage, roiImage2, null);
        }
        return combinedImage;
    }
}
