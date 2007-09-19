package org.esa.beam.util.jai;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.util.ImageUtils;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.SourcelessOpImage;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferUShort;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferDouble;
import java.awt.image.ColorModel;
import java.io.IOException;


public class RasterDataNodeOpImage extends SourcelessOpImage {
    private RasterDataNode rasterDataNode;

    protected RasterDataNodeOpImage(RasterDataNode rasterDataNode, ImageLayout imageLayout) {
        super(imageLayout,
              null,
              imageLayout.getSampleModel(null),
              imageLayout.getMinX(null),
              imageLayout.getMinY(null),
              imageLayout.getWidth(null),
              imageLayout.getHeight(null));
        this.rasterDataNode = rasterDataNode;
        // todo - use rendering hints
        setTileCache(JAI.getDefaultInstance().getTileCache());
    }

    public static RasterDataNodeOpImage create(RasterDataNode band) {
        ImageLayout imageLayout = createSingleBandedImageLayout(band);
        return new RasterDataNodeOpImage(band, imageLayout);
    }

    public RasterDataNode getRasterDataNode() {
        return rasterDataNode;
    }

    @Override
    protected void computeRect(PlanarImage[] sourceImages, WritableRaster tile, Rectangle destRect) {
        ProductData productData;
        boolean directMode = tile.getDataBuffer().getSize() == destRect.width * destRect.height;
        if (directMode) {
            productData = ProductData.createInstance(rasterDataNode.getDataType(), ImageUtils.getDataBufferArray(tile.getDataBuffer()));
        } else {
            productData = ProductData.createInstance(rasterDataNode.getDataType(), destRect.width * destRect.height);
        }

        try {
            rasterDataNode.readRasterData(destRect.x, destRect.y, destRect.width, destRect.height, productData, ProgressMonitor.NULL);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (!directMode) {
            tile.setDataElements(destRect.x, destRect.y, destRect.width, destRect.height, productData.getElems());
        }
    }

    public WritableRaster createWritableRaster(Rectangle tileBounds) {
        SampleModel sampleModel = ImageUtils.createSingleBandedSampleModel(getDataBufferType(rasterDataNode.getDataType()), tileBounds.width, tileBounds.height);
        return createWritableRaster(sampleModel, new Point(tileBounds.x, tileBounds.y));
    }

    @Override
    public String toString() {
        return getClass().getName() + "[rasterDataNode=" + rasterDataNode + "]";
    }

    public static ImageLayout createSingleBandedImageLayout(RasterDataNode rasterDataNode) {
        SampleModel sampleModel = ImageUtils.createSingleBandedSampleModel(getDataBufferType(rasterDataNode.getDataType()),
                                                                           rasterDataNode.getSceneRasterWidth(),
                                                                           rasterDataNode.getSceneRasterHeight());
        ColorModel colorModel = createColorModel(sampleModel);
        Dimension tileSize = JAI.getDefaultTileSize();  // todo use rendering hints
// todo - check if the following operations improve performance
//        tileSize.width = Math.min(tileSize.width, rasterDataNode.getSceneRasterWidth());
//        tileSize.height = Math.min(tileSize.height, rasterDataNode.getSceneRasterHeight());
        return new ImageLayout(0, 0,
                               rasterDataNode.getSceneRasterWidth(),
                               rasterDataNode.getSceneRasterHeight(),
                               0, 0,
                               tileSize.width, tileSize.height,
                               sampleModel,
                               colorModel);
    }

    private static int getDataBufferType(int productDataType) {
        int dataBufferType;
        switch (productDataType) {
            case ProductData.TYPE_INT8:
            case ProductData.TYPE_UINT8:
                dataBufferType = DataBuffer.TYPE_BYTE;
                break;
            case ProductData.TYPE_INT16:
                dataBufferType = DataBuffer.TYPE_SHORT;
                break;
            case ProductData.TYPE_UINT16:
                dataBufferType = DataBuffer.TYPE_USHORT;
                break;
            case ProductData.TYPE_INT32:
            case ProductData.TYPE_UINT32:
                dataBufferType = DataBuffer.TYPE_INT;
                break;
            case ProductData.TYPE_FLOAT32:
                dataBufferType = DataBuffer.TYPE_FLOAT;
                break;
            case ProductData.TYPE_FLOAT64:
                dataBufferType = DataBuffer.TYPE_DOUBLE;
                break;
            default:
                throw new IllegalArgumentException("productDataType");
        }
        return dataBufferType;
    }

    /*
     * May be used later in order to avoid copies between AWT rasters and ProductData
     */
    private static DataBuffer wrapIntoDataBuffer(ProductData productData) {
        switch (productData.getType()) {
            case ProductData.TYPE_INT8:
            case ProductData.TYPE_UINT8:
                return new DataBufferByte((byte[]) productData.getElems(), productData.getNumElems());
            case ProductData.TYPE_INT16:
            case ProductData.TYPE_UINT16:
                return new DataBufferUShort((short[]) productData.getElems(), productData.getNumElems());
            case ProductData.TYPE_INT32:
            case ProductData.TYPE_UINT32:
                return new DataBufferInt((int[]) productData.getElems(), productData.getNumElems());
            case ProductData.TYPE_FLOAT32:
                return new DataBufferFloat((float[]) productData.getElems(), productData.getNumElems());
            case ProductData.TYPE_FLOAT64:
                return new DataBufferDouble((double[]) productData.getElems(), productData.getNumElems());
            default:
                throw new IllegalArgumentException("productData");
        }
    }
}
