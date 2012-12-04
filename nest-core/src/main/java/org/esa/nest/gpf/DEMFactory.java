package org.esa.nest.gpf;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.dataop.dem.ElevationModel;
import org.esa.beam.framework.dataop.dem.ElevationModelDescriptor;
import org.esa.beam.framework.dataop.dem.ElevationModelRegistry;
import org.esa.beam.framework.dataop.resamp.ResamplingFactory;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.visat.VisatApp;

import java.util.Arrays;

/**
 * DEM Handling
 */
public class DEMFactory {

    public final static String AUTODEM = " (Auto Download)";
    
    public static ElevationModel createElevationModel(final String demName, final String demResamplingMethod) {
        final ElevationModelRegistry elevationModelRegistry = ElevationModelRegistry.getInstance();
        final ElevationModelDescriptor demDescriptor = elevationModelRegistry.getDescriptor(demName);
        if (demDescriptor == null) {
            throw new OperatorException("The DEM '" + demName + "' is not supported.");
        }

        if (demDescriptor.isInstallingDem()) {
            throw new OperatorException("The DEM '" + demName + "' is currently being installed.");
        }

        ElevationModel dem = demDescriptor.createDem(ResamplingFactory.createResampling(demResamplingMethod));
        if(dem == null) {
            throw new OperatorException("The DEM '" + demName + "' has not been installed.");
        }
        return dem;
    }

    public static void checkIfDEMInstalled(final String demName) {

        final ElevationModelRegistry elevationModelRegistry = ElevationModelRegistry.getInstance();
        final ElevationModelDescriptor demDescriptor = elevationModelRegistry.getDescriptor(demName);
        if (demDescriptor == null) {
            throw new OperatorException("The DEM '" + demName + "' is not supported.");
        }

        if (!demDescriptor.isInstallingDem() && !demDescriptor.isDemInstalled()) {
            if(!demDescriptor.installDemFiles(VisatApp.getApp())) {
                throw new OperatorException("DEM "+ demName +" must be installed first");
            }
        }
    }

    public static void validateDEM(final String demName, final Product srcProduct) {
        // check if outside dem area
        if(demName.contains("SRTM")) {
            final GeoCoding geocoding = srcProduct.getGeoCoding();
            final int w = srcProduct.getSceneRasterWidth();
            final int h = srcProduct.getSceneRasterHeight();
            final GeoPos geo1 = geocoding.getGeoPos(new PixelPos(0,0), null);
            final GeoPos geo2 = geocoding.getGeoPos(new PixelPos(w,0), null);
            final GeoPos geo3 = geocoding.getGeoPos(new PixelPos(w,h), null);
            final GeoPos geo4 = geocoding.getGeoPos(new PixelPos(0, h), null);

            if((geo1.getLat() > 60 && geo2.getLat() > 60 && geo3.getLat() > 60 && geo4.getLat() > 60) ||
                    (geo1.getLat() < -60 && geo2.getLat() < -60 && geo3.getLat() < -60 && geo4.getLat() < -60)) {
                throw new OperatorException("Entire image is outside of SRTM valid area.\nPlease use another DEM.");
            }
        }
    }

    public static void fillDEM(final float[][] localDEM, final float alt) {
        for (float[] row : localDEM) {
            Arrays.fill(row, alt);
        }
    }

    public static String appendAutoDEM(String demName) {
        if(demName.equals("GETASSE30") || demName.equals("SRTM 3Sec") || demName.equals("ACE2_5Min")
           || demName.equals("ACE30"))
            demName += AUTODEM;
        return demName;
    }

    /**
     * Read DEM for current tile.
     * @param dem the model
     * @param demNoDataValue the no data value of the dem
     * @param tileGeoRef the georeferencing of the target product
     * @param x0 The x coordinate of the pixel at the upper left corner of current tile.
     * @param y0 The y coordinate of the pixel at the upper left corner of current tile.
     * @param tileHeight The tile height.
     * @param tileWidth The tile width.
     * @param localDEM The DEM for the tile.
     * @return true if all dem values are valid
     * @throws Exception from DEM
     */
    public static boolean getLocalDEM(final ElevationModel dem, final float demNoDataValue,
                                       final TileGeoreferencing tileGeoRef,
                                       final int x0, final int y0,
                                       final int tileWidth, final int tileHeight,
                                       final float[][] localDEM) throws Exception {

        // Note: the localDEM covers current tile with 1 extra row above, 1 extra row below, 1 extra column to
        //       the left and 1 extra column to the right of the tile.

        final int maxY = y0 + tileHeight + 1;
        final int maxX = x0 + tileWidth + 1;
        final GeoPos geoPos = new GeoPos();

        float alt;
        boolean valid = false;
        for (int y = y0 - 1; y < maxY; y++) {
            final int yy = y - y0 + 1;

            for (int x = x0 - 1; x < maxX; x++) {
                tileGeoRef.getGeoPos(x, y, geoPos);
                /*if(!geoPos.isValid()) {
                    localDEM[yy][x - x0 + 1] = demNoDataValue;
                    continue;
                }
                if (geoPos.lon > 180) {
                    geoPos.lon -= 360;
                } else if (geoPos.lon < -180) {
                    geoPos.lon += 360;
                }    */

                alt = dem.getElevation(geoPos);
                if(!valid && alt != demNoDataValue)
                    valid = true;
                localDEM[yy][x - x0 + 1] = alt;
            }
        }
        return valid;
    }
}
