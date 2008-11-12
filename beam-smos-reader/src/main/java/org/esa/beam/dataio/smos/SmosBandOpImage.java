package org.esa.beam.dataio.smos;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.jai.SingleBandedOpImage;

import javax.media.jai.PixelAccessor;
import javax.media.jai.PlanarImage;
import javax.media.jai.UnpackedImageData;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;

class SmosBandOpImage extends SingleBandedOpImage {

    private final SmosFile smosFile;
    private final Band smosBand;
    private final int fieldIndex;
    private final PlanarImage seqnumImage;
    private final Number noDataValue;

    public SmosBandOpImage(SmosFile smosFile,
                             Band smosBand,
                             int fieldIndex,
                             Number noDataValue,
                             RenderedImage seqnumImage,
                             ResolutionLevel level) {
        super(ImageManager.getDataBufferType(smosBand.getDataType()),
              smosBand.getSceneRasterWidth(),
              smosBand.getSceneRasterHeight(),
              smosBand.getProduct().getPreferredTileSize(),
              null,
              level);
        this.smosFile = smosFile;
        this.smosBand = smosBand;
        this.fieldIndex = fieldIndex;
        this.noDataValue = noDataValue;
        this.seqnumImage = PlanarImage.wrapRenderedImage(seqnumImage);
    }

