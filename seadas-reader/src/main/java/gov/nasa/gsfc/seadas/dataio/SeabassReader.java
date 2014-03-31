package gov.nasa.gsfc.seadas.dataio;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.util.FeatureUtils;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.GeometryCoordinateSequenceTransformer;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: dshea
 * Date: 11/14/12
 * Time: 1:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class SeabassReader extends LineNumberReader {
    GeoCoding geoCoding;
    ArrayList<ColumnInfo> columnInfos = null;
    Double missingValue = -999.0;
    String fieldDelimiter = "\\s+";
    int latIndex = -1;
    int lonIndex = -1;

    /**
     * Create a reader for SeaBASS files.
     *
     * @param in reader containing SeaBASS formatted text
     * @param geoCoding used to convert lat,lon to pixel,line
     */
    public SeabassReader(Reader in, GeoCoding geoCoding) {
        super(in);
        this.geoCoding = geoCoding;
    }

    /**
     * Create a collection from the reader data.
     *
     * @return the collection containing all of the data from the file.
     * @throws IOException
     */
    public FeatureCollection<SimpleFeatureType, SimpleFeature> createFeatureCollection() throws IOException {

        SimpleFeatureType featureType = createFeatureType();
        FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = new ListFeatureCollection(featureType);

        String line;
        int pointIndex = 0;
        while ((line = readLine()) != null) {
            SimpleFeature feature = createFeature(featureType, pointIndex, line);
            if (feature != null) {
                featureCollection.add(feature);
            }
            pointIndex++;
        }

        if (featureCollection.isEmpty()) {
            throw new IOException("No track point found or all of them are located outside the scene boundaries.");
        }

        final CoordinateReferenceSystem mapCRS = geoCoding.getMapCRS();
        if (!mapCRS.equals(DefaultGeographicCRS.WGS84)) {
            try {
                transformFeatureCollection(featureCollection, mapCRS);
            } catch (TransformException e) {
                throw new IOException("Cannot transform the ship track onto CRS '" + mapCRS.toWKT() + "'.", e);
            }
        }

        return featureCollection;
    }

    private SimpleFeatureType createFeatureType() throws IOException {
        readHeader();

        SimpleFeatureTypeBuilder ftb = new SimpleFeatureTypeBuilder();
        //ftb.setName("org.esa.beam.TrackPoint");
        ftb.setName("gov.nasa.gsfc.seabass.vectorData");
        /*0*/
        ftb.add("pixelPos", Point.class, geoCoding.getImageCRS());
        /*1*/
        ftb.add("geoPos", Point.class, DefaultGeographicCRS.WGS84);
        for (ColumnInfo info : columnInfos) {
            if (!info.getName().equals("lat") && !info.getName().equals("lon")) {
                ftb.add(info.getName(), info.getDataClass());
            }
        }
        ftb.setDefaultGeometry(geoCoding instanceof CrsGeoCoding ? "geoPos" : "pixelPos");
        // GeoTools Bug: this doesn't work
        // ftb.userData("trackPoints", "true");
        final SimpleFeatureType ft = ftb.buildFeatureType();
        ft.getUserData().put("trackPoints", "true");
        return ft;
    }

    /**
     * read the header form "/begin_header" to "/end_header"
     *
     * @throws IOException
     */
    private void readHeader() throws IOException {
        if (columnInfos != null) {
            return;
        }

        boolean missingValueFound = false;
        String line;
        String[] parts;
        String fieldStr = "";
        while ((line = readLine()) != null) {
            if (line.trim().toLowerCase().equals("/end_header")) {
                break;
            }
            parts = line.split("=", 2);
            if (parts.length == 2) {
                String key = parts[0].trim().toLowerCase();
                String value = parts[1].trim();

                if (key.equals("/delimiter")) {
                    value = value.toLowerCase();
                    if (value.equals("tab")) {
                        fieldDelimiter = "\\t";
                    } else if (value.equals("space")) {
                        fieldDelimiter = "\\s+";
                    } else if (value.equals("comma")) {
                        fieldDelimiter = ",";
                    } else {
                        throw new IOException("invalid /delimiter value = " + value);
                    }
                } else if (key.equals("/missing")) {
                    try {
                        missingValue = Double.valueOf(value);
                        missingValueFound = true;
                    } catch (NumberFormatException e) {
                        throw new IOException("/missing is not a valid number");
                    }
                } else if (key.equals("/fields")) {
                    fieldStr = value;
                }
            } // key=value
        } // while line

        if (line == null) {
            throw new IOException("/end_header not found");
        }

        if (!missingValueFound) {
            throw new IOException("/missing not found in header");
        }

        parts = fieldStr.split(",");
        if (parts.length < 1) {
            throw new IOException("/fields needs comma separated field names");
        }

        int index = 0;
        columnInfos = new ArrayList<ColumnInfo>();
        for (String name : parts) {
            name = name.trim();
            ColumnInfo info = new ColumnInfo(name.trim());
            columnInfos.add(info);
            if (name.equals("lat")) {
                latIndex = index;
            } else if (name.equals("lon")) {
                lonIndex = index;
            }
            index++;
        }

        if (latIndex == -1) {
            throw new IOException("lat needs to be in /fields");
        }
        if (lonIndex == -1) {
            throw new IOException("lon needs to be in /fields");
        }
    }

    /**
     * create a feature from one line of text
     *
     * @param type feature type describing a row of data
     * @param pointIndex point number to assign
     * @param line data to be read
     * @return the feature holding the data from the string
     * @throws IOException
     */
    private SimpleFeature createFeature(SimpleFeatureType type, int pointIndex, String line) throws IOException {
        String[] record = line.split(fieldDelimiter);

        if (record.length != columnInfos.size()) {
            throw new IOException("Illegal number of values\n Expecting " +
                    Integer.toString(columnInfos.size()) +
                    ", but found " + Integer.toString(record.length) +
                    " on line " + getLineNumber());
        }

        float lat;
        float lon;
        try {
            lat = Float.parseFloat(record[latIndex]);
        } catch (Exception e) {
            throw new IOException("lat is not a valid float on line " + getLineNumber());
        }

        try {
            lon = Float.parseFloat(record[lonIndex]);
        } catch (Exception e) {
            throw new IOException("lon is not a valid float on line " + getLineNumber());
        }

        PixelPos pixelPos = geoCoding.getPixelPos(new GeoPos(lat, lon), null);
        if (!pixelPos.isValid()) {
            return null;
        }
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(type);
        GeometryFactory gf = new GeometryFactory();
        /*0*/
        fb.add(gf.createPoint(new Coordinate(pixelPos.x, pixelPos.y)));
        /*1*/
        fb.add(gf.createPoint(new Coordinate(lon, lat)));

        for (int i = 0; i < record.length; i++) {
            if (i != latIndex && i != lonIndex) {
                ColumnInfo info = columnInfos.get(i);
                fb.add(info.convertData(record[i]));
            }
        }
        return fb.buildFeature(String.format("ID%08d", pointIndex));
    }

    private static void transformFeatureCollection(FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection, CoordinateReferenceSystem targetCRS) throws TransformException {
        final GeometryCoordinateSequenceTransformer transform = FeatureUtils.getTransform(DefaultGeographicCRS.WGS84, targetCRS);
        final FeatureIterator<SimpleFeature> features = featureCollection.features();
        final GeometryFactory geometryFactory = new GeometryFactory();
        while (features.hasNext()) {
            final SimpleFeature simpleFeature = features.next();
            final Point sourcePoint = (Point) simpleFeature.getDefaultGeometry();
            final Point targetPoint = transform.transformPoint(sourcePoint, geometryFactory);
            simpleFeature.setDefaultGeometry(targetPoint);
        }
    }

    /**
     * internal class used to hold the information about a column of data from the file.
     */
    private class ColumnInfo {
        private String name;

        public ColumnInfo(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public Class getDataClass() {
            String name1 = name.toLowerCase();
            if (name1.equals("chors_id")) {
                return String.class;
            } else if (name1.equals("hpl_id")) {
                return String.class;
            } else if (name1.equals("hplc_gsfc_id")) {
                return String.class;
            } else if (name1.equals("quality")) {
                return String.class;
            } else if (name1.equals("SN")) {
                return String.class;
            } else if (name1.equals("station")) {
                return String.class;
            } else if (name1.equals("time")) {
                // todo: figure what to do about the time field
                // return Date.class;
                return String.class;
            }
            return Double.class;
        }

        public Object convertData(String data) {
            Class c = getDataClass();
            if (c == Double.class) {
                return new Double(data);
            } else {
                return data;
            }
        }


    }


}
