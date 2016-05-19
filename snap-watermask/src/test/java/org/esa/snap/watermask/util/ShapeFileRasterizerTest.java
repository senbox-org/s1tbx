package org.esa.snap.watermask.util;

import org.esa.snap.watermask.operator.WatermaskUtils;
import org.junit.Test;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.zip.ZipFile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ShapeFileRasterizerTest  {

    @Test
    public void testImageCreation() throws Exception {
        final File targetDir = new File("");
        final ShapeFileRasterizer rasterizer = new ShapeFileRasterizer(targetDir);
        final URL shapeUrl = getClass().getResource("e000n05f.shp");
        final File shapeFile = new File(shapeUrl.getFile());
        final int resolution = WatermaskUtils.computeSideLength(150);
        final BufferedImage image = rasterizer.createImage(shapeFile, resolution);

        // test some known values
        final Raster imageData = image.getData();
        assertEquals(0, imageData.getSample(526, 92, 0));
        assertEquals(1, imageData.getSample(312, 115, 0));
        assertEquals(1, imageData.getSample(141, 187, 0));
        assertEquals(0, imageData.getSample(197, 27, 0));
        assertEquals(1, imageData.getSample(308, 701, 0));
    }

     @Test
    public void testFromZip() throws Exception {
        final File targetDir = new File("");
        final ShapeFileRasterizer rasterizer = new ShapeFileRasterizer(targetDir);
        final URL shapeUrl = getClass().getResource("e000n05f.zip");
         final int tileSize = WatermaskUtils.computeSideLength(150);
         final List<File> tempFiles;
         final ZipFile zipFile = new ZipFile(shapeUrl.getFile());
         try {
             tempFiles = rasterizer.createTempFiles(zipFile);
         } finally {
             zipFile.close();
         }

         BufferedImage image = null;
         for (File file : tempFiles) {
             if (file.getName().endsWith("shp")) {
                 image = rasterizer.createImage(file, tileSize);
             }
         }
         assertNotNull(image);

//        // test some known values
        final Raster imageData = image.getData();
        assertEquals(0, imageData.getSample(526, 92, 0));
        assertEquals(1, imageData.getSample(312, 115, 0));
        assertEquals(1, imageData.getSample(141, 187, 0));
        assertEquals(0, imageData.getSample(197, 27, 0));
        assertEquals(1, imageData.getSample(308, 701, 0));

    }

}