    @Override
    protected void computeRect(PlanarImage sources[], WritableRaster dstRaster, Rectangle dstRect) {
        try {
            Raster srcRaster = seqnumImage.getData(dstRect);

            PixelAccessor srcAccessor = new PixelAccessor(srcRaster.getSampleModel(), seqnumImage.getColorModel());
            PixelAccessor dstAccessor = new PixelAccessor(dstRaster.getSampleModel(), null);

            UnpackedImageData srcData = srcAccessor.getPixels(
                    srcRaster, dstRect, srcRaster.getSampleModel().getTransferType(), false);

            UnpackedImageData dstData = dstAccessor.getPixels(
                    dstRaster, dstRect, dstRaster.getSampleModel().getTransferType(), true);

            if (dstData.type == DataBuffer.TYPE_USHORT || dstData.type == DataBuffer.TYPE_SHORT) {
                shortLoop(srcData, dstData, noDataValue.shortValue());
            } else if (dstData.type == DataBuffer.TYPE_INT) {
                intLoop(srcData, dstData, noDataValue.intValue());
            } else if (dstData.type == DataBuffer.TYPE_FLOAT) {
                floatLoop(srcData, dstData, noDataValue.floatValue());
            } else {
                // do nothing
            }
            dstAccessor.setPixels(dstData);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void shortLoop(UnpackedImageData srcData, UnpackedImageData dstData, short noDataValue) throws IOException {
        final int dstWidth = dstData.rect.width;
        final int dstHeight = dstData.rect.height;

        int srcBandOffset = srcData.getOffset(0);
        final int srcPixelStride = srcData.pixelStride;
        final int srcScanlineStride = srcData.lineStride;
        final int[] srcDataArray = srcData.getIntData(0);

        int dstBandOffset = dstData.getOffset(0);
        final int dstPixelStride = dstData.pixelStride;
        final int dstScanlineStride = dstData.lineStride;
        final short[] dstDataArray = dstData.getShortData(0);

        int[][] gridPointIndexCache = new int[2][dstWidth];
        short[][] valueCache = new short[2][dstWidth];
        for (int y = 0; y < dstHeight; y++) {
            int srcPixelOffset = srcBandOffset;
            int dstPixelOffset = dstBandOffset;
            for (int x = 0; x < dstWidth; x++) {
                final int seqnum = srcDataArray[srcPixelOffset];
                final int gridPointIndex = smosFile.getGridPointId(seqnum);
                short btValue = noDataValue;
                if (gridPointIndex != -1) {
                    if (x > 0 && gridPointIndexCache[1][x - 1] == gridPointIndex) {
                        // this line, pixel to the left
                        btValue = valueCache[1][x - 1];
                    } else if (y > 0 && gridPointIndexCache[0][x] == gridPointIndex) {
                        // last line, pixel above
                        btValue = valueCache[0][x];
                    } else if (x > 0 && y > 0 && gridPointIndexCache[0][x - 1] == gridPointIndex) {
                        // last line, pixel to the left
                        btValue = valueCache[0][x - 1];
                    } else if (x < dstWidth - 1 && y > 0 && gridPointIndexCache[0][x + 1] == gridPointIndex) {
                        // last line, pixel to the right
                        btValue = valueCache[0][x + 1];
                    } else {
                        btValue = smosFile.getL1CBrowseBtDataShort(gridPointIndex, fieldIndex);
                    }
                }
                valueCache[1][x] = btValue;
                gridPointIndexCache[1][x] = gridPointIndex;

                dstDataArray[dstPixelOffset] = btValue;
                srcPixelOffset += srcPixelStride;
                dstPixelOffset += dstPixelStride;
            }
            int[] t1 = gridPointIndexCache[0];
            gridPointIndexCache[0] = gridPointIndexCache[1];
            gridPointIndexCache[1] = t1;
            short[] t2 = valueCache[0];
            valueCache[0] = valueCache[1];
            valueCache[1] = t2;

            srcBandOffset += srcScanlineStride;
            dstBandOffset += dstScanlineStride;
        }
    }

    private void intLoop(UnpackedImageData srcData, UnpackedImageData dstData, int noDataValue) throws IOException {
        final int dstWidth = dstData.rect.width;
        final int dstHeight = dstData.rect.height;

        int srcBandOffset = srcData.getOffset(0);
        final int srcPixelStride = srcData.pixelStride;
        final int srcScanlineStride = srcData.lineStride;
        final int[] srcDataArray = srcData.getIntData(0);

        int dstBandOffset = dstData.getOffset(0);
        final int dstPixelStride = dstData.pixelStride;
        final int dstScanlineStride = dstData.lineStride;
        final int[] dstDataArray = dstData.getIntData(0);

        int[][] gridPointIndexCache = new int[2][dstWidth];
        int[][] valueCache = new int[2][dstWidth];
        for (int y = 0; y < dstHeight; y++) {
            int srcPixelOffset = srcBandOffset;
            int dstPixelOffset = dstBandOffset;
            for (int x = 0; x < dstWidth; x++) {
                final int seqnum = srcDataArray[srcPixelOffset];
                final int gridPointIndex = smosFile.getGridPointId(seqnum);
                int btValue = noDataValue;
                if (gridPointIndex != -1) {
                    if (x > 0 && gridPointIndexCache[1][x - 1] == gridPointIndex) {
                        // this line, pixel to the left
                        btValue = valueCache[1][x - 1];
                    } else if (y > 0 && gridPointIndexCache[0][x] == gridPointIndex) {
                        // last line, pixel above
                        btValue = valueCache[0][x];
                    } else if (x > 0 && y > 0 && gridPointIndexCache[0][x - 1] == gridPointIndex) {
                        // last line, pixel to the left
                        btValue = valueCache[0][x - 1];
                    } else if (x < dstWidth - 1 && y > 0 && gridPointIndexCache[0][x + 1] == gridPointIndex) {
                        // last line, pixel to the right
                        btValue = valueCache[0][x + 1];
                    } else {
                        btValue = smosFile.getL1CBrowseBtDataInt(gridPointIndex, fieldIndex);
                    }
                }
                valueCache[1][x] = btValue;
                gridPointIndexCache[1][x] = gridPointIndex;

                dstDataArray[dstPixelOffset] = btValue;
                srcPixelOffset += srcPixelStride;
                dstPixelOffset += dstPixelStride;
            }
            int[] t1 = gridPointIndexCache[0];
            gridPointIndexCache[0] = gridPointIndexCache[1];
            gridPointIndexCache[1] = t1;
            int[] t2 = valueCache[0];
            valueCache[0] = valueCache[1];
            valueCache[1] = t2;

            srcBandOffset += srcScanlineStride;
            dstBandOffset += dstScanlineStride;
        }
    }

    private void floatLoop(UnpackedImageData srcData, UnpackedImageData dstData, float noDataValue) throws IOException {
        final int dstWidth = dstData.rect.width;
        final int dstHeight = dstData.rect.height;

        int srcBandOffset = srcData.getOffset(0);
        final int srcPixelStride = srcData.pixelStride;
        final int srcScanlineStride = srcData.lineStride;
        final int[] srcDataArray = srcData.getIntData(0);

        int dstBandOffset = dstData.getOffset(0);
        final int dstPixelStride = dstData.pixelStride;
        final int dstScanlineStride = dstData.lineStride;
        final float[] dstDataArray = dstData.getFloatData(0);

        int[][] gridPointIndexCache = new int[2][dstWidth];
        float[][] valueCache = new float[2][dstWidth];
        for (int y = 0; y < dstHeight; y++) {
            int srcPixelOffset = srcBandOffset;
            int dstPixelOffset = dstBandOffset;
            for (int x = 0; x < dstWidth; x++) {
                final int seqnum = srcDataArray[srcPixelOffset];
                final int gridPointIndex = smosFile.getGridPointId(seqnum);
                float btValue = noDataValue;
                if (gridPointIndex != -1) {
                    if (x > 0 && gridPointIndexCache[1][x - 1] == gridPointIndex) {
                        // this line, pixel to the left
                        btValue = valueCache[1][x - 1];
                    } else if (y > 0 && gridPointIndexCache[0][x] == gridPointIndex) {
                        // last line, pixel above
                        btValue = valueCache[0][x];
                    } else if (x > 0 && y > 0 && gridPointIndexCache[0][x - 1] == gridPointIndex) {
                        // last line, pixel to the left
                        btValue = valueCache[0][x - 1];
                    } else if (x < dstWidth - 1 && y > 0 && gridPointIndexCache[0][x + 1] == gridPointIndex) {
                        // last line, pixel to the right
                        btValue = valueCache[0][x + 1];
                    } else {
                        btValue = smosFile.getL1CBtDataFloat(gridPointIndex, fieldIndex);
                    }
                }
                valueCache[1][x] = btValue;
                gridPointIndexCache[1][x] = gridPointIndex;

                dstDataArray[dstPixelOffset] = btValue;
                srcPixelOffset += srcPixelStride;
                dstPixelOffset += dstPixelStride;
            }
            int[] t1 = gridPointIndexCache[0];
            gridPointIndexCache[0] = gridPointIndexCache[1];
            gridPointIndexCache[1] = t1;
            float[] t2 = valueCache[0];
            valueCache[0] = valueCache[1];
            valueCache[1] = t2;

            srcBandOffset += srcScanlineStride;
            dstBandOffset += dstScanlineStride;
        }
    }

    @Override
    public String toString() {
        String className = getClass().getSimpleName();
        String productName = "";
        if (smosBand.getProduct() != null) {
            productName = ":" + smosBand.getProduct().getName();
        }
        String bandName = "." + smosBand.getName();
        return className + productName + bandName;
    }
}
