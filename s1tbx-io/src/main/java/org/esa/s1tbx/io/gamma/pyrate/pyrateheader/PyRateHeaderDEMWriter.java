package org.esa.s1tbx.io.gamma.pyrate.pyrateheader;

import org.esa.s1tbx.io.gamma.pyrate.PyRateGammaProductWriter;
import org.esa.snap.core.datamodel.*;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public class PyRateHeaderDEMWriter extends PyRateHeaderWriter {

    final static String sep = ":\t";

    private Product srcProduct ;
    private double cornerLat, cornerLon;
    private String easting, northing, centralMeridian, latitudeOfOrigin, scaleFactor;


    static final String HEADER_X_STEP = "X_STEP";
    static final String HEADER_Y_STEP = "Y_STEP";
    static final String HEADER_LAT = "LAT";
    static final String HEADER_LON = "LON";
    static final String HEADER_EPOCH_DATE = "EPOCH_DATE";
    static final String HEADER_DATUM = "DATUM";

    static final String HEADER_CORNER_LAT = "corner_lat";
    static final String HEADER_CORNER_LON = "corner_lon";

    static final String HEADER_POST_LAT = "post_lat"; // Pixel size in decimal degrees
    static final String HEADER_POST_LON = "post_lon";
    static final String HEADER_ELLIPSOID_NAME = "ellipsoid_name";
    static final String PROJECTION_UTM = "UTM"; //  	Universal Transverse Mercator

    public PyRateHeaderDEMWriter(PyRateGammaProductWriter writer, Product srcProduct, File userOutputFile) {
        super(writer, srcProduct, userOutputFile);
        this.srcProduct = srcProduct;
    }


    public void writeParFile() throws IOException {
        final String oldEOL = System.getProperty("line.separator");
        System.setProperty("line.separator", "\n");
        final FileOutputStream out = new FileOutputStream(outputFile);
        try (final PrintStream p = new PrintStream(out)) {
            p.println(GammaConstants.HEADER_KEY_NAME + sep + srcProduct.getName());

            p.println(GammaConstants.HEADER_KEY_WIDTH + sep + srcProduct.getSceneRasterWidth());
            p.println(GammaConstants.HEADER_KEY_NLINES + sep + srcProduct.getSceneRasterHeight());
            p.println(GammaConstants.HEADER_KEY_DATA_FORMAT + sep + getDataType());

            final String demProjection = getDEMProjection();
            getCornerCoords();

            p.println(this.HEADER_CORNER_LAT + sep + cornerLat + " decimal degrees");
            p.println(this.HEADER_CORNER_LON + sep + cornerLon + " decimal degrees");
            p.println(this.HEADER_POST_LAT + sep + getXPixelDimension() + " decimal degrees");
            p.println(this.HEADER_POST_LON + sep + getYPixelDimension() + " decimal degrees");
            p.println(this.HEADER_ELLIPSOID_NAME + sep + "WGS 84");

/*
            p.println(this.HEADER_KEY_DEM_PROJECTION + sep + demProjection);
            p.println(this.HEADER_KEY_DEM_HGT_OFFSET + sep + "0.0");
            p.println(this.HEADER_KEY_DEM_SCALE + sep + "1.0");
            p.println(this.HEADER_KEY_DEM_POST_NORTH + sep + "90");
            p.println(this.HEADER_KEY_DEM_POST_EAST + sep + "90");

            p.println(this.HEADER_KEY_DATUM_NAME + sep + "WGS84");
            p.println(this.HEADER_KEY_DATUM_SHIFT_DX + sep + "0.0");
            p.println(this.HEADER_KEY_DATUM_SHIFT_DY + sep + "0.0");
            p.println(this.HEADER_KEY_DATUM_SHIFT_DZ + sep + "0.0");
            p.println(this.HEADER_KEY_DATUM_SCALE + sep + "0.0");
            p.println(this.HEADER_KEY_DATUM_ROTATION_ALPHA + sep + "0.0");
            p.println(this.HEADER_KEY_DATUM_ROTATION_BETA + sep + "0.0");
            p.println(this.HEADER_KEY_DATUM_ROTATION_GAMMA + sep + "0.0");

            p.println(this.HEADER_KEY_PROJECTION_NAME + sep + demProjection);
            if(demProjection.equals(this.PROJECTION_UTM)) {
                p.println(this.HEADER_KEY_PROJECTION_ZONE + sep + getUTMZone());
                p.println(this.HEADER_KEY_PROJECTION_FALSE_EASTING + sep + easting);
                p.println(this.HEADER_KEY_PROJECTION_FALSE_NORTHING + sep + northing);
                p.println(this.HEADER_KEY_PROJECTION_CENTER_LON + sep + centralMeridian);
                p.println(this.HEADER_KEY_PROJECTION_CENTER_LAT + sep + latitudeOfOrigin);
                p.println(this.HEADER_KEY_PROJECTION_K0 + sep + scaleFactor);
            }
        */
            p.flush();
        } catch (Exception e) {
            throw new IOException("GammaWriter unable to write par file " + e.getMessage());
        } finally {
            System.setProperty("line.separator", oldEOL);
        }
    }

    private void getCornerCoords() {
        GeoPos geoPos = srcProduct.getSceneGeoCoding().getGeoPos(new PixelPos(0, 0), null);
        cornerLat = geoPos.lat;
        cornerLon = geoPos.lon;
    }

    private String getDEMProjection() {
        CoordinateReferenceSystem crs = srcProduct.getSceneCRS();
        ReferenceIdentifier id = crs.getName();
        if (id.getCode().contains(this.PROJECTION_UTM)) {
            String wkt = crs.toWKT();
            easting = wkt.substring(wkt.indexOf("false_easting") + 15);
            easting = easting.substring(0, easting.indexOf(']')).trim();

            northing = wkt.substring(wkt.indexOf("false_northing") + 16);
            northing = northing.substring(0, northing.indexOf(']')).trim();

            centralMeridian = wkt.substring(wkt.indexOf("central_meridian") + 18);
            centralMeridian = centralMeridian.substring(0, centralMeridian.indexOf(']')).trim();

            latitudeOfOrigin = wkt.substring(wkt.indexOf("latitude_of_origin") + 20);
            latitudeOfOrigin = latitudeOfOrigin.substring(0, latitudeOfOrigin.indexOf(']')).trim();

            scaleFactor = wkt.substring(wkt.indexOf("scale_factor") + 14);
            scaleFactor = scaleFactor.substring(0, scaleFactor.indexOf(']')).trim();

            return this.PROJECTION_UTM;
        }

        return "";
    }

    private String  getXPixelDimension() {
        MetadataElement absRoot = this.srcProduct.getMetadataRoot().getElement("Abstracted_Metadata");

        return "-" + absRoot.getAttributeString("lat_pixel_res");

    }
    private String  getYPixelDimension() {
        MetadataElement absRoot = this.srcProduct.getMetadataRoot().getElement("Abstracted_Metadata");

        return absRoot.getAttributeString("lon_pixel_res");

    }
}
