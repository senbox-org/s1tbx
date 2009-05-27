package org.esa.beam.jai;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ProductData;

import java.awt.Rectangle;
import java.io.IOException;


/**
 * An {@code OpImage} which retrieves its data from the product reader associated with the
 * given {@code RasterDataNode} at a given pyramid level.
 */
public class BandOpImage extends RasterDataNodeOpImage {

    public BandOpImage(Band band) {
        super(band, ResolutionLevel.MAXRES);
    }

    public BandOpImage(Band band, ResolutionLevel level) {
        super(band, level);
    }

    public Band getBand() {
        return (Band) getRasterDataNode();
    }

    @Override
    protected void computeProductData(ProductData productData, Rectangle destRect) throws IOException {
        final ProductReader productReader = getBand().getProductReader();
        if (getLevel() == 0) {
            productReader.readBandRasterData(getBand(), destRect.x, destRect.y,
                                             destRect.width, destRect.height,
                                             productData,
                                             ProgressMonitor.NULL);
        } else {
            final int sourceWidth = getSourceWidth(destRect.width);
            ProductData lineData = ProductData.createInstance(getBand().getDataType(), sourceWidth);
            int[] sourceCoords = getSourceCoords(sourceWidth, destRect.width);
            for (int y = 0; y < destRect.height; y++) {
                productReader.readBandRasterData(getBand(),
                                                 getSourceX(destRect.x),
                                                 getSourceY(destRect.y + y),
                                                 lineData.getNumElems(), 1,
                                                 lineData,
                                                 ProgressMonitor.NULL);
                copyLine(y, destRect.width, lineData, productData, sourceCoords);
            }
        }
    }
}
