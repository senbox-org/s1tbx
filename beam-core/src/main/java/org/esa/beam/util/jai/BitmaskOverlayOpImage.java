package org.esa.beam.util.jai;

import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.BitmaskDef;

import javax.media.jai.*;
import java.awt.image.*;
import java.awt.*;
import java.io.IOException;

import com.bc.jexp.Parser;
import com.bc.jexp.Term;
import com.bc.jexp.ParseException;
import com.bc.ceres.core.SubProgressMonitor;

public class BitmaskOverlayOpImage extends PointOpImage {

    private RasterDataNode rasterDataNode;

    public BitmaskOverlayOpImage(RenderedImage source, RasterDataNode raster) {
        super(source, new ImageLayout(source), null, true);
        this.rasterDataNode = raster;
        // todo - use rendering hints
        setTileCache(JAI.getDefaultInstance().getTileCache());
    }

    protected void computeRect(Raster[] sourceRasters,
                               WritableRaster targetRaster,
                               Rectangle targetRectangle) {
        Rectangle sourceRectangle = mapDestRect(targetRectangle, 0);
        RasterFormatTag[] formatTags = getFormatTags();

        RasterAccessor sourceAccessor = new RasterAccessor(sourceRasters[0],
                                                           sourceRectangle,
                                                           formatTags[0],
                                                           getSourceImage(0).getColorModel());
        if (sourceAccessor.getDataType() != DataBuffer.TYPE_BYTE) {
            throw new IllegalArgumentException(this.getClass().getName() +
                    " does not implement computeRect()" +
                    " for short/int/float/double type sources");
        }
        RasterAccessor targetAccessor = new RasterAccessor(targetRaster,
                                                           targetRectangle,
                                                           formatTags[sourceRasters.length],
                                                           getColorModel());
        if (targetAccessor.getDataType() != DataBuffer.TYPE_BYTE) {
            throw new IllegalArgumentException(this.getClass().getName() +
                    " does not implement computeRect()" +
                    " for short/int/float/double type targets");
        }

        final Product product = rasterDataNode.getProduct();
        if (product == null) {
            throw new IllegalArgumentException("raster data node has not been added to a product");
        }

        final BitmaskDef[] bitmaskDefs = rasterDataNode.getBitmaskDefs();
        if (bitmaskDefs.length == 0) {
            return;
        }
        System.out.println("bitmaskDefs = " + bitmaskDefs);
        copyLoop(sourceAccessor, targetAccessor);

        final Parser parser = rasterDataNode.getProduct().createBandArithmeticParser();

        final byte[] mask = new byte[targetRectangle.width * targetRectangle.height];
        for (int i = bitmaskDefs.length - 1; i >= 0; i--) {
            final BitmaskDef bitmaskDef = bitmaskDefs[i];
            final String expr = bitmaskDef.getExpr();

            try {
                Term term = parser.parse(expr);
                product.readBitmask(targetRectangle.x,
                                    targetRectangle.y,
                                    targetRectangle.width,
                                    targetRectangle.height,
                                    term,
                                    mask,
                                    (byte) 1,
                                    (byte) 0,
                                    SubProgressMonitor.NULL);
                overlayLoop(targetAccessor, mask,
                            bitmaskDef.getColor(), bitmaskDef.getAlpha());
            } catch (ParseException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if (targetAccessor.isDataCopy()) {
            targetAccessor.copyDataToRaster();
        }
    }

    private void copyLoop(RasterAccessor source, RasterAccessor target) {
        final int targetWidth = target.getWidth();
        final int targetHeight = target.getHeight();
        final int numTargetBands = target.getNumBands();

        final byte srcDataArrays[][] = source.getByteDataArrays();
        final int srcBandOffsets[] = source.getBandOffsets();
        final int srcPixelStride = source.getPixelStride();
        final int srcScanlineStride = source.getScanlineStride();

        final byte dstDataArrays[][] = target.getByteDataArrays();
        final int dstBandOffsets[] = target.getBandOffsets();
        final int dstPixelStride = target.getPixelStride();
        final int dstScanlineStride = target.getScanlineStride();

        for (int k = 0; k < numTargetBands; k++) {
            final byte srcData[] = srcDataArrays[k];
            final byte dstData[] = dstDataArrays[k];
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
            for (int y = 0; y < targetHeight; y++) {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;
                for (int x = 0; x < targetWidth; x++) {
                    dstData[dstPixelOffset] = srcData[srcPixelOffset];
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }

    private void overlayLoop(RasterAccessor target, byte[] mask, Color color, float alpha) {
        final int targetWidth = target.getWidth();
        final int targetHeight = target.getHeight();
        final int numTargetBands = target.getNumBands();

        final byte dstDataArrays[][] = target.getByteDataArrays();
        final int dstBandOffsets[] = target.getBandOffsets();
        final int dstPixelStride = target.getPixelStride();
        final int dstScanlineStride = target.getScanlineStride();

        final int[] components = new int[]{
                color.getRed(),
                color.getGreen(),
                color.getBlue(),
                color.getAlpha()
        };

        final int a = (int) (256.0f * alpha);
        final int b = 256 - a;
        for (int k = 0; k < numTargetBands; k++) {
            final int colorEnergy = a * components[k];
            final byte dstData[] = dstDataArrays[k];
            int dstScanlineOffset = dstBandOffsets[k];
            for (int y = 0; y < targetHeight; y++) {
                int dstPixelOffset = dstScanlineOffset;
                for (int x = 0; x < targetWidth; x++) {
                    if (mask[y * targetWidth + x] != 0) {
                        int value = (colorEnergy + b * (dstData[dstPixelOffset] & 0xff)) >> 8;
                        if (value > 255) {
                            value = 255;
                        }
                        dstData[dstPixelOffset] = (byte) value;
                    }
                    dstPixelOffset += dstPixelStride;
                }
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }

    public synchronized void dispose() {
        rasterDataNode = null;
        super.dispose();
    }
}
