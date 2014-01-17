/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.nest.clustering.fuzzykmeans;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.StringUtils;

import javax.media.jai.PlanarImage;
import javax.media.jai.operator.MinDescriptor;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class Roi {

    private final Set<RenderedImage> maskImageSet = new HashSet<>();
    private final RenderedImage combinedMaskImage;

    public Roi(Product sourceProduct, Band[] sourceBands, String roiMaskName) {

        handleRoiMask(sourceProduct, roiMaskName);
//        handleValidMasks(sourceBands);
        if (maskImageSet.size() > 0) {
            combinedMaskImage = createCombinedMaskImage();
        } else {
            combinedMaskImage = null;
        }
    }

    public boolean contains(final int x, final int y) {
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
//        if (StringUtils.isNotNullAndNotEmpty(roiMaskName)) {
//        final Mask mask = product.getMaskGroup().get(roiMaskName);

        int height = product.getBandAt(0).getSourceImage().getHeight();
        int width = product.getBandAt(0).getSourceImage().getWidth();

        Rectangle rect = new Rectangle(width / 100, height / 100);
        System.out.println(rect.getHeight());
        System.err.println(rect.getWidth());
        ColorModel colorModel = product.getBandAt(0).getSourceImage().getColorModel();
        final BufferedImage bImage = new BufferedImage(width, height, BufferedImage.TYPE_USHORT_GRAY);
        Graphics2D bImageGraphics = bImage.createGraphics();
        bImageGraphics.drawImage(product.getBandAt(0).getSourceImage().getAsBufferedImage(rect, colorModel), null, null);
        RenderedImage rImage = (RenderedImage) bImage;
        maskImageSet.add(rImage);
//        if (mask != null) {
//            maskImageSet.add(mask.getSourceImage());
//        }
//        }
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
