package org.esa.beam.framework.gpf.internal;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.SourcelessOpImage;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.IOException;


public class RasterDataNodeOpImage extends SourcelessOpImage {
    private RasterDataNode rasterDataNode;

    protected RasterDataNodeOpImage(ImageLayout imageLayout, SampleModel sampleModel, RasterDataNode band) {
        super(imageLayout, null, sampleModel, 0, 0,
              band.getSceneRasterWidth(),
              band.getSceneRasterHeight());
        this.rasterDataNode = band;
        setTileCache(JAI.getDefaultInstance().getTileCache());
    }

    public static RasterDataNodeOpImage create(RasterDataNode band) {
        SampleModel sampleModel = ImageHelpers.createSingleBandSampleModel(band);
        ImageLayout imageLayout = ImageHelpers.createSingleBandImageLayout(band, sampleModel);
        return new RasterDataNodeOpImage(imageLayout, sampleModel, band);
    }

    public RasterDataNode getRasterDataNode() {
        return rasterDataNode;
    }

    @Override
    protected void computeRect(PlanarImage[] sourceImages, WritableRaster tile, Rectangle destRect) {
        ProductData productData;
        boolean directMode = tile.getDataBuffer().getSize() == destRect.width * destRect.height;
        if (directMode) {
            productData = ProductData.createInstance(rasterDataNode.getDataType(), ImageHelpers.getDataBufferArray(tile.getDataBuffer()));
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

    public WritableRaster createWritableRaster(Rectangle tile) {
        SampleModel sampleModel = createSampleModel(tile.width, tile.height);
        return createWritableRaster(sampleModel, new Point(tile.x, tile.y));
    }

    public SampleModel createSampleModel(int width, int height) {
        return ImageHelpers.createSingleBandSampleModel(ImageHelpers.getDataBufferType(rasterDataNode.getDataType()), width, height);
    }

    @Override
    public String toString() {
        return getClass().getName() + "[band=" + rasterDataNode + ",imageID=" + getImageID() + "]";
    }
}
