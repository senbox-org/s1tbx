package org.esa.beam.dataio.spot;

import com.bc.ceres.binding.PropertySet;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class LogVolDescriptor {
    private final PropertySet propertySet;
    private final String productId;
    private boolean plateCare;

    private static final double PIXEL_CENTER = 0.0;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss", Locale.ENGLISH);

    public LogVolDescriptor(Reader reader) throws IOException {
        this.propertySet = SpotVgtProductReaderPlugIn.readKeyValuePairs(reader);
        this.productId = getValue("PRODUCT_ID");
    }

    public PropertySet getPropertySet() {
        return propertySet;
    }

    public String getValue(String key) {
        return (String) propertySet.getValue(key);
    }

    public String getProductId() {
        return productId;
    }

    public GeoCoding getGeoCoding() {
        String meridian_origin = getValue("MERIDIAN_ORIGIN");
        if (meridian_origin != null && Float.parseFloat(meridian_origin) != 0.0) {
            return null;
        }

        String geodetic_syst_name = getValue("GEODETIC_SYST_NAME");
        if (geodetic_syst_name != null && !geodetic_syst_name.equals("WGS 1984")) {
            return null;
        }

        String map_proj_unit = getValue("MAP_PROJ_UNIT");
        if (map_proj_unit != null && !map_proj_unit.equals("DEGREES")) {
            return null;
        }

        String map_proj_resolution = getValue("MAP_PROJ_RESOLUTION");
        String geo_upper_left_lat = getValue("GEO_UPPER_LEFT_LAT");
        String geo_upper_left_long = getValue("GEO_UPPER_LEFT_LONG");
        String image_upper_left_col = getValue("IMAGE_UPPER_LEFT_COL");
        String image_upper_left_row = getValue("IMAGE_UPPER_LEFT_ROW");
        String image_lower_right_col = getValue("IMAGE_LOWER_RIGHT_COL");
        String image_lower_right_row = getValue("IMAGE_LOWER_RIGHT_ROW");
        if (map_proj_resolution != null
                && geo_upper_left_lat != null
                && geo_upper_left_long != null
                && image_upper_left_col != null
                && image_upper_left_row != null
                && image_lower_right_col != null
                && image_lower_right_row != null) {
            try {
                double upperLeftLat = Double.parseDouble(geo_upper_left_lat);
                double upperLeftLon = Double.parseDouble(geo_upper_left_long);
                double pixelSize = Double.parseDouble(map_proj_resolution);
                int upperLeftCol = Integer.parseInt(image_upper_left_col);
                int upperLeftRow = Integer.parseInt(image_upper_left_row);
                int lowerRightCol = Integer.parseInt(image_lower_right_col);
                int lowerRightRow = Integer.parseInt(image_lower_right_row);

                final Rectangle rect = new Rectangle(upperLeftCol - 1, upperLeftRow - 1,
                                                     lowerRightCol - upperLeftCol + 1,
                                                     lowerRightRow - upperLeftRow + 1);
                AffineTransform transform = new AffineTransform();
                transform.translate(upperLeftLon, upperLeftLat);
                transform.scale(pixelSize, -pixelSize);
                transform.translate(-PIXEL_CENTER, -PIXEL_CENTER);
                return new CrsGeoCoding(DefaultGeographicCRS.WGS84, rect, transform);
            } catch (NumberFormatException e) {
                // ?
            } catch (TransformException e) {
                // ?
            } catch (FactoryException e) {
                // ?
            }
        }

        return null;
    }

    public Date getStartDate() {
        Date date = getDate("SYNTHESIS_FIRST_DATE");
        if (date == null) {
            date = getDate("SEGM_FIRST_DATE", "SEGM_FIRST_TIME");
        }
        return date;
    }

    public Date getEndDate() {
        Date date = getDate("SYNTHESIS_LAST_DATE");
        if (date == null) {
            date = getDate("SEGM_LAST_DATE", "SEGM_LAST_TIME");
        }
        return date;
    }

    private Date getDate(String s) {
        String value = getValue(s);
        if (value != null) {
            try {
                return DATE_FORMAT.parse(value);
            } catch (ParseException e) {
                // ?
            }
        }
        return null;
    }

    private Date getDate(String s1, String s2) {
        String value1 = getValue(s1);
        String value2 = getValue(s2);
        if (value1 != null && value2 != null) {
            try {
                return DATE_FORMAT.parse(value1 + value2);
            } catch (ParseException e) {
                // ?
            }
        }
        return null;
    }
}
