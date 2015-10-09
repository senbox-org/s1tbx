package org.esa.snap.core.dataio.placemark;

import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.GcpDescriptor;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PinDescriptor;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.datamodel.PlacemarkDescriptor;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.io.CsvReader;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static org.junit.Assert.*;

/**
 * User: Marco
 * Date: 03.09.2010
 */
public class PlacemarkIOTest {

    private static final int WRITER_INITIAL_SIZE = 200;
    private static final int NUM_PLACEMARKS = 5;
    private static final Rectangle DATA_BOUNDS = new Rectangle(100, 100);
    private static CrsGeoCoding GEO_CODING;

    @BeforeClass
    public static void beforeClass() throws TransformException, FactoryException {
        AffineTransform i2mTransform = new AffineTransform();
        final int northing = 60;
        final int easting = -5;
        i2mTransform.translate(easting, northing);
        final double scaleX = 0.3;
        final double scaleY = 0.3;
        i2mTransform.scale(scaleX, -scaleY);
        GEO_CODING = new CrsGeoCoding(DefaultGeographicCRS.WGS84, DATA_BOUNDS, i2mTransform);
    }

    @Test
    public void testReadWritePinXmlFile() throws Exception {
        testReadWritePlacemarkXmlFile(PinDescriptor.getInstance());
    }

    @Test
    public void testReadWriteGcpXmlFile() throws Exception {
        testReadWritePlacemarkXmlFile(GcpDescriptor.getInstance());
    }

    @Test
    public void testReadMinimalPlacemarkTextFile() throws Exception {
        testReadMinimalPlacemarkTextFile(GcpDescriptor.getInstance());
        testReadMinimalPlacemarkTextFile(PinDescriptor.getInstance());
    }


    @Test
    public void testFindColumnIndex() throws Exception {
        assertEquals(1, PlacemarkIO.findColumnIndex(new String[]{"col1", "cOL2", "CoL3"}, "cOL2"));
        assertEquals(1, PlacemarkIO.findColumnIndex(new String[]{"col1", "cOL2", "CoL3"}, "col2"));
        assertEquals(-1, PlacemarkIO.findColumnIndex(new String[]{"col1", "cOL2", "CoL3"}, "abc"));

        assertEquals(0, PlacemarkIO.findColumnIndex(new String[]{"col1", "cOL2", "CoL3"}, "abc", "xyz", "COL1"));
        assertEquals(1, PlacemarkIO.findColumnIndex(new String[]{"col1", "cOL2", "CoL3"}, "COL2", "col2"));

    }

    @Test
    public void testReadWritePlacemarksTextFileWithAdditionalData() throws Exception {
        StringWriter writer = new StringWriter(WRITER_INITIAL_SIZE);
        PinDescriptor pinDescriptor = PinDescriptor.getInstance();
        List<Placemark> expectedPlacemarks = createPlacemarks(pinDescriptor, GEO_CODING, DATA_BOUNDS);
        String[] stdColumnName = {"X", "Y", "Lon", "Lat", "Label"};
        String[] addColumnName = {"A", "B", "C", "D"};
        List<Object[]> valuesList = new ArrayList<Object[]>();
        for (Placemark expectedPlacemark : expectedPlacemarks) {
            Object[] values = new Object[stdColumnName.length + addColumnName.length];
            values[0] = expectedPlacemark.getPixelPos().x;
            values[1] = expectedPlacemark.getPixelPos().y;
            values[2] = expectedPlacemark.getGeoPos().lon;
            values[3] = expectedPlacemark.getGeoPos().lat;
            values[4] = expectedPlacemark.getLabel();
            for (int j = stdColumnName.length; j < values.length; j++) {
                values[j] = Math.random();
            }
            valuesList.add(values);
        }
        PlacemarkIO.writePlacemarksWithAdditionalData(writer, pinDescriptor.getRoleLabel(), "ProductName",
                                                      expectedPlacemarks, valuesList, stdColumnName, addColumnName);
        String output = writer.toString();

        List<Placemark> actualPlacemarks = PlacemarkIO.readPlacemarks(new StringReader(output), GEO_CODING,
                                                                      pinDescriptor);

        testReadStandardResult(expectedPlacemarks, actualPlacemarks, pinDescriptor);
        CsvReader csvReader = new CsvReader(new StringReader(output), new char[]{'\t'}, true, "#");
        String[] header = csvReader.readRecord(); // header line
        assertEquals("Name", header[0]);
        String[] actualStdColNames = Arrays.copyOfRange(header, 1, stdColumnName.length + 1);
        assertArrayEquals(stdColumnName, actualStdColNames);
        assertEquals("Desc", header[stdColumnName.length + 1]);
        String[] actualAddColNames = Arrays.copyOfRange(header, stdColumnName.length + 2, header.length);
        assertArrayEquals(addColumnName, actualAddColNames);
        for (Object[] values : valuesList) {
            String[] record = csvReader.readRecord();
            String[] actualAddValues = Arrays.copyOfRange(record, stdColumnName.length + 2, header.length);
            Object[] expectedValues = Arrays.copyOfRange(values, stdColumnName.length, values.length);
            for (int i = 0; i < expectedValues.length; i++) {
                assertEquals(expectedValues[i].toString(), actualAddValues[i]);
            }
        }
        assertNull(csvReader.readRecord()); // assert all records read
    }

