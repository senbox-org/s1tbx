package org.esa.snap.watermask.util;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.watermask.operator.WatermaskClassifier;

import java.awt.*;
import java.awt.image.Raster;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

/* Creates a property file which will contain the land-water classification for all tiles
   which do not exist in the shape-file set.
   To determine the classification for missing tiles a IGBP map was used.
   namely: gigbp1_2g.img
   To use this map, the correct reader must be included.
        <dependency>
            <groupId>org.esa.beam</groupId>
            <artifactId>beam-igbp-glcc-reader</artifactId>
            <version>2.0-SNAPSHOT</version>
        </dependency>
   3 tiles are not correctly classified, these are handled separately
 */
class MissingTilesPropertyFileGenerator {

    private MissingTilesPropertyFileGenerator() {
    }

    public static void main(String[] args) throws IOException {
        final Properties properties = new Properties();
        final File directory = new File(args[0]);
        final Product product = ProductIO.readProduct(new File(args[1]));
        final Band land_water = product.getBandAt(0);
        for (int x = -180; x < 180; x++) {
            for (int y = 89; y >= -90; y--) {
                final String tileFileName = getTileFileName(x, y);
                final String propertyKey = tileFileName.substring(0, tileFileName.indexOf('.'));
                if (y >= 60 || y <= -60) {
                    properties.setProperty(propertyKey, WatermaskClassifier.INVALID_VALUE + "");
                } else if (!new File(directory, tileFileName).exists()) {
                    System.out.printf("Not existing: %s%n", tileFileName);
                    final int landWater = getLandWaterValue(land_water, x, y);
                    properties.setProperty(propertyKey, String.valueOf(landWater));
                }
            }
        }
        // these tiles are not correctly classified by the IGBP reference
        properties.setProperty("e034n18", "0");
        properties.setProperty("e019n09", "0");
        properties.setProperty("e026n00", "0");
        final FileWriter writer = new FileWriter(new File(directory, "MissingTiles.properties"));

        try {
            properties.store(writer, null);
        } finally {
            writer.close();
        }
    }

    private static int getLandWaterValue(Band landWaterBand, int x, int y) {
        final PixelPos pixelPos = landWaterBand.getGeoCoding().getPixelPos(new GeoPos(y, x), null);
        final Raster raster = landWaterBand.getSourceImage().getData(
                new Rectangle((int) pixelPos.getX(), (int) pixelPos.getY(), 1, 1));
        final int sample = raster.getSample((int) pixelPos.getX(), (int) pixelPos.getY(), 0);
        return sample == 17 ? WatermaskClassifier.WATER_VALUE : WatermaskClassifier.LAND_VALUE;
    }

    static String getTileFileName(double lon, double lat) {
        final boolean geoPosIsWest = lon < 0;
        final boolean geoPosIsSouth = lat < 0;
        final String eastOrWest = geoPosIsWest ? "w" : "e";
        final String northOrSouth = geoPosIsSouth ? "s" : "n";
        return String.format("%s%03d%s%02d.img",
                             eastOrWest, (int) Math.abs(Math.floor(lon)),
                             northOrSouth, (int) Math.abs(Math.floor(lat)));
    }
}
