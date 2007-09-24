package org.esa.beam.util.jai;

import javax.media.jai.*;
import java.awt.image.*;
import java.awt.*;
import java.util.Vector;
import java.util.Arrays;

/**
 * Takes 3 or 4 single-banded source images
 * and creates a pixel interleaved 3- or 4-banded target image
 * using a component color model.
 */
public class InterleavedRgbOpImage extends PointOpImage {

    public InterleavedRgbOpImage(RenderedImage[] sources) {
        super(createVector(sources), createImageLayout(sources), null, true);
    }

    protected void computeRect(Raster[] sourceRasters,
                               WritableRaster targetRaster,
                               Rectangle targetRectangle) {
        Rectangle sourceRectangle = mapDestRect(targetRectangle, 0);
        RasterFormatTag[] formatTags = getFormatTags();

        RasterAccessor[] sourceAccessors = new RasterAccessor[sourceRasters.length];
        for (int i = 0; i < sourceRasters.length; i++) {
            RasterAccessor sourceAccessor = new RasterAccessor(sourceRasters[i],
                                                               sourceRectangle,
                                                               formatTags[i],
                                                               getSourceImage(i).getColorModel());
            if (sourceAccessor.getDataType() != DataBuffer.TYPE_BYTE) {
                throw new RuntimeException(this.getClass().getName() +
                        " does not implement computeRect()" +
                        " for short/int/float/double type sources");
            }
            sourceAccessors[i] = sourceAccessor;
        }
        RasterAccessor targetAccessor = new RasterAccessor(targetRaster,
                                                           targetRectangle,
                                                           formatTags[sourceRasters.length],
                                                           getColorModel());
        if (targetAccessor.getDataType() != DataBuffer.TYPE_BYTE) {
            throw new RuntimeException(this.getClass().getName() +
                    " does not implement computeRect()" +
                    " for short/int/float/double type targets");
        }
        byteLoop(sourceAccessors, targetAccessor);
        if (targetAccessor.isDataCopy()) {
            targetAccessor.clampDataArrays();
            targetAccessor.copyDataToRaster();
        }
    }

    private void byteLoop(RasterAccessor[] sources, RasterAccessor target) {
        final int targetWidth = target.getWidth();
        final int targetHeight = target.getHeight();
        final int numTargetBands = target.getNumBands();

        final byte dstDataArrays[][] = target.getByteDataArrays();
        final int dstBandOffsets[] = target.getBandOffsets();
        final int dstPixelStride = target.getPixelStride();
        final int dstScanlineStride = target.getScanlineStride();

        for (int k = 0; k < numTargetBands; k++) {
            final byte srcDataArrays[][] = sources[k].getByteDataArrays();
            final int srcBandOffsets[] = sources[k].getBandOffsets();
            final int srcPixelStride = sources[k].getPixelStride();
            final int srcScanlineStride = sources[k].getScanlineStride();

            final byte srcData[] = srcDataArrays[0]; // single band expected
            final byte dstData[] = dstDataArrays[k];
            int srcScanlineOffset = srcBandOffsets[0]; // single band expected
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

    private static Vector createVector(RenderedImage[] sources) {
        return new Vector<RenderedImage>(Arrays.asList(sources));
    }

    private static ImageLayout createImageLayout(RenderedImage[] sources) {
        RenderedImage source0 = sources[0];
        int numBands = sources.length;
        SampleModel sampleModel = RasterFactory.createPixelInterleavedSampleModel(DataBuffer.TYPE_BYTE,
                                                                                  source0.getWidth(),
                                                                                  source0.getHeight(),
                                                                                  numBands,
                                                                                  numBands * source0.getWidth(),
                                                                                  new int[]{2, 1, 0});
        ColorModel colorModel = PlanarImage.createColorModel(sampleModel);
        return new ImageLayout(source0.getMinX(),
                               source0.getMinY(),
                               source0.getWidth(),
                               source0.getHeight(),
                               source0.getTileGridXOffset(),
                               source0.getTileGridYOffset(),
                               source0.getTileWidth(),
                               source0.getTileHeight(),
                               sampleModel,
                               colorModel);
    }

}