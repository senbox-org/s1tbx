package org.esa.beam.jai;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.TiePointGrid;

import java.awt.Rectangle;
import java.io.IOException;


/**
 * An {@code OpImage} which retrieves its data from the product reader associated with the
 * given {@code RasterDataNode} at a given pyramid level.
 */
public class TiePointGridOpImage extends RasterDataNodeOpImage {

    public TiePointGridOpImage(TiePointGrid band, ResolutionLevel level) {
        super(band, level);
    }

    public TiePointGrid getTiePointGrid() {
        return (TiePointGrid) getRasterDataNode();
    }

    @Override
    protected void computeProductData(ProductData productData, Rectangle destRect) throws IOException {
        if (getLevel() == 0) {
            getTiePointGrid().readPixels(destRect.x, destRect.y,
                                         destRect.width, destRect.height,
                                         (float[]) productData.getElems(),
                                         ProgressMonitor.NULL);
        } else {
            final int sourceWidth = getSourceWidth(destRect.width);
            ProductData lineData = ProductData.createInstance(getTiePointGrid().getDataType(), sourceWidth);
            int[] sourceCoords = getSourceCoords(sourceWidth, destRect.width);
            for (int y = 0; y < destRect.height; y++) {
                getTiePointGrid().readPixels(getSourceX(destRect.x),
                                             getSourceY(destRect.y + y),
                                             sourceWidth, 1,
                                             (float[]) lineData.getElems(),
                                             ProgressMonitor.NULL);
                copyLine(y, destRect.width, lineData, productData, sourceCoords);
            }
        }
    }

}