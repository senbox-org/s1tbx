package org.esa.beam.util.jai;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.jexp.ParseException;
import com.bc.jexp.Parser;
import com.bc.jexp.Term;
import org.esa.beam.framework.datamodel.BitmaskDef;
import org.esa.beam.framework.datamodel.BitmaskOverlayInfo;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.dataop.barithm.BandArithmetic;
import org.esa.beam.framework.dataop.barithm.RasterDataSymbol;
import org.esa.beam.util.Debug;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.ImageUtils;
import org.esa.beam.util.StopWatch;
import org.esa.beam.util.math.Histogram;

import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.io.IOException;

/*
 * New JAI-based tiled image creation (nf, 20.09.2007). IN PROGRESS! 
 */
public class ImageFactory {


    public static RenderedImage createOverlayedImageJAI(final RasterDataNode[] rasterDataNodes, final String histogramMatching) throws IOException {
        Assert.notNull(rasterDataNodes,
                       "rasterDataNodes");
        Assert.state(rasterDataNodes.length == 1 || rasterDataNodes.length == 3,
                     "rasterDataNodes.length == 1 || rasterDataNodes.length == 3");

        final RasterDataNode raster = rasterDataNodes[0];

        PlanarImage overlayBIm;
        BitmaskDef[] bitmaskDefs = raster.getBitmaskDefs();
        PlanarImage sourcePIm = createRgbImageJAI(rasterDataNodes);

        final boolean doEqualize = ImageInfo.HISTOGRAM_MATCHING_EQUALIZE.equalsIgnoreCase(histogramMatching);
        final boolean doNormalize = ImageInfo.HISTOGRAM_MATCHING_NORMALIZE.equalsIgnoreCase(histogramMatching);
        if (doEqualize || doNormalize) {
            sourcePIm = JAIUtils.createTileFormatOp(sourcePIm, 512, 512);
            if (doEqualize) {
                sourcePIm = JAIUtils.createHistogramEqualizedImage((PlanarImage) sourcePIm);
            } else {
                sourcePIm = JAIUtils.createHistogramNormalizedImage((PlanarImage) sourcePIm);
            }
        }

        overlayBIm = sourcePIm;

        if (bitmaskDefs.length == 0) {
            overlayBIm = overlayBitmasksJAI(raster, overlayBIm);
        }

        return overlayBIm;
    }

    public static PlanarImage createRgbImageJAI(final RasterDataNode[] rasterDataNodes) {
        Guardian.assertNotNull("rasterDataNodes", rasterDataNodes);
        if (rasterDataNodes.length != 1 && rasterDataNodes.length != 3) {
            throw new IllegalArgumentException("rasterDataNodes.length is not 1 and not 3");
        }

        final boolean singleBand = rasterDataNodes.length == 1;
        final StopWatch stopWatch = new StopWatch();
        final int width = rasterDataNodes[0].getSceneRasterWidth();
        final int height = rasterDataNodes[0].getSceneRasterHeight();

        final int numPixels = width * height;
        final byte[] rgbSamples = new byte[3 * numPixels];

        PlanarImage resultingImage;
        PlanarImage[] images = new PlanarImage[rasterDataNodes.length];
        for (int i = 0; i < rasterDataNodes.length; i++) {
            final RasterDataNode raster = rasterDataNodes[i];
            PlanarImage image = (PlanarImage) raster.getImage(); // todo - check cast!
            if (image == null) {
                image = RasterDataNodeOpImage.create(raster);
                raster.setImage(image);
            }
            final ImageInfo imageInfo;
            if (raster.getImageInfo() == null) {
                double[] extrema = JAIUtils.getExtrema(image, null);
                RenderedOp histogramImage = JAIUtils.createHistogramImage((PlanarImage) image, 512, extrema[0], extrema[1]);
                javax.media.jai.Histogram jaiHistogram = JAIUtils.getHistogramOf(histogramImage);
                final Histogram histogram = new Histogram(jaiHistogram.getBins(0), jaiHistogram.getLowValue(0), jaiHistogram.getHighValue(0));
                imageInfo = raster.createDefaultImageInfo(null, histogram, true);
                raster.setImageInfo(imageInfo);
            } else {
                imageInfo = raster.getImageInfo();
            }

            final double newMin = imageInfo.getMinDisplaySample();
            final double newMax = imageInfo.getMaxDisplaySample();
            final double gamma = imageInfo.getGamma();  // todo

            image = JAIUtils.createRescaleOp(image, 255.0 / (newMax - newMin), 255.0 * newMin / (newMin - newMax));

            image = JAIUtils.createFormatOp(image, DataBuffer.TYPE_BYTE);

            images[i] = image;
        }

        if (singleBand) {
// todo - replace by lookup op
//                final RasterDataNode raster = rasterDataNodes[0];
//                final Color[] palette = raster.getImageInfo().getColorPalette();
//                Guardian.assertEquals("palette.length must be 256", palette.length, 256);
//                final byte[] r = new byte[256];
//                final byte[] g = new byte[256];
//                final byte[] b = new byte[256];
//                for (int i = 0; i < 256; i++) {
//                    r[i] = (byte) palette[i].getRed();
//                    g[i] = (byte) palette[i].getGreen();
//                    b[i] = (byte) palette[i].getBlue();
//                }
//                int colorIndex;
//                for (int i = 0; i < rgbSamples.length; i += 3) {
//                    colorIndex = rgbSamples[i] & 0xff;
//                    // BufferedImage.TYPE_3BYTE_BGR order
//                    rgbSamples[i + 0] = b[colorIndex];
//                    rgbSamples[i + 1] = g[colorIndex];
//                    rgbSamples[i + 2] = r[colorIndex];
//                }
//                pm.worked(1);
            resultingImage = images[0];
        } else {
            // todo - use band combine op
            resultingImage = images[0];
        }

        // Create a BufferedImage of type TYPE_3BYTE_BGR (the fastest type)
        //
//            final ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
//            final ColorModel cm = new ComponentColorModel(cs,
//                                                          false, // hasAlpha,
//                                                          false, //isAlphaPremultiplied,
//                                                          Transparency.OPAQUE, //  transparency,
//                                                          DataBuffer.TYPE_BYTE); //transferType
//            final DataBuffer db = new DataBufferByte(rgbSamples, rgbSamples.length);
//            final WritableRaster wr = Raster.createInterleavedRaster(db, width, height, 3 * width, 3, RGB_BAND_OFFSETS,
//                                                                     null);
//            bufferedImage = new BufferedImage(cm, wr, false, null);

        stopWatch.stopAndTrace("ProductUtils.createRgbImage");
        return resultingImage;
    }