    @Test
    public void testReadPlacemarkTextFileWithDateTime() throws Exception {
        StringWriter writer = new StringWriter(WRITER_INITIAL_SIZE);
        PinDescriptor pinDescriptor = PinDescriptor.getInstance();
        List<Placemark> expectedPlacemarks = createPlacemarks(pinDescriptor, GEO_CODING, DATA_BOUNDS);
        String[] stdColumnName = {"X", "Y", "Lon", "Lat", "Label"};
        String[] addColumnName = {"DateTime"};
        List<Object[]> valuesList = new ArrayList<>();
        Calendar utc = ProductData.UTC.createCalendar();
        for (Placemark expectedPlacemark : expectedPlacemarks) {
            Object[] values = new Object[stdColumnName.length + addColumnName.length];
            values[0] = expectedPlacemark.getPixelPos().x;
            values[1] = expectedPlacemark.getPixelPos().y;
            values[2] = expectedPlacemark.getGeoPos().lon;
            values[3] = expectedPlacemark.getGeoPos().lat;
            values[4] = expectedPlacemark.getLabel();
            utc.setTimeInMillis(new Date().getTime() + valuesList.size() * (60 * 60 * 1000));
            utc.set(Calendar.MILLISECOND, 0); // set millis to zero because it is not written; just second accuracy
            values[5] = utc.getTime();
            valuesList.add(values);
        }
        PlacemarkIO.writePlacemarksWithAdditionalData(writer, pinDescriptor.getRoleLabel(), "ProductName",
                                                      expectedPlacemarks, valuesList, stdColumnName, addColumnName);
        String output = writer.toString();

        List<Placemark> actualPlacemarks = PlacemarkIO.readPlacemarks(new StringReader(output), GEO_CODING, pinDescriptor);

        for (int i = 0; i < actualPlacemarks.size(); i++) {
            final Placemark actualPlacemark = actualPlacemarks.get(i);
            final Date expectedDateTimeAttribute = (Date) valuesList.get(i)[5];
            final Date actualDateTimeAttribute = (Date) actualPlacemark.getFeature().getAttribute("dateTime");
            assertEquals(expectedDateTimeAttribute, actualDateTimeAttribute);
        }
    }

    @Test
    public void testReadPlacemarkTextFileWithDateTimeFromConstantInput() throws Exception {
        final StringWriter stringWriter = new StringWriter(WRITER_INITIAL_SIZE);
        PrintWriter writer = new PrintWriter(stringWriter);
        writer.printf("Name	Lat	Lon	DateTime%n");
        writer.printf("One	59.885	10.664	2005-04-18T17:53:58%n");
        writer.printf("Two	59.883	10.657	2006-08-06T12:54:58%n");
        writer.printf("Three	59.88	10.65	2007-09-01T07:55:58%n");

        List<Placemark> actualPlacemarks = PlacemarkIO.readPlacemarks(new StringReader(stringWriter.toString()),
                                                                      GEO_CODING,
                                                                      PinDescriptor.getInstance());
        assertEquals(3, actualPlacemarks.size());
        final Placemark mark1 = actualPlacemarks.get(0);
        assertEquals("One", mark1.getLabel());
        assertDateTime(mark1, 2005, 3, 18, 17, 53, 58);
        assertEquals(59.885, mark1.getGeoPos().getLat(), 1.0e-3);
        assertEquals(10.664, mark1.getGeoPos().getLon(), 1.0e-3);
        final Placemark mark2 = actualPlacemarks.get(1);
        assertEquals("Two", mark2.getLabel());
        assertDateTime(mark2, 2006, 7, 6, 12, 54, 58);
        assertEquals(59.883, mark2.getGeoPos().getLat(), 1.0e-3);
        assertEquals(10.657, mark2.getGeoPos().getLon(), 1.0e-3);
        final Placemark mark3 = actualPlacemarks.get(2);
        assertEquals("Three", mark3.getLabel());
        assertDateTime(mark3, 2007, 8, 1, 7, 55, 58);
        assertEquals(59.88, mark3.getGeoPos().getLat(), 1.0e-3);
        assertEquals(10.65, mark3.getGeoPos().getLon(), 1.0e-3);
    }

