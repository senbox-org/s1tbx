package org.esa.beam.util.jai;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
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

    public static PlanarImage createOverlayedRgbImage(final RasterDataNode[] rasterDataNodes,
                                                      final String histogramMatching,
                                                      final ProgressMonitor pm) throws IOException {
        PlanarImage image = createRgbImage(rasterDataNodes, histogramMatching, pm);
        BitmaskDef[] bitmaskDefs = rasterDataNodes[0].getBitmaskDefs();
        if (bitmaskDefs.length > 0) {
            image = new BitmaskOverlayOpImage(image, rasterDataNodes[0]);
        }
        return image;
    }

    public static PlanarImage createRgbImage(final RasterDataNode[] rasterDataNodes,
                                             final String histogramMatching,
                                             final ProgressMonitor pm) {
        Assert.notNull(rasterDataNodes,
                       "rasterDataNodes");
        Assert.state(rasterDataNodes.length == 1 || rasterDataNodes.length == 3 || rasterDataNodes.length == 4,
                     "invalid number of bands");

        pm.beginTask("Creating image...", rasterDataNodes.length * 300 + 1);

        PlanarImage image;
        try {
            PlanarImage[] sourceImages = createSourceImages(rasterDataNodes, pm);
            image = combineBands(rasterDataNodes, sourceImages);
            image = performHistogramMatching(image, histogramMatching);
        } finally {
            pm.done();
        }

        return image;
    }

    private static PlanarImage[] createSourceImages(RasterDataNode[] rasterDataNodes, ProgressMonitor pm) {
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
            RasterDataNodeOpImage.setProgressMonitor(planarImage, SubProgressMonitor.create(pm, 1));
            final ImageInfo imageInfo;
            if (raster.getImageInfo() == null) {
                Debug.trace("planarImage = " + planarImage);
                Debug.trace("  Computing extrema for [" + planarImage + "]...");
                double[] extrema;
                if (planarImage.getSampleModel().getDataType() == DataBuffer.TYPE_BYTE) {
                    extrema = new double[]{0.0, 255.0};
                } else if (planarImage.getSampleModel().getDataType() == DataBuffer.TYPE_SHORT) {
                    extrema = new double[]{-32768.0, 32767.0};
                } else if (planarImage.getSampleModel().getDataType() == DataBuffer.TYPE_USHORT) {
                    extrema = new double[]{0.0, 65535.0};
                } else {
                    extrema = JAIUtils.getExtrema(planarImage, null);
                }
                checkCanceled(pm);
                pm.worked(100);
                Debug.trace("  Extrema computed.");
                Debug.trace("  Computing histogram...");
                // todo - construct ROI from no-data image here
                // todo - make no-data image property of RasterDataNode
                RenderedOp histogramImage = JAIUtils.createHistogramImage(planarImage, 512, extrema[0], extrema[1]);
                Histogram histogram = u(histogramImage);
                Debug.trace("  Histogram computed.");
                imageInfo = raster.createDefaultImageInfo(null, histogram, true); // Note: this method expects a raw data histogram!
                raster.setImageInfo(imageInfo);
                checkCanceled(pm);
                pm.worked(100);
            } else {
                imageInfo = raster.getImageInfo();
                pm.worked(200);
            }

            final double newMin = raster.scaleInverse(imageInfo.getMinDisplaySample());
            final double newMax = raster.scaleInverse(imageInfo.getMaxDisplaySample());
            final double gamma = 1.0; //imageInfo.getGamma();  // todo

            planarImage = JAIUtils.createRescaleOp(planarImage, 255.0 / (newMax - newMin), 255.0 * newMin / (newMin - newMax));
            planarImage = JAIUtils.createFormatOp(planarImage, DataBuffer.TYPE_BYTE);
            sourceImages[i] = planarImage;
            checkCanceled(pm);
            pm.worked(100);
        }
        return sourceImages;
    }

    private static PlanarImage performHistogramMatching(PlanarImage image, String histogramMatching) {
        if (ImageInfo.HISTOGRAM_MATCHING_EQUALIZE.equalsIgnoreCase(histogramMatching)) {
            image = JAIUtils.createHistogramEqualizedImage(image);
        } else if (ImageInfo.HISTOGRAM_MATCHING_NORMALIZE.equalsIgnoreCase(histogramMatching)) {
            image = JAIUtils.createHistogramNormalizedImage(image);
        }
        return image;
    }

    private static PlanarImage combineBands(RasterDataNode[] rasterDataNodes, PlanarImage[] sourceImages) {
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
        return image;
    }

    private static Histogram u(RenderedOp histogramImage) {
        javax.media.jai.Histogram jaiHistogram = JAIUtils.getHistogramOf(histogramImage);

        int[] bins = jaiHistogram.getBins(0);
        int minIndex = 0;
        int maxIndex = bins.length - 1;
        for (int i = 0; i < bins.length; i++) {
            if (bins[i] > 0) {
                minIndex = i;
                break;
            }
        }
        for (int i = bins.length - 1; i >= 0; i--) {
            if (bins[i] > 0) {
                maxIndex = i;
                break;
            }
        }
        double lowValue = jaiHistogram.getLowValue(0);
        double highValue = jaiHistogram.getHighValue(0);
        int numBins = jaiHistogram.getNumBins(0);
        double binWidth = (highValue - lowValue) / numBins;
        int[] croppedBins = new int[maxIndex - minIndex + 1];
        System.arraycopy(bins, minIndex, croppedBins, 0, croppedBins.length);
        Histogram histogram = new Histogram(croppedBins,
                                            lowValue + minIndex * binWidth,
                                            lowValue + (maxIndex + 1.0) * binWidth);
        return histogram;
    }

    public static ROI createROI(final RasterDataNode rasterDataNode) {
        if (!rasterDataNode.isROIUsable()) {
            return null;
        }
        return new ROI(new RoiMaskOpImage(rasterDataNode), 1);
    }

    public static PlanarImage createROIImage(final ROI roi, final Color color) {
        return convertBinaryToComponentColorModel(roi.getAsImage(), color);
    }

    private static PlanarImage convertBinaryToComponentColorModel(PlanarImage image, Color color) {
        image = convertBinaryToIndexColorModel(image, color);
        image = convertIndexToComponentColorModel(image);
        return image;
    }

    public static PlanarImage createNoDataImage(final RasterDataNode rasterDataNode, final Color color) {
        return convertBinaryToComponentColorModel(new ValidMaskOpImage(rasterDataNode), color);
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

    private static void checkCanceled(ProgressMonitor pm) {
        if (pm.isCanceled()) {
            throw new RuntimeException("Process canceled by user.");
        }
    }
}
