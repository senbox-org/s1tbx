package org.esa.beam.jai;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.util.ImageUtils;

import javax.media.jai.PlanarImage;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.WritableRaster;
import java.io.IOException;


/**
 * A base class for {@code OpImage}s acting as source image for
 * a {@link org.esa.beam.framework.datamodel.RasterDataNode}.
 */
public abstract class RasterDataNodeOpImage extends SingleBandedOpImage {
    private RasterDataNode rasterDataNode;

    private static Dimension getPreferredTileSize(RasterDataNode rdn) {
        Product product = rdn.getProduct();
        if (product != null && product.getPreferredTileSize() != null) {
            return product.getPreferredTileSize();
        }
        return null;
    }
    
    protected RasterDataNodeOpImage(RasterDataNode rasterDataNode, ResolutionLevel level) {
        super(ImageManager.getDataBufferType(rasterDataNode.getDataType()),
              rasterDataNode.getSceneRasterWidth(),
              rasterDataNode.getSceneRasterHeight(),
              getPreferredTileSize(rasterDataNode),
              null, level);
        this.rasterDataNode = rasterDataNode;
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

    protected abstract void computeProductData(ProductData productData, Rectangle destRect) throws IOException;

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
    
    protected void copyLine(final  int y, final int srcWidth, final int destWidth, ProductData src, ProductData dest) {
        final int destOffset = y * destWidth;
        final int type = src.getType();

        // TODO -  remove after sped comparison (mz) (28.10.2008)
//        for (int x = 0; x < destWidth; x++) {
//            int i = getSourceCoord(x, 0, srcWidth - 1);
//            dest.setElemDoubleAt(destOffset + x, src.getElemDoubleAt(i));
//        }
        
        switch (type) {
            case ProductData.TYPE_INT8:
            case ProductData.TYPE_UINT8:
                byte[] srcArrayB = (byte[]) src.getElems();
                byte[] destArrayB = (byte[]) dest.getElems();
                for (int x = 0; x < destWidth; x++) {
                    int i = getSourceCoord(x, 0, srcWidth - 1);
                    destArrayB[destOffset + x] =  srcArrayB[i];
                }
                return;
            case ProductData.TYPE_INT16:
            case ProductData.TYPE_UINT16:
                short[] srcArrayS = (short[]) src.getElems();
                short[] destArrayS = (short[]) dest.getElems();
                for (int x = 0; x < destWidth; x++) {
                    int i = getSourceCoord(x, 0, srcWidth - 1);
                    destArrayS[destOffset + x] =  srcArrayS[i];
                }
                return;
            case ProductData.TYPE_INT32:
            case ProductData.TYPE_UINT32:
                int[] srcArrayI = (int[]) src.getElems();
                int[] destArrayI = (int[]) dest.getElems();
                for (int x = 0; x < destWidth; x++) {
                    int i = getSourceCoord(x, 0, srcWidth - 1);
                    destArrayI[destOffset + x] =  srcArrayI[i];
                }
                return;
            case ProductData.TYPE_FLOAT32:
                float[] srcArrayF = (float[]) src.getElems();
                float[] destArrayF = (float[]) dest.getElems();
                for (int x = 0; x < destWidth; x++) {
                    int i = getSourceCoord(x, 0, srcWidth - 1);
                    destArrayF[destOffset + x] =  srcArrayF[i];
                }
                return;
            case ProductData.TYPE_FLOAT64:
                double[] srcArrayD = (double[]) src.getElems();
                double[] destArrayD = (double[]) dest.getElems();
                for (int x = 0; x < destWidth; x++) {
                    int i = getSourceCoord(x, 0, srcWidth - 1);
                    destArrayD[destOffset + x] =  srcArrayD[i];
                }
                return;
            case ProductData.TYPE_ASCII:
            case ProductData.TYPE_UTC:
            default:
                throw new IllegalArgumentException("wrong product data type: "+type);
        }
    }

}