package org.esa.beam.util.jai;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.util.Debug;
import org.esa.beam.util.IntMap;
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

    private static final ColorPaletteDef.Point OTHER_POINT = new ColorPaletteDef.Point(Double.NaN, Color.BLACK,
                                                                                       "Other");

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
            image = performIndexToRgbConversion(rasterDataNodes, sourceImages);
            image = performHistogramMatching(image, histogramMatching);
        } finally {
            pm.done();
        }

        return image;
    }

    private static PlanarImage[] createSourceImages(RasterDataNode[] rasterDataNodes, ProgressMonitor pm) {
        prepareRenderedImages(rasterDataNodes);
        prepareImageInfos(rasterDataNodes, SubProgressMonitor.create(pm, 1));

        PlanarImage[] sourceImages = new PlanarImage[rasterDataNodes.length];
        for (int i = 0; i < rasterDataNodes.length; i++) {
            final RasterDataNode raster = rasterDataNodes[i];
            PlanarImage planarImage = PlanarImage.wrapRenderedImage(raster.getImage());

            final ColorPaletteDef def = raster.getImageInfo().getColorPaletteDef();
            final double newMin = raster.scaleInverse(def.getMinDisplaySample());
            final double newMax = raster.scaleInverse(def.getMaxDisplaySample());
            // todo - honour log-scaled bands
            planarImage = JAIUtils.createRescaleOp(planarImage,
                                                   255.0 / (newMax - newMin),
                                                   255.0 * newMin / (newMin - newMax));
            planarImage = JAIUtils.createFormatOp(planarImage,
                                                  DataBuffer.TYPE_BYTE);
            sourceImages[i] = planarImage;
            checkCanceled(pm);
            pm.worked(100);
        }
        return sourceImages;
    }

    private static void prepareRenderedImages(RasterDataNode[] rasterDataNodes) {
        for (final RasterDataNode raster : rasterDataNodes) {
            RenderedImage renderedImage = raster.getImage();
            if (renderedImage == null) {
                raster.setImage(new RasterDataNodeOpImage(raster));
            }
        }
    }

    private static void prepareImageInfos(RasterDataNode[] rasterDataNodes, ProgressMonitor pm) {
        for (final RasterDataNode raster : rasterDataNodes) {
            final RenderedImage renderedImage = raster.getImage();
            Assert.notNull(renderedImage);
            final PlanarImage planarImage = PlanarImage.wrapRenderedImage(renderedImage);
            if (raster.getImageInfo() == null) {
                if (raster instanceof Band) {
                    final IndexCoding indexCoding = ((Band) raster).getIndexCoding();
                    if (indexCoding != null) {
                        raster.setImageInfo(createIndexedImageInfo(planarImage, raster, indexCoding));
                    }
                }
                if (raster.getImageInfo() == null) {
                    Debug.trace("Computing sample extrema for [" + raster.getName() + "]...");
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
                    Debug.trace("Sample extrema computed.");

                    Debug.trace("Computing sample frequencies for [" + raster.getName() + "]...");
                    // Continuous --> Histogram
                    // todo - construct ROI from no-data image here
                    // todo - make no-data image property of RasterDataNode
                    RenderedOp histogramImage = JAIUtils.createHistogramImage(planarImage, 512, extrema[0], extrema[1]);
                    Histogram histogram = u(histogramImage);
                    // Note: createDefaultImageInfo() expects a raw data histogram!
                    ImageInfo imageInfo = raster.createDefaultImageInfo(null, histogram);
                    raster.setImageInfo(imageInfo);
                    pm.worked(100);
                    Debug.trace("Sample frequencies computed.");
                }
                checkCanceled(pm);
            } else {
                pm.worked(200);
            }
        }
    }

    private static ImageInfo createIndexedImageInfo(PlanarImage planarImage, RasterDataNode raster, IndexCoding indexCoding) {
        final MetadataAttribute[] attributes = indexCoding.getAttributes();
        IntMap sampleToIndexMap = new IntMap();
        // Note: we need one colour more, the last element is the "Other" class
        final ColorPaletteDef.Point[] points = new ColorPaletteDef.Point[attributes.length + 1];
        for (int index = 0; index < attributes.length; index++) {
            MetadataAttribute attribute = attributes[index];
            final int sample = attribute.getData().getElemInt();
            sampleToIndexMap.putValue(sample, index);
            double t = (index + 1.0) / attributes.length;
            points[index] = new ColorPaletteDef.Point(sample,
                                                      new Color((float) (0.5 + 0.5 * Math.sin(
                                                              Math.PI / 3. + t * 4. * Math.PI)),
                                                                (float) (0.5 + 0.5 * Math.sin(
                                                                        Math.PI / 2. + t * 2. * Math.PI)),
                                                                (float) (0.5 + 0.5 * Math.sin(
                                                                        Math.PI / 4. + t * 3. * Math.PI))),
                                                      attribute.getName());
        }
        points[points.length - 1] = OTHER_POINT;
        return new ImageInfo(new ColorPaletteDef(points));
    }

    private static PlanarImage performHistogramMatching(PlanarImage image, String histogramMatching) {
        if (ImageInfo.HISTOGRAM_MATCHING_EQUALIZE.equalsIgnoreCase(histogramMatching)) {
            image = JAIUtils.createHistogramEqualizedImage(image);
        } else if (ImageInfo.HISTOGRAM_MATCHING_NORMALIZE.equalsIgnoreCase(histogramMatching)) {
            image = JAIUtils.createHistogramNormalizedImage(image);
        }
        return image;
    }

    private static PlanarImage performIndexToRgbConversion(RasterDataNode[] rasterDataNodes,
                                                           PlanarImage[] sourceImages) {
        PlanarImage image;
        if (rasterDataNodes.length == 1) {
            final RasterDataNode raster = rasterDataNodes[0];
            final Color[] palette = raster.getImageInfo().getColorPaletteDef().createColorPalette(raster);
            final byte[][] lutData = new byte[3][palette.length];
            for (int i = 0; i < palette.length; i++) {
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
