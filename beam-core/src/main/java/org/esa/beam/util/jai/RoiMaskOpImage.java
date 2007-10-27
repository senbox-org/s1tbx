package org.esa.beam.util.jai;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.jexp.ParseException;
import com.bc.jexp.Term;
import org.esa.beam.framework.datamodel.Pin;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.ROIDefinition;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.draw.Figure;
import org.esa.beam.util.Debug;
import org.esa.beam.util.StringUtils;

import javax.media.jai.PlanarImage;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.IOException;

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
            setROIMask(getRasterDataNode(), targetAccessor);
        } catch (IOException e) {
            // todo - what do do here?
            throw new RuntimeException(e);
        }
        if (targetAccessor.isDataCopy()) {
            targetAccessor.copyDataToRaster();
        }
    }

    private static void setROIMask(RasterDataNode rasterDataNode, RasterAccessor rasterAccessor) throws IOException {
        final ROIDefinition roiDefinition = rasterDataNode.getROIDefinition();
        Debug.assertNotNull(roiDefinition);

        boolean dataValid = false;

        // Step 1:  insert ROI pixels determined by bitmask expression
        String bitmaskExpr = roiDefinition.getBitmaskExpr();
        if (!StringUtils.isNullOrEmpty(bitmaskExpr) && roiDefinition.isBitmaskEnabled()) {
            setROIMaskByBandArithmetic(rasterDataNode, rasterAccessor, bitmaskExpr);
            dataValid = true;
        }

        // Step 2:  insert ROI pixels within value range
        if (roiDefinition.isValueRangeEnabled()) {
            if (setROIMaskByValueRange(rasterDataNode, rasterAccessor, dataValid)) {
                dataValid = true;
            }
        }

        // Step 3:  insert ROI pixels for pins
        if (roiDefinition.isPinUseEnabled()) {
            if (setROIMaskByPins(rasterDataNode, rasterAccessor, dataValid)) {
                dataValid = true;
            }
        }

        // Step 4:  insert ROI pixels within shape
        Figure roiShapeFigure = roiDefinition.getShapeFigure();
        if (roiDefinition.isShapeEnabled() && roiShapeFigure != null) {
            if (setROIMaskByShape(rasterDataNode, rasterAccessor, dataValid)) {
                dataValid = true;
            }
        }

        // Step 5:  invert ROI pixels
        if (dataValid && roiDefinition.isInverted()) {
            setROIMaskInverted(rasterDataNode, rasterAccessor, dataValid);
        }
    }

    private static void setROIMaskByBandArithmetic(RasterDataNode rasterDataNode, RasterAccessor rasterAccessor, String bitmaskExpr) throws IOException {
        int x0 = rasterAccessor.getX();
        int y0 = rasterAccessor.getY();
        int w = rasterAccessor.getWidth();
        int h = rasterAccessor.getHeight();
        byte[] data = rasterAccessor.getByteDataArray(0);
        int offset = rasterAccessor.getBandOffset(0);
        int stride = rasterAccessor.getScanlineStride();
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
        // Since this is the 1st mask we create, we don't need to call setROIMaskResult() for each pixel
        if (data.length == w * h) {
            product.readBitmask(x0, y0, w, h, term, data, TRUE, FALSE, ProgressMonitor.NULL);
        } else {
            byte[] temp = new byte[w * h];
            product.readBitmask(x0, y0, w, h, term, temp, TRUE, FALSE, ProgressMonitor.NULL);
            int lineIndex = offset;
            for (int y = 0; y < h; y++) {
                int pixelIndex = lineIndex;
                for (int x = 0; x < w; x++) {
                    data[pixelIndex] = temp[y * w + x];
                    pixelIndex++;
                }
                lineIndex += stride;
            }
        }
    }

    private static boolean setROIMaskByShape(RasterDataNode rasterDataNode, RasterAccessor rasterAccessor, boolean dataValid) {
        int x0 = rasterAccessor.getX();
        int y0 = rasterAccessor.getY();
        int w = rasterAccessor.getWidth();
        int h = rasterAccessor.getHeight();
        byte[] data = rasterAccessor.getByteDataArray(0);
        int offset = rasterAccessor.getBandOffset(0);
        int stride = rasterAccessor.getScanlineStride();
        boolean orCombined = rasterDataNode.getROIDefinition().isOrCombined();
        Figure shapeFigure = rasterDataNode.getROIDefinition().getShapeFigure();
        if (orCombined && !shapeFigure.getShape().intersects(x0, y0, w, h)) {
            return false;
        }
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
        if (!dataValid && data.length == w * h) {
            System.arraycopy(data2, 0, data, 0, w * h);
        } else {
            int lineIndex = offset;
            for (int y = 0; y < h; y++) {
                int pixelIndex = lineIndex;
                for (int x = 0; x < w; x++) {
                    setROIMaskResult(data, dataValid, pixelIndex, data2[y * w + x] != 0, orCombined);
                    pixelIndex++;
                }
                lineIndex += stride;
            }
        }
        return true;
    }

    private static boolean setROIMaskByPins(RasterDataNode rasterDataNode, RasterAccessor rasterAccessor, boolean dataValid) {
        int x0 = rasterAccessor.getX();
        int y0 = rasterAccessor.getY();
        int w = rasterAccessor.getWidth();
        int h = rasterAccessor.getHeight();
        byte[] data = rasterAccessor.getByteDataArray(0);
        int offset = rasterAccessor.getBandOffset(0);
        int stride = rasterAccessor.getScanlineStride();
        boolean orCombined = rasterDataNode.getROIDefinition().isOrCombined();
        ProductNodeGroup<Pin> pinGroup = rasterDataNode.getProduct().getPinGroup();
        final Pin[] pins = pinGroup.toArray(new Pin[pinGroup.getNodeCount()]);
        if (!dataValid || orCombined) {
            boolean pinSet = false;
            for (Pin pin : pins) {
                final PixelPos pixelPos = pin.getPixelPos();
                int x = (int) Math.floor(pixelPos.getX());
                int y = (int) Math.floor(pixelPos.getY());
                if ((x >= x0 && x < x0 + w) && (y >= y0 && y < y0 + h)) {
                    int pixelIndex = offset + (y - y0) * stride + (x - x0);
                    data[pixelIndex] = TRUE;
                    pinSet = true;
                }
            }
            return pinSet;
        } else {
            boolean[] mask = new boolean[w * h];
            for (Pin pin : pins) {
                final PixelPos pixelPos = pin.getPixelPos();
                int x = (int) Math.floor(pixelPos.getX()) - x0;
                int y = (int) Math.floor(pixelPos.getY()) - y0;
                if ((x >= 0 && x < w) && (y >= 0 && y < h)) {
                    mask[y * w + x] = true;
                }
            }
            int lineIndex = offset;
            for (int y = 0; y < h; y++) {
                int pixelIndex = lineIndex;
                for (int x = 0; x < w; x++) {
                    setROIMaskResult(data, dataValid, pixelIndex, mask[y * w + x], orCombined);
                    pixelIndex++;
                }
                lineIndex += stride;
            }
            return true;
        }
    }

    private static boolean setROIMaskByValueRange(RasterDataNode rasterDataNode, RasterAccessor rasterAccessor, boolean dataValid) throws IOException {
        int x0 = rasterAccessor.getX();
        int y0 = rasterAccessor.getY();
        int w = rasterAccessor.getWidth();
        int h = rasterAccessor.getHeight();
        byte[] data = rasterAccessor.getByteDataArray(0);
        int offset = rasterAccessor.getBandOffset(0);
        int stride = rasterAccessor.getScanlineStride();
        boolean orCombined = rasterDataNode.getROIDefinition().isOrCombined();
        rasterDataNode.ensureValidMaskComputed(ProgressMonitor.NULL);
        ROIDefinition roiDefinition = rasterDataNode.getROIDefinition();
        final float min = roiDefinition.getValueRangeMin();
        final float max = roiDefinition.getValueRangeMax();
        float[] pixels = new float[w * h];
        rasterDataNode.readPixels(x0, y0, w, h, pixels, ProgressMonitor.NULL);
        float value;
        boolean result;
        int lineIndex = offset;
        for (int y = 0; y < h; y++) {
            int pixelIndex = lineIndex;
            for (int x = 0; x < w; x++) {
                if (rasterDataNode.isPixelValid(x0 + x, y0 + y)) {
                    value = pixels[y * w + x];
                    result = value >= min && value <= max;
                    setROIMaskResult(data,
                                     dataValid, pixelIndex,
                                     result,
                                     orCombined);
                }
                pixelIndex++;
            }
            lineIndex += stride;
        }
        return true;
    }


    private static void setROIMaskInverted(RasterDataNode rasterDataNode, RasterAccessor rasterAccessor, boolean dataValid) {
        int w = rasterAccessor.getWidth();
        int h = rasterAccessor.getHeight();
        byte[] data = rasterAccessor.getByteDataArray(0);
        int offset = rasterAccessor.getBandOffset(0);
        int stride = rasterAccessor.getScanlineStride();
        int lineIndex = offset;
        for (int y = 0; y < h; y++) {
            int pixelIndex = lineIndex;
            for (int x = 0; x < w; x++) {
                data[pixelIndex] = (data[pixelIndex] == FALSE) ? TRUE : FALSE;
                pixelIndex++;
            }
            lineIndex += stride;
        }
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
