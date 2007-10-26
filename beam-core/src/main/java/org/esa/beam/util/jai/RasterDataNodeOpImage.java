package org.esa.beam.util.jai;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.util.ImageUtils;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.SourcelessOpImage;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.*;
import java.io.IOException;


public class RasterDataNodeOpImage extends SourcelessOpImage {
    private RasterDataNode rasterDataNode;
    private ProgressMonitor progressMonitor;

    public RasterDataNodeOpImage(RasterDataNode rasterDataNode) {
        this(rasterDataNode, createSingleBandedImageLayout(rasterDataNode));
    }

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

    public ProgressMonitor getProgressMonitor() {
        return progressMonitor != null ? progressMonitor : ProgressMonitor.NULL;
    }

    public void setProgressMonitor(ProgressMonitor pm) {
        this.progressMonitor = pm;
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
            if (rasterDataNode instanceof TiePointGrid) {
                rasterDataNode.readPixels(destRect.x, destRect.y,
                                          destRect.width, destRect.height,
                                          (float[]) productData.getElems(),
                                          getProgressMonitor());
            } else {
                rasterDataNode.readRasterData(destRect.x, destRect.y,
                                              destRect.width, destRect.height,
                                              productData,
                                              getProgressMonitor());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (!directMode) {
            tile.setDataElements(destRect.x, destRect.y, destRect.width, destRect.height, productData.getElems());
        }
    }

    public WritableRaster createWritableRaster(Rectangle rectangle) {
        final int dataBufferType = getDataBufferType(rasterDataNode.getDataType());
        SampleModel sampleModel = ImageUtils.createSingleBandedSampleModel(dataBufferType, rectangle.width, rectangle.height);
        final Point location = new Point(rectangle.x, rectangle.y);
        return createWritableRaster(sampleModel, location);
    }

    public static ImageLayout createSingleBandedImageLayout(RasterDataNode rasterDataNode) {
        int dataBufferType = getDataBufferType(rasterDataNode.getDataType());
        return createSingleBandedImageLayout(rasterDataNode, dataBufferType);
    }

    public static ImageLayout createSingleBandedImageLayout(RasterDataNode rasterDataNode, int dataBufferType) {
        int width = rasterDataNode.getSceneRasterWidth();
        int height = rasterDataNode.getSceneRasterHeight();
        SampleModel sampleModel = ImageUtils.createSingleBandedSampleModel(dataBufferType,
                                                                           width,
                                                                           height);
        ColorModel colorModel = createColorModel(sampleModel);
        Dimension tileSize = getPreferredTileSize(rasterDataNode.getProduct());
        return new ImageLayout(0, 0,
                               width,
                               height,
                               0, 0,
                               tileSize.width, tileSize.height,
                               sampleModel,
                               colorModel);
    }

    private static Dimension getPreferredTileSize(Product product) {
        Dimension tileSize;
        final Dimension preferredTileSize = product.getPreferredTileSize();
        if (preferredTileSize != null) {
            tileSize = preferredTileSize;
        } else {
            tileSize = JAIUtils.computePreferredTileSize(product.getSceneRasterWidth(),
                                                         product.getSceneRasterHeight(), 1);
        }
        return tileSize;
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

    @Override
    public synchronized void dispose() {
        rasterDataNode = null;
        progressMonitor = null;
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

    public static ProgressMonitor setProgressMonitor(RenderedImage image, ProgressMonitor pm) {
        ProgressMonitor oldPm = ProgressMonitor.NULL;
        if (image instanceof RasterDataNodeOpImage) {
            final RasterDataNodeOpImage opImage = (RasterDataNodeOpImage) image;
            oldPm = opImage.getProgressMonitor();
            opImage.setProgressMonitor(pm);
        }
        return oldPm;
    }
}
