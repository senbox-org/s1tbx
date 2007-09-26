package org.esa.beam.framework.gpf.internal;

import junit.framework.TestCase;
import org.esa.beam.util.jai.SingleBandedSampleModel;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.ProductData;

import javax.media.jai.SourcelessOpImage;
import javax.media.jai.ImageLayout;
import javax.media.jai.PlanarImage;
import javax.media.jai.PixelAccessor;
import javax.media.jai.UnpackedImageData;
import java.awt.image.SampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.Rectangle;
import java.util.Map;

public class RasterImplTest extends TestCase {
    private static final int IMAGE_W = 256;
    private static final int IMAGE_H = 256;
    private static final int TILE_SIZE = 64;

    public void testThatRastersAreWrittenCorrectly() {
        TestOpImage image = new TestOpImage();
        Raster[] rasters = image.getTiles();

    }


    private class TestOpImage extends SourcelessOpImage {
        private PixelAccessor pixelAccessor;

        public TestOpImage() {
            super(createTestImageLayout(), null, new SingleBandedSampleModel(DataBuffer.TYPE_FLOAT, IMAGE_W, IMAGE_H), 0, 0, IMAGE_W, IMAGE_H);
            pixelAccessor = new PixelAccessor(this);
        }


        @Override
        protected void computeRect(PlanarImage[] planarImages, WritableRaster writableRaster, Rectangle rectangle) {
            UnpackedImageData unpackedImageData = pixelAccessor.getPixels(writableRaster, rectangle, 0, true);


            System.out.println("--> writableRaster = " + writableRaster);
            System.out.println("    rectangle = " + rectangle);
        }
    }

    private static ImageLayout createTestImageLayout() {
        ImageLayout imageLayout = new ImageLayout();
        imageLayout.setTileWidth(TILE_SIZE);
        imageLayout.setTileHeight(TILE_SIZE);
        return imageLayout;
    }

    public static class RasterBibo implements org.esa.beam.framework.gpf.Raster {
        RasterDataNode rasterDataNode;
        WritableRaster writableRaster;
        UnpackedImageData imageData;
        Rectangle rectangle;


        public RasterBibo(RasterDataNode rasterDataNode, WritableRaster writableRaster, UnpackedImageData imageData, Rectangle rectangle) {
            this.rasterDataNode = rasterDataNode;
            this.writableRaster = writableRaster;
            this.imageData = imageData;
            this.rectangle = rectangle;
        }

        public Rectangle getRectangle() {
            return rectangle;
        }

        public int getOffsetX() {
            return writableRaster.getMinX();
        }

        public int getOffsetY() {
            return writableRaster.getMinY();
        }

        public int getWidth() {
            return writableRaster.getWidth();
        }

        public int getHeight() {
            return writableRaster.getHeight();
        }

        public RasterDataNode getRasterDataNode() {
            return rasterDataNode;
        }

        public ProductData getDataBuffer() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public int getInt(int x, int y) {
            return 0;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public void setInt(int x, int y, int v) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public float getFloat(int x, int y) {
            return 0;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public void setFloat(int x, int y, float v) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public double getDouble(int x, int y) {
            return 0;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public void setDouble(int x, int y, double v) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public boolean getBoolean(int x, int y) {
            return false;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public void setBoolean(int x, int y, boolean v) {
            //To change body of implemented methods use File | Settings | File Templates.
        }
    }
}