    public static PlanarImage overlayBitmasksJAI(RasterDataNode raster, PlanarImage overlayPIm) throws
            IOException {
        final StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        final BitmaskOverlayInfo bitmaskOverlayInfo = raster.getBitmaskOverlayInfo();
        if (bitmaskOverlayInfo == null) {
            return overlayPIm;
        }
        final BitmaskDef[] bitmaskDefs = bitmaskOverlayInfo.getBitmaskDefs();
        if (bitmaskDefs.length == 0) {
            return overlayPIm;
        }

        final Product product = raster.getProduct();
        if (product == null) {
            throw new IllegalArgumentException("raster data node has not been added to a product");
        }

        final int w = raster.getSceneRasterWidth();
        final int h = raster.getSceneRasterHeight();

        final Parser parser = raster.getProduct().createBandArithmeticParser();

        for (int i = bitmaskDefs.length - 1; i >= 0; i--) {
            BitmaskDef bitmaskDef = bitmaskDefs[i];

            final String expr = bitmaskDef.getExpr();

            final Term term;
            try {
                term = parser.parse(expr);
            } catch (ParseException e) {
                final IOException ioException = new IOException("Illegal bitmask expression '" + expr + "'");
                ioException.initCause(e);
                throw ioException;
            }
            final RasterDataSymbol[] rasterSymbols = BandArithmetic.getRefRasterDataSymbols(term);
            final RasterDataNode[] rasterNodes = BandArithmetic.getRefRasters(rasterSymbols);

            // Ensures that all the raster data which are needed to create overlays are loaded
            for (RasterDataNode rasterNode : rasterNodes) {
                if (!rasterNode.hasRasterData()) {
                    rasterNode.readRasterDataFully(ProgressMonitor.NULL);
                }
            }

            final byte[] alphaData = new byte[w * h];
            product.readBitmask(0, 0, w, h, term, alphaData, (byte) (255 * bitmaskDef.getAlpha()), (byte) 0,
                                SubProgressMonitor.NULL);

            Debug.trace("ProductSceneView: creating bitmask overlay '" + bitmaskDef.getName() + "'...");
            BufferedImage alphaBIm = ImageUtils.createGreyscaleColorModelImage(w, h, alphaData);
            PlanarImage alphaPIm = PlanarImage.wrapRenderedImage(alphaBIm);

            overlayPIm = JAIUtils.createAlphaOverlay(overlayPIm, alphaPIm, bitmaskDef.getColor());
            Debug.trace("ProductSceneView: bitmask overlay OK");
        }
        return overlayPIm;
    }
}
