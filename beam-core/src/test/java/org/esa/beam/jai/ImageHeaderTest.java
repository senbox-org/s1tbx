package org.esa.beam.jai;

import junit.framework.TestCase;
import org.esa.beam.util.ImageUtils;

import javax.media.jai.ImageLayout;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;

public class ImageHeaderTest extends TestCase {
    public void testLoad() throws IOException {
        final ImageHeader imageHeader = ImageHeader.load(new InputStreamReader(ImageHeaderTest.class.getResourceAsStream("image.properties")),
                                                         null, null);
        assertEquals("raw", imageHeader.getTileFormat());
        assertNotNull(imageHeader.getImageLayout());
        assertEquals(1, imageHeader.getImageLayout().getMinX(null));
        assertEquals(-1, imageHeader.getImageLayout().getMinY(null));
        assertEquals(1280, imageHeader.getImageLayout().getWidth(null));
        assertEquals(1024, imageHeader.getImageLayout().getHeight(null));
        assertEquals(0, imageHeader.getImageLayout().getTileGridXOffset(null));
        assertEquals(6, imageHeader.getImageLayout().getTileGridYOffset(null));
        assertEquals(512, imageHeader.getImageLayout().getTileWidth(null));
        assertEquals(256, imageHeader.getImageLayout().getTileHeight(null));
        assertNull(imageHeader.getImageLayout().getColorModel(null));
        assertNotNull(imageHeader.getImageLayout().getSampleModel(null));
        assertEquals(1, imageHeader.getImageLayout().getSampleModel(null).getNumBands());
        assertEquals(4, imageHeader.getImageLayout().getSampleModel(null).getDataType());
    }

    public void testStoreAndLoad() throws IOException {
        int minX = 1;
        int minY = -1;
        int width = 1280;
        int height = 1024;
        int tileGridXOffset = 0;
        int tileGridYOffset = 6;
        int tileWidth = 512;
        int tileHeight = 256;
        SampleModel sampleModel = ImageUtils.createSingleBandedSampleModel(DataBuffer.TYPE_FLOAT, width, height);
        ColorModel colorModel = null;

        ImageLayout imageLayout = new ImageLayout(minX, minY,
                                                  width, height,
                                                  tileGridXOffset,
                                                  tileGridYOffset,
                                                  tileWidth, tileHeight,
                                                  sampleModel, colorModel);


        StringWriter writer = new StringWriter();

        ImageHeader imageHeader = new ImageHeader(imageLayout, "raw");

        imageHeader.store(writer, null);

        imageHeader = ImageHeader.load(new StringReader(writer.toString()), null, null);

        assertNotNull(imageHeader.getImageLayout());
        assertEquals(minX, imageHeader.getImageLayout().getMinX(null));
        assertEquals(minY, imageHeader.getImageLayout().getMinY(null));
        assertEquals(width, imageHeader.getImageLayout().getWidth(null));
        assertEquals(height, imageHeader.getImageLayout().getHeight(null));
        assertEquals(tileGridXOffset, imageHeader.getImageLayout().getTileGridXOffset(null));
        assertEquals(tileGridYOffset, imageHeader.getImageLayout().getTileGridYOffset(null));
        assertEquals(tileWidth, imageHeader.getImageLayout().getTileWidth(null));
        assertEquals(tileHeight, imageHeader.getImageLayout().getTileHeight(null));
        assertNull(imageHeader.getImageLayout().getColorModel(null));
        assertNotNull(imageHeader.getImageLayout().getSampleModel(null));
        assertEquals(sampleModel.getNumBands(), imageHeader.getImageLayout().getSampleModel(null).getNumBands());
        assertEquals(sampleModel.getDataType(), imageHeader.getImageLayout().getSampleModel(null).getDataType());
    }
}
