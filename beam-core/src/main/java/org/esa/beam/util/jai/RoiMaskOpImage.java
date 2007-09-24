package org.esa.beam.util.jai;

import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.draw.Figure;
import org.esa.beam.util.Debug;
import org.esa.beam.util.StringUtils;

import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFormatTag;
import javax.media.jai.RasterAccessor;
import java.awt.image.*;
import java.awt.*;
import java.io.IOException;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.jexp.Term;
import com.bc.jexp.ParseException;

/**
 * Creates a ROI mask image for a given {@link RasterDataNode}.
 * The resulting image will have a single-band, interleaved sample model
 * with sample values 1 (= ROI pixel) or 0 (= not a ROI pixel). 
 */
public class RoiMaskOpImage extends RasterDataNodeOpImage {
    private static final byte FALSE = (byte) 0;
    private static final byte TRUE = (byte) 1;

    public RoiMaskOpImage(RasterDataNode rasterDataNode) {
        super(rasterDataNode,
              createSingleBandedImageLayout(rasterDataNode,
                                            DataBuffer.TYPE_BYTE));
    }

    @Override
    protected void computeRect(PlanarImage[] sourceImages, WritableRaster tile, Rectangle destRect) {
        RasterFormatTag[] formatTags = getFormatTags();
        RasterAccessor targetAccessor = new RasterAccessor(tile,
                                                           destRect,
                                                           formatTags[0],
                                                           getColorModel());
        if (targetAccessor.getDataType() != DataBuffer.TYPE_BYTE) {
            throw new IllegalArgumentException(this.getClass().getName() +
                    " does not implement computeRect()" +
                    " for short/int/float/double type targets");
        }
        try {
            setROIMask(targetAccessor.getByteDataArray(0),
                       getRasterDataNode(),
                       destRect.x,
                       destRect.y,
                       destRect.width,
                       destRect.height);
        } catch (IOException e) {
            // todo - what do do here?
            throw new RuntimeException(e);
        }
        if (targetAccessor.isDataCopy()) {
            targetAccessor.copyDataToRaster();
        }
    }

    private static void setROIMask(byte[] data, RasterDataNode rasterDataNode, int x0, int y0, int w, int h) throws IOException {
        final ROIDefinition roiDefinition = rasterDataNode.getROIDefinition();
        Debug.assertNotNull(roiDefinition);

        boolean dataValid = false;

        // Step 1:  insert ROI pixels determined by bitmask expression
        String bitmaskExpr = roiDefinition.getBitmaskExpr();
        if (!StringUtils.isNullOrEmpty(bitmaskExpr) && roiDefinition.isBitmaskEnabled()) {
            setROIMaskByBandArithmetic(rasterDataNode, bitmaskExpr, x0, y0, w, h, data);
            dataValid = true;
        }

        // Step 2:  insert ROI pixels within value range
        if (roiDefinition.isValueRangeEnabled()) {
            setROIMaskByValueRange(rasterDataNode, x0, y0, w, h, data, dataValid);
            dataValid = true;
        }

        // Step 3:  insert ROI pixels for pins
        if (roiDefinition.isPinUseEnabled()) {
            setROIMaskByPins(rasterDataNode, x0, y0, w, h, data, dataValid);
            dataValid = true;
        }

        // Step 4:  insert ROI pixels within shape
        Figure roiShapeFigure = roiDefinition.getShapeFigure();
        if (roiDefinition.isShapeEnabled() && roiShapeFigure != null) {
            setROIMaskByShape(rasterDataNode, x0, y0, w, h, data, dataValid);
            dataValid = true;
        }

        // Step 5:  invert ROI pixels
        if (dataValid && roiDefinition.isInverted()) {
            for (int i = 0; i < w * h; i++) {
                data[i] = (data[i] == FALSE) ? TRUE : FALSE;
            }
        }
    }

