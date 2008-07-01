package org.esa.beam.util.jai;

import com.bc.jexp.ParseException;
import com.bc.jexp.Term;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;

import javax.media.jai.PlanarImage;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.io.IOException;

/**
 * Creates a valid-pixel mask image for a given {@link org.esa.beam.framework.datamodel.RasterDataNode}.
 * The resulting image will have a single-band, interleaved sample model
 * with sample values 1 (= valid pixel) or 0 (= invalid pixel).
 */
public class ValidMaskOpImage extends RasterDataNodeOpImage {
    private static final byte FALSE = (byte) 0;
    private static final byte TRUE = (byte) 1;
    private Term term;

    public ValidMaskOpImage(RasterDataNode rasterDataNode) {
        super(rasterDataNode,
              createSingleBandedImageLayout(rasterDataNode,
                                            DataBuffer.TYPE_BYTE));
        final Product product = rasterDataNode.getProduct();
        try {
            this.term = product.createTerm(rasterDataNode.getValidMaskExpression());
        } catch (ParseException e) {
            throw new IllegalArgumentException("rasterDataNode", e);
        }
    }

    @Override
    protected void computeRect(PlanarImage[] sourceImages, WritableRaster tile, Rectangle destRect) {
        RasterFormatTag[] formatTags = getFormatTags();
        RasterAccessor targetAccessor = new RasterAccessor(tile,
                                                           destRect,
                                                           formatTags[0],
                                                           getColorModel());
        if (targetAccessor.getDataType() != DataBuffer.TYPE_BYTE) {
            throw new IllegalStateException(this.getClass().getName() +
                    " does not implement computeRect()" +
                    " for short/int/float/double type targets");
        }
        try {
            setValidMask(targetAccessor);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (targetAccessor.isDataCopy()) {
            targetAccessor.copyDataToRaster();
        }
    }

    private void setValidMask(RasterAccessor rasterAccessor) throws IOException {
        final RasterDataNode rasterDataNode = getRasterDataNode();
        final Product product = rasterDataNode.getProduct();
        final int x0 = rasterAccessor.getX();
        final int y0 = rasterAccessor.getY();
        final int w = rasterAccessor.getWidth();
        final int h = rasterAccessor.getHeight();
        final int offset = rasterAccessor.getBandOffset(0);
        final int stride = rasterAccessor.getScanlineStride();
        final byte[] data = rasterAccessor.getByteDataArray(0);
        if (data.length == w * h) {
            product.readBitmask(x0, y0, w, h, term, data, TRUE, FALSE, getProgressMonitor());
        } else {
            final byte[] temp = new byte[w * h];
            product.readBitmask(x0, y0, w, h, term, temp, TRUE, FALSE, getProgressMonitor());
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

    @Override
    public synchronized void dispose() {
        term = null;
        super.dispose();
    }
}