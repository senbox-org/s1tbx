package org.esa.beam.jai;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.util.ImageUtils;

import javax.media.jai.PlanarImage;
import java.awt.*;
import java.awt.image.WritableRaster;
import java.io.IOException;


/**
 * An {@code OpImage} which retrieves its data from the product reader associated with the
 * given {@code RasterDataNode} at a given pyramid level.
 */
public class BandOpImage extends SingleBandedOpImage {
    private RasterDataNode rasterDataNode;

    public BandOpImage(RasterDataNode rasterDataNode) {
        this(rasterDataNode, null, 0);
    }

    private BandOpImage(RasterDataNode rasterDataNode, LevelOpImage level0Image, int level) {
        super(ImageManager.getDataBufferType(rasterDataNode.getDataType()),
              rasterDataNode.getSceneRasterWidth(),
              rasterDataNode.getSceneRasterHeight(),
              rasterDataNode.getProduct().getPreferredTileSize(), level0Image, level,
              null);
        this.rasterDataNode = rasterDataNode;
    }

    @Override
    protected LevelOpImage createDownscaledImage(int level) {
        return new BandOpImage(rasterDataNode, getLevel0Image(), level);
    }

    public RasterDataNode getRasterDataNode() {
        return rasterDataNode;
    }

    @Override
    protected void computeRect(PlanarImage[] sourceImages, WritableRaster tile, Rectangle destRect) {
        ProductData productData;
        boolean directMode = tile.getDataBuffer().getSize() == destRect.width * destRect.height;
        if (directMode) {
            productData = ProductData.createInstance(rasterDataNode.getDataType(),
                                                     ImageUtils.getPrimitiveArray(tile.getDataBuffer()));
        } else {
            productData = ProductData.createInstance(rasterDataNode.getDataType(),
                                                     destRect.width * destRect.height);
        }

        try {
            computeProductData(productData, destRect);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (!directMode) {
            tile.setDataElements(destRect.x, destRect.y, destRect.width, destRect.height, productData.getElems());
        }
    }

    private void computeProductData(ProductData productData, Rectangle destRect) throws IOException {
        if (getLevel() == 0) {
            if (rasterDataNode instanceof TiePointGrid) {
                rasterDataNode.readPixels(destRect.x, destRect.y,
                                          destRect.width, destRect.height,
                                          (float[]) productData.getElems(),
                                          ProgressMonitor.NULL);
            } else {
                rasterDataNode.readRasterData(destRect.x, destRect.y,
                                              destRect.width, destRect.height,
                                              productData,
                                              ProgressMonitor.NULL);
            }
        } else {
            final int sourceWidth = getSourceWidth(destRect.width);
            ProductData lineData = ProductData.createInstance(rasterDataNode.getDataType(), sourceWidth);
            for (int y = 0; y < destRect.height; y++) {
                if (rasterDataNode instanceof TiePointGrid) {
                    rasterDataNode.readPixels(getSourceX(destRect.x),
                                              getSourceY(destRect.y + y),
                                              sourceWidth, 1,
                                              (float[]) lineData.getElems(),
                                              ProgressMonitor.NULL);
                } else {
                    rasterDataNode.readRasterData(getSourceX(destRect.x),
                                                  getSourceY(destRect.y + y),
                                                  lineData.getNumElems(), 1,
                                                  lineData,
                                                  ProgressMonitor.NULL);
                }
                // TODO - optimize this copy loop!!!
                for (int x = 0; x < destRect.width; x++) {
                    int i = getSourceCoord(x, 0, sourceWidth - 1);
                    productData.setElemDoubleAt(y * destRect.width + x, lineData.getElemDoubleAt(i));
                }
            }
        }
    }

    @Override
    public synchronized void dispose() {
        rasterDataNode = null;
        super.dispose();
    }

    @Override
    public String toString() {
        String className = getClass().getSimpleName();
        String productName = "";
        if (rasterDataNode.getProduct() != null) {
            productName = ":" + rasterDataNode.getProduct().getName();
        }
        String bandName = "." + rasterDataNode.getName();
        return className + productName + bandName;
    }

}