    private static boolean setROIMaskByShape(RasterDataNode rasterDataNode, int x0, int y0, int w, int h, byte[] data, boolean dataValid) {
        boolean orCombined = rasterDataNode.getROIDefinition().isOrCombined();
        Figure shapeFigure = rasterDataNode.getROIDefinition().getShapeFigure();
        final BufferedImage bi2 = new BufferedImage(w, h,
                                                    BufferedImage.TYPE_BYTE_INDEXED,
                                                    new IndexColorModel(8, 2,
                                                                        new byte[]{0x00, (byte) 0xff},
                                                                        new byte[]{0x00, (byte) 0xff},
                                                                        new byte[]{0x00, (byte) 0xff}));
        final Graphics2D graphics2D = bi2.createGraphics();
        graphics2D.translate(-x0, -y0);
        graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics2D.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
        graphics2D.setColor(Color.white);
        graphics2D.setStroke(new BasicStroke(1.0f));
        if (!shapeFigure.isOneDimensional()) {
            graphics2D.fill(shapeFigure.getShape());
        }
        graphics2D.draw(shapeFigure.getShape());
        graphics2D.dispose();
        final byte[] data2 = ((DataBufferByte) bi2.getRaster().getDataBuffer()).getData();
        if (!dataValid) {
            System.arraycopy(data2, 0, data, 0, w * h);
        } else {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int pixelIndex = y * w + x;
                    setROIMaskResult(data, dataValid, pixelIndex, data2[pixelIndex] != 0, orCombined);
                }
            }
        }
        return true;
    }

    private static void setROIMaskByPins(RasterDataNode rasterDataNode, int x0, int y0, int w, int h, byte[] data, boolean dataValid) {
        boolean orCombined = rasterDataNode.getROIDefinition().isOrCombined();
        ProductNodeGroup<Pin> pinGroup = rasterDataNode.getProduct().getPinGroup();
        final Pin[] pins = pinGroup.toArray(new Pin[pinGroup.getNodeCount()]);
        for (Pin pin : pins) {
            final PixelPos pixelPos = pin.getPixelPos();
            int x = (int) Math.floor(pixelPos.getX());
            int y = (int) Math.floor(pixelPos.getY());
            boolean result = (x >= x0 && x < x0 + w) && (y >= y0 && y < y0 + h);
            setROIMaskResult(data, dataValid, (y - y0) * w + (x - x0), result, orCombined);
        }
    }

    private static void setROIMaskByValueRange(RasterDataNode rasterDataNode, int x0, int y0, int w, int h, byte[] data, boolean dataValid) throws IOException {
        boolean orCombined = rasterDataNode.getROIDefinition().isOrCombined();
        rasterDataNode.ensureValidMaskComputed(ProgressMonitor.NULL);
        ROIDefinition roiDefinition = rasterDataNode.getROIDefinition();
        final float min = roiDefinition.getValueRangeMin();
        final float max = roiDefinition.getValueRangeMax();
        float[] pixels = new float[w * h];
        rasterDataNode.readPixels(x0, y0, w, h, pixels, ProgressMonitor.NULL);
        int pixelIndex;
        float value;
        boolean result;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (rasterDataNode.isPixelValid(x0 + x, y0 + y)) {
                    pixelIndex = y * w + x;
                    value = pixels[pixelIndex];
                    result = value >= min && value <= max;
                    setROIMaskResult(data,
                                     dataValid, pixelIndex,
                                     result,
                                     orCombined);
                }
            }
        }
    }

    private static void setROIMaskByBandArithmetic(RasterDataNode rasterDataNode, String bitmaskExpr, int x0, int y0, int w, int h, byte[] data) throws IOException {
        final Product product = rasterDataNode.getProduct();
        final Term term;
        try {
            term = product.createTerm(bitmaskExpr);
        } catch (ParseException e) {
            final IOException ioException = new IOException(
                    "Could not create the ROI image because the bitmask expression\n" +
                            "'" + bitmaskExpr + "'\n" +
                            "is not a valid expression.");
            ioException.initCause(e);
            throw ioException;
        }
        product.readBitmask(x0, y0, w, h, term, data, TRUE, FALSE, ProgressMonitor.NULL);
    }

    private static void setROIMaskResult(byte[] data, boolean dataValid, int pixelIndex, boolean result, boolean orCombined) {
        byte bNew = result ? TRUE : FALSE;
        if (dataValid) {
            byte bOld = data[pixelIndex];
            if (orCombined) {
                data[pixelIndex] = (bOld == TRUE || bNew == TRUE) ? TRUE : FALSE;
            } else {
                data[pixelIndex] = (bOld == TRUE && bNew == TRUE) ? TRUE : FALSE;
            }
        } else {
            data[pixelIndex] = bNew;
        }
    }
}
