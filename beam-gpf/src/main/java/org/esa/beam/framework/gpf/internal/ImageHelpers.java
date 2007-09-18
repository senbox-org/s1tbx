package org.esa.beam.framework.gpf.internal;

import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import java.awt.Dimension;
import java.awt.image.*;

/**
 * Created by IntelliJ IDEA.
 * User: Norman
 * Date: 18.09.2007
 * Time: 16:42:51
 * To change this template use File | Settings | File Templates.
 */
public class ImageHelpers {
    public static Object getDataBufferArray(DataBuffer dataBuffer) {
        switch (dataBuffer.getDataType()) {
            case DataBuffer.TYPE_BYTE:
                return ((DataBufferByte) dataBuffer).getData();
            case DataBuffer.TYPE_SHORT:
                return ((DataBufferShort) dataBuffer).getData();
            case DataBuffer.TYPE_USHORT:
                return ((DataBufferUShort) dataBuffer).getData();
            case DataBuffer.TYPE_INT:
                return ((DataBufferInt) dataBuffer).getData();
            case DataBuffer.TYPE_FLOAT:
                return ((DataBufferFloat) dataBuffer).getData();
            case DataBuffer.TYPE_DOUBLE:
                return ((DataBufferDouble) dataBuffer).getData();
            default:
                throw new IllegalArgumentException("dataBuffer");
        }
    }

    public static Object createDataBufferArray(int dataBufferType, int size) {
        switch (dataBufferType) {
            case DataBuffer.TYPE_BYTE:
                return new byte[size];
            case DataBuffer.TYPE_SHORT:
            case DataBuffer.TYPE_USHORT:
                return new short[size];
            case DataBuffer.TYPE_INT:
                return new int[size];
            case DataBuffer.TYPE_FLOAT:
                return new float[size];
            case DataBuffer.TYPE_DOUBLE:
                return new double[size];
            default:
                throw new IllegalArgumentException("dataBuffer");
        }
    }

    public static int getDataBufferType(int productDataType) {
        int dataBufferType;
        switch (productDataType) {
            case ProductData.TYPE_INT8:
            case ProductData.TYPE_UINT8:
            case ProductData.TYPE_BOOLEAN:
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

    public static DataBuffer createDataBuffer(ProductData productData) {
        switch (productData.getType()) {
            case ProductData.TYPE_INT8:
            case ProductData.TYPE_UINT8:
            case ProductData.TYPE_BOOLEAN:
                return new DataBufferByte((byte[]) productData.getElems(), productData.getNumElems());
            case ProductData.TYPE_INT16:
                return new DataBufferShort((short[]) productData.getElems(), productData.getNumElems());
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

    protected static ImageLayout createSingleBandImageLayout(RasterDataNode rasterDataNode, SampleModel sampleModel) {
        ColorModel colorModel = PlanarImage.createColorModel(sampleModel);
        Dimension defaultTileSize = JAI.getDefaultTileSize();
        return new ImageLayout(0, 0,
                               rasterDataNode.getSceneRasterWidth(),
                               rasterDataNode.getSceneRasterHeight(),
                               0, 0,
                               defaultTileSize.width,
                               defaultTileSize.height,
                               sampleModel, colorModel);
    }

    public static SampleModel createSingleBandSampleModel(RasterDataNode rasterDataNode) {
        return createSingleBandSampleModel(getDataBufferType(rasterDataNode.getDataType()),
                                           rasterDataNode.getSceneRasterWidth(),
                                           rasterDataNode.getSceneRasterHeight());
    }

    public static SampleModel createSingleBandSampleModel(int dataBufferType, int width, int height) {
        // Note: The SingleBandSampleModel has shown to be about 2 times faster!
        //        return RasterFactory.createPixelInterleavedSampleModel(dataBufferType,
        //                                                               width,
        //                                                               height,
        //                                                               1);
        return new SingleBandSampleModel(dataBufferType, width, height);
    }

}
