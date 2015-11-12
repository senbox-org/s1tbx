package org.esa.snap.core.image;

import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;

import java.awt.Rectangle;
import java.io.IOException;

/**
 * An {@link RasterDataNodeOpImage} that computes its sample values independently of each other.
 *
 * @author Norman Fomferra
 * @since BEAM 4.9
 */
public abstract class RasterDataNodeSampleOpImage extends RasterDataNodeOpImage {
    public RasterDataNodeSampleOpImage(RasterDataNode rasterDataNode, ResolutionLevel level) {
        super(rasterDataNode, level);
    }

    @Override
    protected void computeProductData(ProductData productData, Rectangle destRect) throws IOException {
        final int sourceWidth = getRasterDataNode().getRasterWidth();
        final int sourceHeight = getRasterDataNode().getRasterHeight();
        final int targetWidth = destRect.width;
        final int targetHeight = destRect.height;
        final int sourceX0 = getSourceX(destRect.x);
        final int sourceY0 = getSourceY(destRect.y);
        final int[] sourceXs = getSourceCoords(sourceWidth, targetWidth);
        final int[] sourceYs = getSourceCoords(sourceHeight, targetHeight);
        int elemIndex = 0;
        for (int j = 0; j < targetHeight; j++) {
            final int sourceY = sourceY0 + sourceYs[j];
            for (int i = 0; i < targetWidth; i++) {
                productData.setElemDoubleAt(elemIndex, computeSample(sourceX0 + sourceXs[i], sourceY));
                elemIndex++;
            }
        }
    }

    /**
     * Computes the sample value at the given source pixel coordinates {@code sourceX} and {@code sourceY}.
     *
     * @param sourceX The source pixel X coordinate of the sample to be computed.
     * @param sourceY The source pixel Y coordinate of the sample to be computed.
     * @return The sample value.
     */
    protected abstract double computeSample(int sourceX, int sourceY);
}
