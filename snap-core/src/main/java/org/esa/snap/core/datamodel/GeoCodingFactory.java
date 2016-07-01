/*
 * Copyright (C) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.core.datamodel;/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.dataop.barithm.BandArithmetic;
import org.esa.snap.core.jexp.ParseException;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.runtime.Config;

import javax.media.jai.Interpolation;
import javax.media.jai.operator.CropDescriptor;
import javax.media.jai.operator.ScaleDescriptor;
import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import java.io.IOException;

public class GeoCodingFactory {

    public static final String USE_ALTERNATE_PIXEL_GEO_CODING_PROPERTY = "snap.useAlternatePixelGeoCoding";

    public static BasicPixelGeoCoding createPixelGeoCoding(final Band latBand,
                                                           final Band lonBand,
                                                           final String validMask,
                                                           final int searchRadius) {
        if (useAlternatePixelGeoCoding()) {
            return new PixelGeoCoding(latBand, lonBand, validMask, searchRadius);
        }
        return new PixelGeoCoding2(latBand, lonBand, validMask, 2);
    }

    public static BasicPixelGeoCoding createPixelGeoCoding(final Band latBand,
                                                           final Band lonBand,
                                                           final String validMask,
                                                           final int searchRadius,
                                                           ProgressMonitor pm) throws IOException {
        if (useAlternatePixelGeoCoding()) {
            return new PixelGeoCoding(latBand, lonBand, validMask, searchRadius, pm); // this is a special constructor
        }
        return new PixelGeoCoding2(latBand, lonBand, validMask, 2);
    }

    private static boolean useAlternatePixelGeoCoding() {
        return Config.instance().preferences().getBoolean(USE_ALTERNATE_PIXEL_GEO_CODING_PROPERTY, false);
    }

    static void copyReferencedRasters(String validMaskExpression,
                                      Scene sourceScene,
                                      Scene targetScene,
                                      ProductSubsetDef subsetDef) throws ParseException {
        final Product targetProduct = targetScene.getProduct();
        final RasterDataNode[] nodes = BandArithmetic.getRefRasters(validMaskExpression,
                                                                    sourceScene.getProduct());
        for (RasterDataNode node : nodes) {
            if (!targetProduct.containsRasterDataNode(node.getName())) {
                if (node instanceof TiePointGrid) {
                    TiePointGrid tpg = TiePointGrid.createSubset((TiePointGrid) node, subsetDef);
                    targetProduct.addTiePointGrid(tpg);
                }
                if (node instanceof Band) {
                    final Band sourceBand = (Band) node;
                    final Band band = createSubset(sourceBand, targetScene, subsetDef);
                    targetProduct.addBand(band);
                    setFlagCoding(band, sourceBand.getFlagCoding());
                }
            }
        }
    }

    static Band createSubset(Band sourceBand, Scene targetScene, ProductSubsetDef subsetDef) {
        final Band targetBand = new Band(sourceBand.getName(),
                                         sourceBand.getDataType(),
                                         targetScene.getRasterWidth(),
                                         targetScene.getRasterHeight());
        ProductUtils.copyRasterDataNodeProperties(sourceBand, targetBand);
        targetBand.setSourceImage(getSourceImage(subsetDef, sourceBand));
        return targetBand;
    }

    // for copying mask images - don't remove
    static Mask createSubset(Mask sourceMask, Scene targetScene, ProductSubsetDef subsetDef) {
        final Mask targetMask = Mask.BandMathsType.create(sourceMask.getName(),
                                                          sourceMask.getDescription(),
                                                          targetScene.getRasterWidth(),
                                                          targetScene.getRasterHeight(),
                                                          Mask.BandMathsType.getExpression(sourceMask),
                                                          sourceMask.getImageColor(),
                                                          sourceMask.getImageTransparency());
        targetMask.setSourceImage(getSourceImage(subsetDef, sourceMask));
        return targetMask;
    }

    private static void setFlagCoding(Band band, FlagCoding flagCoding) {
        if (flagCoding != null) {
            final String flagCodingName = flagCoding.getName();
            final Product product = band.getProduct();
            if (!product.getFlagCodingGroup().contains(flagCodingName)) {
                addFlagCoding(product, flagCoding);
            }
            band.setSampleCoding(product.getFlagCodingGroup().get(flagCodingName));
        }
    }

    private static void addFlagCoding(Product product, FlagCoding flagCoding) {
        final FlagCoding targetFlagCoding = new FlagCoding(flagCoding.getName());

        targetFlagCoding.setDescription(flagCoding.getDescription());
        ProductUtils.copyMetadata(flagCoding, targetFlagCoding);
        product.getFlagCodingGroup().add(targetFlagCoding);
    }

    private static RenderedImage getSourceImage(ProductSubsetDef subsetDef, Band band) {
        RenderedImage sourceImage = band.getSourceImage();
        if (subsetDef != null) {
            final Rectangle region = subsetDef.getRegion();
            if (region != null) {
                float x = region.x;
                float y = region.y;
                float width = region.width;
                float height = region.height;
                sourceImage = CropDescriptor.create(sourceImage, x, y, width, height, null);
            }
            final int subSamplingX = subsetDef.getSubSamplingX();
            final int subSamplingY = subsetDef.getSubSamplingY();
            if (mustSubSample(subSamplingX, subSamplingY) || mustTranslate(region)) {
                float scaleX = 1.0f / subSamplingX;
                float scaleY = 1.0f / subSamplingY;
                float transX = region != null ? -region.x : 0;
                float transY = region != null ? -region.y : 0;
                Interpolation interpolation = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
                sourceImage = ScaleDescriptor.create(sourceImage, scaleX, scaleY, transX, transY, interpolation, null);
            }
        }
        return sourceImage;
    }

    private static boolean mustTranslate(Rectangle region) {
        return (region != null && (region.x != 0 || region.y != 0));
    }

    private static boolean mustSubSample(int subSamplingX, int subSamplingY) {
        return subSamplingX != 1 || subSamplingY != 1;
    }

    public static double interpolateLon(double wx, double wy, double d00, double d10, double d01, double d11) {
        double range = GeoCodingFactory.computeRange(d00, d01, d10, d11);
        if (range > 180) {
            return GeoCodingFactory.interpolateSperical(wx, wy, d00, d10, d01, d11);
        } else {
            return MathUtils.interpolate2D(wx, wy, d00, d10, d01, d11);
        }
    }


    private static double computeRange(double d00, double d01, double d10, double d11) {
        double min = Math.min(d00, Math.min(d01, Math.min(d10, d11)));
        double max = Math.max(d00, Math.max(d01, Math.max(d10, d11)));

        return max - min;
    }

    private static double interpolateSperical(double wx, double wy, double d00, double d10, double d01, double d11) {
        double r00 = Math.toRadians(d00);
        double s00 = Math.sin(r00);
        double c00 = Math.cos(r00);

        double r01 = Math.toRadians(d01);
        double s01 = Math.sin(r01);
        double c01 = Math.cos(r01);

        double r10 = Math.toRadians(d10);
        double s10 = Math.sin(r10);
        double c10 = Math.cos(r10);

        double r11 = Math.toRadians(d11);
        double s11 = Math.sin(r11);
        double c11 = Math.cos(r11);

        double sinAngle = MathUtils.interpolate2D(wx, wy, s00, s10, s01, s11);
        double cosAngle = MathUtils.interpolate2D(wx, wy, c00, c10, c01, c11);
        return MathUtils.RTOD * Math.atan2(sinAngle, cosAngle);
    }
}