    private void assertDateTime(Placemark mark, int year, int month, int day, int hourOfDay, int minute, int second) {
        final Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTime((Date) mark.getFeature().getAttribute("dateTime"));
        assertEquals(year, cal.get(Calendar.YEAR));
        assertEquals(month, cal.get(Calendar.MONTH));
        assertEquals(day, cal.get(Calendar.DATE));
        assertEquals(hourOfDay, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(minute, cal.get(Calendar.MINUTE));
        assertEquals(second, cal.get(Calendar.SECOND));
    }

    private void testReadWritePlacemarkXmlFile(PlacemarkDescriptor descriptorInstance) throws IOException {
        StringWriter writer = new StringWriter(WRITER_INITIAL_SIZE);
        List<Placemark> expectedPlacemarks = createPlacemarks(descriptorInstance, GEO_CODING, DATA_BOUNDS);
        PlacemarkIO.writePlacemarksFile(writer, expectedPlacemarks);
        String output = writer.toString();

        List<Placemark> actualPlacemarks = PlacemarkIO.readPlacemarks(new StringReader(output), GEO_CODING,
                                                                      descriptorInstance);
        testReadXmlResult(expectedPlacemarks, actualPlacemarks, descriptorInstance);
    }

    private void testReadXmlResult(List<Placemark> expectedPlacemarks, List<Placemark> actualPlacemarks,
                                   PlacemarkDescriptor descriptorInstance) {
        assertEquals(expectedPlacemarks.size(), actualPlacemarks.size());
        testReadStandardResult(expectedPlacemarks, actualPlacemarks, descriptorInstance);
        for (int i = 0; i < actualPlacemarks.size(); i++) {
            assertEquals(expectedPlacemarks.get(i).getStyleCss(), actualPlacemarks.get(i).getStyleCss());
        }
    }

    private void testReadStandardResult(List<Placemark> expectedPlacemarks, List<Placemark> actualPlacemarks,
                                        PlacemarkDescriptor descriptorInstance) {
        for (int i = 0; i < actualPlacemarks.size(); i++) {
            Placemark actualPlacemark = actualPlacemarks.get(i);
            Placemark expectedPlacemark = expectedPlacemarks.get(i);
            assertNotSame(expectedPlacemark, actualPlacemark);
            assertEquals(expectedPlacemark.getName(), actualPlacemark.getName());
            assertEquals(expectedPlacemark.getLabel(), actualPlacemark.getLabel());
            assertEquals(expectedPlacemark.getPixelPos().getX(), actualPlacemark.getPixelPos().getX(), 1.0e-8);
            assertEquals(expectedPlacemark.getPixelPos().getY(), actualPlacemark.getPixelPos().getY(), 1.0e-8);
            assertEquals(expectedPlacemark.getGeoPos().getLat(), actualPlacemark.getGeoPos().getLat(), 1.0e-8);
            assertEquals(expectedPlacemark.getGeoPos().getLon(), actualPlacemark.getGeoPos().getLon(), 1.0e-8);
            assertEquals(expectedPlacemark.getDescription(), actualPlacemark.getDescription());
            PlacemarkDescriptor descriptor = expectedPlacemark.getDescriptor();
            assertEquals(descriptor.getRoleLabel(), descriptorInstance.getRoleLabel());
        }
    }

    private List<Placemark> createPlacemarks(PlacemarkDescriptor descriptor, CrsGeoCoding geoCoding,
                                             Rectangle data_bounds) {
        ArrayList<Placemark> placemarkList = new ArrayList<Placemark>();
        for (int i = 0; i < NUM_PLACEMARKS; i++) {
            PixelPos pixelPos = new PixelPos(Math.random() * data_bounds.width,
                                             Math.random() * data_bounds.height);
            GeoPos geoPos = geoCoding.getGeoPos(pixelPos, null);
            Placemark placemark = Placemark.createPointPlacemark(descriptor, "name_" + i, "label_" + i, "description_" + i,
                                                                 pixelPos, geoPos, geoCoding);
            placemark.setStyleCss("fill:#FFFFFF;stroke:#000000");
            placemarkList.add(placemark);
        }
        return placemarkList;
    }

    private void testReadMinimalPlacemarkTextFile(PlacemarkDescriptor descriptor) throws IOException {
        String output = "Name\tLat\tLon\n";
        output += "name_1\t10.2\t12.4\n";
        output += "name_2\t40.0\t-2.9\n";


        List<Placemark> actualPlacemarks = PlacemarkIO.readPlacemarks(new StringReader(output), GEO_CODING,
                                                                      descriptor);
        assertEquals(2, actualPlacemarks.size());
        assertEquals(actualPlacemarks.get(0).getName(), "name_1");
        assertEquals(actualPlacemarks.get(0).getGeoPos().lat, 10.2, 1.0e-4);
        assertEquals(actualPlacemarks.get(0).getGeoPos().lon, 12.4, 1.0e-4);
        assertEquals(actualPlacemarks.get(1).getName(), "name_2");
        assertEquals(actualPlacemarks.get(1).getGeoPos().lat, 40.0, 1.0e-4);
        assertEquals(actualPlacemarks.get(1).getGeoPos().lon, -2.9, 1.0e-4);
    }
}
