package org.esa.beam.dataio.smos;

import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import com.bc.ceres.glevel.support.DefaultMultiLevelSource;
import org.esa.beam.jai.TiledFileOpImage;

import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import javax.media.jai.PlanarImage;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DggridTilizer {
    private File inputLevel0Dir;
    private File outputDir;

    public static void main(String[] args) throws IOException {
        new DggridTilizer().doit(new File(args[0]), new File(args[1]));
    }

    private void doit(File inputLevel0Dir, File outputDir) throws IOException {
        this.inputLevel0Dir = inputLevel0Dir;
        this.outputDir = outputDir;
        outputDir.mkdir();

        final TiledFileOpImage opImage = TiledFileOpImage.create(inputLevel0Dir, null);
        final int dataType = opImage.getSampleModel().getDataType();
        int tileWidth = 512;
        int tileHeight = 512;
        final int levelCount = 7;
        final MultiLevelModel model = new DefaultMultiLevelModel(levelCount, new AffineTransform(), opImage.getWidth(), opImage.getHeight());
        final MultiLevelSource multiLevelSource = new DefaultMultiLevelSource(opImage, model);

        for (int level = 5; level < levelCount; level++) {

            final PlanarImage image = PlanarImage.wrapRenderedImage(multiLevelSource.getImage(level));

            final int width = image.getWidth();
            final int height = image.getHeight();

            int numXTiles;
            int numYTiles;
            while (true) {
                numXTiles = width / tileWidth;
                numYTiles = height / tileHeight;
                if (numXTiles * tileWidth == width && numYTiles * tileHeight == image.getHeight()) {
                    break;
                }
                if (numXTiles * tileWidth < width) {
                    tileWidth /= 2;
                }
                if (numYTiles * tileHeight < height) {
                    tileHeight /= 2;
                }
            }
            if (numXTiles == 0 || numYTiles == 0) {
                throw new IllegalStateException("numXTiles == 0 || numYTiles == 0");
            }
            if (tileWidth < 512 && tileHeight < 512) {
                tileWidth = width;
                tileHeight = height;
                numXTiles = numYTiles = 1;
            }

            final File outputLevelDir = new File(outputDir, "" + level);
            outputLevelDir.mkdir();
            final File imagePropertiesFile = new File(outputLevelDir, "image.properties");
            System.out.println("Writing " + imagePropertiesFile + "...");
            final PrintWriter printWriter = new PrintWriter(new FileWriter(imagePropertiesFile));
            writeImageProperties(level, dataType, width, height, tileWidth, tileHeight, numXTiles, numYTiles,
                                 new PrintWriter(System.out));
            writeImageProperties(level, dataType, width, height, tileWidth, tileHeight, numXTiles, numYTiles,
                                 printWriter);
            System.out.flush();
            printWriter.close();

            writeTiles(outputLevelDir, image, tileWidth, tileHeight, numXTiles, numYTiles);
        }
    }

    private void writeTiles(File levelDir, PlanarImage image, int tileWidth, int tileHeight, int numXTiles, int numYTiles) throws IOException {
        for (int tileY = 0; tileY < numYTiles; tileY++) {
            for (int tileX = 0; tileX < numXTiles; tileX++) {
                final int x = tileX * tileWidth;
                final int y = tileY * tileHeight;
                final Raster raster = image.getData(new Rectangle(x, y, tileWidth, tileHeight));
                int[] data = ((DataBufferInt) raster.getDataBuffer()).getData();
                if (data.length != tileWidth * tileHeight) {
                    data = new int[tileWidth * tileHeight];
                    raster.getDataElements(x, y, tileWidth, tileHeight, data);
                }
                writeData(levelDir, tileX, tileY, data);
            }
        }
    }

    private void writeImageProperties(int level, int dataType, int width, int height, int tileWidth, int tileHeight, int numXTiles, int numYTiles, PrintWriter printWriter) {
        printWriter.println("level      = " + level);
        printWriter.println("dataType   = " + dataType);
        printWriter.println("width      = " + width);
        printWriter.println("height     = " + height);
        printWriter.println("tileWidth  = " + tileWidth);
        printWriter.println("tileHeight = " + tileHeight);
        printWriter.println("numXTiles  = " + numXTiles);
        printWriter.println("numYTiles  = " + numYTiles);
    }

    private void writeData(File levelDir, int tileX, int tileY, int[] data) throws IOException {
        final String baseName = tileX + "-" + tileY + ".raw";
        final File file = new File(levelDir, baseName + ".zip");
        final ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(file));
        zipOutputStream.putNextEntry(new ZipEntry(baseName));
        final ImageOutputStream imageOutputStream = new MemoryCacheImageOutputStream(zipOutputStream);
        imageOutputStream.writeInts(data, 0, data.length);
        imageOutputStream.flush();
        zipOutputStream.closeEntry();
        zipOutputStream.close();
    }
}
