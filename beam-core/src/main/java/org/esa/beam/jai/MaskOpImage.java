package org.esa.beam.jai;

import com.bc.ceres.core.ProgressMonitor;
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
 * Creates a mask image for a given {@link org.esa.beam.framework.datamodel.RasterDataNode}.
 * The resulting image will have a single-band, interleaved sample model
 * with sample values 255 or 0.
 * @deprecated Since BEAM 4.5. Use {@link org.esa.beam.jai.VirtualBandOpImage} instead.
 */
public class MaskOpImage extends SingleBandedOpImage {
    private static final byte FALSE = (byte) 0;
    private static final byte TRUE = (byte) 255;
    private final Product product;
    private final Term term;

    public static MaskOpImage create(RasterDataNode rasterDataNode, ResolutionLevel level) {
        return create(rasterDataNode.getProduct(), rasterDataNode.getValidMaskExpression(), level);
    }

    public static MaskOpImage create(Product product, String expression, ResolutionLevel level) {
        try {
            return new MaskOpImage(product, product.parseExpression(expression), level);
        } catch (ParseException e) {
            throw new IllegalArgumentException("expression", e);
        }
    }

    private MaskOpImage(Product product, Term term, ResolutionLevel level) {
        super(DataBuffer.TYPE_BYTE,
              product.getSceneRasterWidth(),
              product.getSceneRasterHeight(),
              product.getPreferredTileSize(),
              null,
              level);
        this.product = product;
        this.term = term;
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
            computeMask(targetAccessor);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (targetAccessor.isDataCopy()) {
            targetAccessor.copyDataToRaster();
        }
    }

    private void computeMask(RasterAccessor rasterAccessor) throws IOException {
        final int x0 = rasterAccessor.getX();
        final int y0 = rasterAccessor.getY();
        final int w = rasterAccessor.getWidth();
        final int h = rasterAccessor.getHeight();
        final int offset = rasterAccessor.getBandOffset(0);
        final int stride = rasterAccessor.getScanlineStride();
        final byte[] data = rasterAccessor.getByteDataArray(0);

        if (getLevel() == 0 && data.length == w * h) {
            product.readBitmask(x0, y0,
                                w, h,
                                term, data,
                                TRUE, FALSE,
                                ProgressMonitor.NULL);
        } else {
            final int sourceWidth = getSourceWidth(w);
            final byte[] scanLine = new byte[sourceWidth];
            int lineIndex = offset;
            for (int y = 0; y < h; y++) {
                product.readBitmask(getSourceX(x0), getSourceY(y0 + y),
                                    sourceWidth, 1,
                                    term, scanLine,
                                    TRUE, FALSE,
                                    ProgressMonitor.NULL);
                int pixelIndex = lineIndex;
                for (int x = 0; x < w; x++) {
                    int i = getSourceCoord(x, 0, sourceWidth - 1);
                    data[pixelIndex] = scanLine[i];
                    pixelIndex++;
                }
                lineIndex += stride;
            }
        }
    }
}
