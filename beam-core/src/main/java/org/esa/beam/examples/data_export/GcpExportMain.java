package org.esa.beam.examples.data_export;

import java.io.FileWriter;
import java.io.IOException;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;

/**
 * This example program extracts the geolocation information of a satellite product to an envi compatible
 * ground control points file.
 *
 * The program expects three program arguments
 * <ul>
 * <li>the gcp file to be written</li>
 * <li>the satellite product to be opened</li>
 * <li>the resolution of the GCPs, i.e. how many pixels inbetween two GCPs</li>
 * </ul>
 */
public class GcpExportMain {

    private static final String _GCP_LINE_SEPARATOR = System.getProperty("line.separator");

    public static void main(String[] args) {

        FileWriter writer = null;

        try {
            writer = new FileWriter(args[0]);

            // open product and extract the geocoding
            Product product = ProductIO.readProduct(args[1]);
            GeoCoding geoCoding = product.getGeoCoding();


            final int width = product.getSceneRasterWidth();
            final int height = product.getSceneRasterHeight();
            final int resolution = Integer.parseInt(args[2]);
            final int gcpWidth = Math.max(width / resolution + 1, 2); //2 minimum
            final int gcpHeight = Math.max(height / resolution + 1, 2);//2 minimum
            final float xMultiplier = 1f * (width - 1) / (gcpWidth - 1);
            final float yMultiplier = 1f * (height - 1) / (gcpHeight - 1);
            final PixelPos pixelPos = new PixelPos();
            final GeoPos geoPos = new GeoPos();

            writer.write(createLineString("; ENVI Registration GCP File"));
            for (int y = 0; y < gcpHeight; y++) {
                for (int x = 0; x < gcpWidth; x++) {
                    final float imageX = xMultiplier * x;
                    final float imageY = yMultiplier * y;
                    pixelPos.x = imageX + 0.5f;
                    pixelPos.y = imageY + 0.5f;
                    geoCoding.getGeoPos(pixelPos, geoPos);
                    final float mapX = geoPos.lon; //longitude
                    final float mapY = geoPos.lat; //latitude
                    writer.write(createLineString(mapX, mapY,
                            pixelPos.x + 1, // + 1 because ENVI uses a one-based pixel co-ordinate system
                            pixelPos.y + 1));
                }
            }
            writer.close();
            writer = null;
        } catch (IOException e) {
            e.printStackTrace();  
        }
    }

    private static String createLineString(final String str) {
        return str.concat(_GCP_LINE_SEPARATOR);
    }

    private static String createLineString(final float mapX, final float mapY, final float imageX, final float imageY) {
        return "" + mapX + "\t" + mapY + "\t" + imageX + "\t" + imageY + _GCP_LINE_SEPARATOR;
    }
}
