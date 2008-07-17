package org.esa.beam.dataio.smos;

import com.bc.ceres.glayer.level.DownscalableImage;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.glayer.level.TiledFileLevelImage;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.jai.LevelOpImage;
import org.esa.beam.jai.SingleBandedOpImage;

import javax.media.jai.PixelAccessor;
import javax.media.jai.PlanarImage;
import javax.media.jai.UnpackedImageData;
import java.awt.*;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.IOException;

public class SmosL1BandOpImage extends SingleBandedOpImage implements DownscalableImage {

    private final SmosFile smosFile;
    private Band smosBand;
    private final int btDataIndex;
    private final PlanarImage seqnumImage;
    private final TiledFileLevelImage dggridLevelImage;

    public SmosL1BandOpImage(SmosFile smosFile,
                             Band smosBand,
                             int btDataIndex,
                             TiledFileLevelImage dggridLevelImage,
                             int level) {
        super(ImageManager.getDataBufferType(smosBand.getDataType()),
              smosBand.getSceneRasterWidth(),
              smosBand.getSceneRasterHeight(),
              smosBand.getProduct().getPreferredTileSize(), null, level,
              null);

        this.smosFile = smosFile;
        this.smosBand = smosBand;
        this.btDataIndex = btDataIndex;
        this.dggridLevelImage = dggridLevelImage;
        this.seqnumImage = dggridLevelImage.getPlanarImage(level);
    }

    @Override
    protected void computeRect(PlanarImage sources[], WritableRaster destRaster, Rectangle destRect) {
        try {
            Raster raster = seqnumImage.getData(destRect);
            SampleModel srcSampleModel = raster.getSampleModel();
            PixelAccessor seqnumAccessor = new PixelAccessor(srcSampleModel, seqnumImage.getColorModel());
            PixelAccessor btAccessor = new PixelAccessor(destRaster.getSampleModel(), null);

            int srcTransferType = srcSampleModel.getTransferType();

            UnpackedImageData seqnumData = seqnumAccessor.getPixels(
                    raster, destRect, srcTransferType, false);

            UnpackedImageData btData = btAccessor.getPixels(
                    destRaster, destRect, destRaster.getSampleModel().getTransferType(), true);


            final int targetWidth = destRect.width;
            final int targetHeight = destRect.height;

            final int[] srcDataArray = seqnumData.getIntData(0);
            int srcBandOffset = seqnumData.getOffset(0);
            final int srcPixelStride = seqnumData.pixelStride;
            final int srcScanlineStride = seqnumData.lineStride;

            final float[] dstDataArray = btData.getFloatData(0);
            int dstBandOffset = btData.getOffset(0);
            final int dstPixelStride = btData.pixelStride;
            final int dstScanlineStride = btData.lineStride;

            int[][] gridPointIndexCache = new int[2][targetWidth];
            float[][] valueCache = new float[2][targetWidth];
            for (int y = 0; y < targetHeight; y++) {
                int srcPixelOffset = srcBandOffset;
                int dstPixelOffset = dstBandOffset;
                for (int x = 0; x < targetWidth; x++) {
                    final int seqnum = srcDataArray[srcPixelOffset];
                    final int gridPointIndex = smosFile.getGridPointId(seqnum);
                    float btValue = -999f;
                    if (gridPointIndex != -1) {
                        if (x > 0 && gridPointIndexCache[1][x - 1] == gridPointIndex) {
                            btValue = valueCache[1][x - 1];
                        } else if (y > 0 && gridPointIndexCache[0][x] == gridPointIndex) {
                            btValue = valueCache[0][x];
                        } else if (x > 0 && y > 0 && gridPointIndexCache[0][x - 1] == gridPointIndex) {
                            btValue = valueCache[0][x - 1];
                        } else if (x < targetWidth - 1 && y > 0 && gridPointIndexCache[0][x + 1] == gridPointIndex) {
                            btValue = valueCache[0][x + 1];
                        } else {
                            btValue = smosFile.getMeanBtData(gridPointIndex, btDataIndex);
                        }
                    }
                    dstDataArray[dstPixelOffset] = btValue;
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcBandOffset += srcScanlineStride;
                dstBandOffset += dstScanlineStride;
            }
            btAccessor.setPixels(btData);
        } catch (IOException e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }

    @Override
    protected LevelOpImage createDownscaledImage(int newLevel) {
        return new SmosL1BandOpImage(smosFile, smosBand, btDataIndex, dggridLevelImage, newLevel);
    }

    @Override
    public synchronized void dispose() {
        smosBand = null;
        super.dispose();
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
