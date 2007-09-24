package org.esa.beam.util.jai;

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.datamodel.BitmaskDef;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.util.Debug;
import org.esa.beam.util.math.Histogram;

import javax.media.jai.*;
import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.renderable.ParameterBlock;
import java.io.IOException;

/*
 * New fully JAI-backed, tiled image creation (nf, 20.09.2007). IN PROGRESS!
 */

public class ImageFactory {

    public static PlanarImage createOverlayedRgbImage(final RasterDataNode[] rasterDataNodes, final String histogramMatching) throws IOException {
        PlanarImage image = createRgbImage(rasterDataNodes, histogramMatching);
        BitmaskDef[] bitmaskDefs = rasterDataNodes[0].getBitmaskDefs();
        if (bitmaskDefs.length > 0) {
            image = new BitmaskOverlayOpImage(image, rasterDataNodes[0]);
        }
        return image;
    }

    public static PlanarImage createRgbImage(final RasterDataNode[] rasterDataNodes, final String histogramMatching) {
        Assert.notNull(rasterDataNodes,
                       "rasterDataNodes");
        Assert.state(rasterDataNodes.length == 1 || rasterDataNodes.length == 3 || rasterDataNodes.length == 4,
                     "invalid number of bands");

        PlanarImage[] sourceImages = new PlanarImage[rasterDataNodes.length];
        for (int i = 0; i < rasterDataNodes.length; i++) {
            final RasterDataNode raster = rasterDataNodes[i];
            RenderedImage renderedImage = raster.getImage();
            PlanarImage planarImage;
            if (renderedImage != null) {
                planarImage = PlanarImage.wrapRenderedImage(renderedImage);
            } else {
                planarImage = new RasterDataNodeOpImage(raster);
                raster.setImage(planarImage);
            }
            final ImageInfo imageInfo;
            if (raster.getImageInfo() == null) {
                Debug.trace("planarImage = " + planarImage);
                Debug.trace("  Computing extrema for [" + planarImage + "]...");
                double[] extrema = JAIUtils.getExtrema(planarImage, null);
                Debug.trace("  Extrema computed.");
                Debug.trace("  Computing histogram...");
                RenderedOp histogramImage = JAIUtils.createHistogramImage(planarImage, 512, extrema[0], extrema[1]);
                javax.media.jai.Histogram jaiHistogram = JAIUtils.getHistogramOf(histogramImage);
                Histogram histogram = new Histogram(jaiHistogram.getBins(0),  // Note: this is a raw data histogram
                                                    jaiHistogram.getLowValue(0),
                                                    jaiHistogram.getHighValue(0));
                Debug.trace("  Histogram computed.");
                imageInfo = raster.createDefaultImageInfo(null, histogram, true); // Note: this method expects a raw data histogram!
                raster.setImageInfo(imageInfo);
            } else {
                imageInfo = raster.getImageInfo();
            }

            final double newMin = raster.scaleInverse(imageInfo.getMinDisplaySample());
            final double newMax = raster.scaleInverse(imageInfo.getMaxDisplaySample());
            final double gamma = 1.0; //imageInfo.getGamma();  // todo

            planarImage = JAIUtils.createRescaleOp(planarImage, 255.0 / (newMax - newMin), 255.0 * newMin / (newMin - newMax));
            planarImage = JAIUtils.createFormatOp(planarImage, DataBuffer.TYPE_BYTE);
            sourceImages[i] = planarImage;
        }

        PlanarImage image;
        if (rasterDataNodes.length == 1) {
            final RasterDataNode raster = rasterDataNodes[0];
            final Color[] palette = raster.getImageInfo().getColorPalette();
            Assert.state(palette.length == 256, "palette.length == 256");
            final byte[][] lutData = new byte[3][256];
            for (int i = 0; i < 256; i++) {
                lutData[0][i] = (byte) palette[i].getRed();
                lutData[1][i] = (byte) palette[i].getGreen();
                lutData[2][i] = (byte) palette[i].getBlue();
            }
            image = JAIUtils.createLookupOp(sourceImages[0], lutData);
        } else {
            image = new InterleavedRgbOpImage(sourceImages);
        }

        if (ImageInfo.HISTOGRAM_MATCHING_EQUALIZE.equalsIgnoreCase(histogramMatching)) {
            image = JAIUtils.createHistogramEqualizedImage(image);
        } else if (ImageInfo.HISTOGRAM_MATCHING_NORMALIZE.equalsIgnoreCase(histogramMatching)) {
            image = JAIUtils.createHistogramNormalizedImage(image);
        }

        return image;
    }

    public static ROI createROI(final RasterDataNode rasterDataNode) {
        if (!rasterDataNode.isROIUsable()) {
            return null;
        }
        return new ROI(new RoiMaskOpImage(rasterDataNode), 1);
    }

    /**
     * Creates a new ROI image for the current ROI definition.
     *
     * @return a new ROI instance or null if no ROI definition is available
     */
    public static PlanarImage createROIImage(final ROI roi, final Color color) {
        PlanarImage image = roi.getAsImage();
        image = convertBinaryToIndexColorModel(image, color);
        image = convertIndexToComponentColorModel(image);
        return image;
    }

    private static PlanarImage convertBinaryToIndexColorModel(PlanarImage image, Color color) {
        ParameterBlock parameterBlock = new ParameterBlock();
        parameterBlock.addSource(image);
        parameterBlock.add(DataBuffer.TYPE_BYTE);
        ImageLayout imageLayout = new ImageLayout();
        imageLayout.setColorModel(new IndexColorModel(8, 2,
                                                      new byte[]{0, (byte) color.getRed()},
                                                      new byte[]{0, (byte) color.getGreen()},
                                                      new byte[]{0, (byte) color.getBlue()},
                                                      new byte[]{0, (byte) color.getAlpha()}));
        image = JAI.create("format", parameterBlock, new RenderingHints(JAI.KEY_IMAGE_LAYOUT, imageLayout));
        return image;
    }

    private static PlanarImage convertIndexToComponentColorModel(PlanarImage image) {
        ParameterBlock parameterBlock = new ParameterBlock();
        parameterBlock.addSource(image);
        parameterBlock.add(DataBuffer.TYPE_BYTE);
        ImageLayout imageLayout = new ImageLayout();
        SampleModel sampleModel = RasterFactory.createPixelInterleavedSampleModel(DataBuffer.TYPE_BYTE,
                                                                                  image.getWidth(),
                                                                                  image.getHeight(),
                                                                                  4);
        imageLayout.setSampleModel(sampleModel);
        imageLayout.setColorModel(PlanarImage.createColorModel(sampleModel));
        image = JAI.create("format", parameterBlock, new RenderingHints(JAI.KEY_IMAGE_LAYOUT, imageLayout));
        return image;
    }
}
