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
package org.esa.snap.cluster;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.StringUtils;

import javax.media.jai.PlanarImage;
import javax.media.jai.operator.MinDescriptor;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @deprecated It is wrong to force users to specify a <i>mask name</i> to filter pixels in a ROI.
 * Instead they should provide any boolean expression
 * (which also may include mask names).
 */
@Deprecated
class Roi {

    private final Set<RenderedImage> maskImageSet = new HashSet<RenderedImage>();
    private final RenderedImage combinedMaskImage;

    Roi(Product sourceProduct, Band[] sourceBands, String roiMaskName) {
        handleRoiMask(sourceProduct, roiMaskName);
        handleValidMasks(sourceBands);
        if (maskImageSet.size() > 0) {
            combinedMaskImage = createCombinedMaskImage();
        } else {
            combinedMaskImage = null;
        }
    }

    boolean contains(final int x, final int y) {
        if (combinedMaskImage == null) {
            return true;
        }

        final int tileW = combinedMaskImage.getTileWidth();
        final int tileH = combinedMaskImage.getTileHeight();
        final int tileX = PlanarImage.XToTileX(x, combinedMaskImage.getTileGridXOffset(), tileW);
        final int tileY = PlanarImage.YToTileY(y, combinedMaskImage.getTileGridYOffset(), tileH);
        final Raster tile = combinedMaskImage.getTile(tileX, tileY);

        return tile.getSample(x, y, 0) != 0;
    }

    private void handleRoiMask(Product product, String roiMaskName) {
        if (StringUtils.isNotNullAndNotEmpty(roiMaskName)) {
            final Mask mask = product.getMaskGroup().get(roiMaskName);
            if (mask != null) {
                maskImageSet.add(mask.getSourceImage());
            }
        }
    }

    private void handleValidMasks(Band[] sourceBands) {
        for (Band band : sourceBands) {
            if (StringUtils.isNotNullAndNotEmpty(band.getValidMaskExpression())) {
                maskImageSet.add(band.getValidMaskImage());
            }
        }
    }

    private RenderedImage createCombinedMaskImage() {
        final List<RenderedImage> imageList = new ArrayList<>(maskImageSet);
        RenderedImage combinedImage = imageList.get(0);

        for (int i = 1; i < imageList.size(); i++) {
            combinedImage = MinDescriptor.create(combinedImage, imageList.get(i), null);
        }

        return combinedImage;
    }
}
