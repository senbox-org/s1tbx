/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.core.dataio.placemark;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.dimap.DimapProductConstants;
import org.esa.snap.core.dataio.dimap.DimapProductHelpers;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.datamodel.PlacemarkDescriptor;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.XmlWriter;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.core.util.kmz.ExtendedData;
import org.esa.snap.core.util.kmz.KmlDocument;
import org.esa.snap.core.util.kmz.KmlPlacemark;
import org.esa.snap.core.util.kmz.KmzExporter;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.DOMBuilder;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.Color;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.Writer;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.zip.ZipOutputStream;

/**
 * Utility class, that reads and writes placemarks from and to various plain text formats.
 *
 * @author Nobody wants to be responsible for this mess.
 * @since BEAM 1.0
 */
public class PlacemarkIO {

    public static final String FILE_EXTENSION_FLAT_OLD = ".pnf";
    public static final String FILE_EXTENSION_XML_OLD = ".pnx";
    public static final String FILE_EXTENSION_FLAT_TEXT = ".txt";
    public static final String FILE_EXTENSION_PLACEMARK = ".placemark";
    public static final String FILE_EXTENSION_KMZ = ".kmz";

    public static final String TAG_FILL_COLOR = "FillColor";
    public static final String TAG_OUTLINE_COLOR = "OutlineColor";

    private static final int INDEX_FOR_NAME = 0;
    private static final int INDEX_FOR_LON = 1;
    private static final int INDEX_FOR_LAT = 2;
    private static final int INDEX_FOR_DESC = 3;
    private static final int INDEX_FOR_LABEL = 4;
    private static final int INDEX_FOR_DATETIME = 5;

    private static final String LABEL_COL_NAME = "Label";
    // same columns allowed as in VectorDataNodeReader
    private static final String[] LON_COL_NAMES = {"Lon", "long", "longitude", "lon_IS"};
    private static final String[] LAT_COL_NAMES = {"Lat", "latitude", "lat_IS"};
    private static final String NAME_COL_NAME = "Name";
    private static final String[] DESC_COL_NAMES = {"Desc", "Description"};
    private static final String DATETIME_COL_NAME = "DateTime";

    private static final String ISO8601_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
    private static final DateFormat dateFormat = ProductData.UTC.createDateFormat(ISO8601_PATTERN);

    private PlacemarkIO() {

    }

    public static List<Placemark> readPlacemarks(Reader reader, GeoCoding geoCoding,
                                                 PlacemarkDescriptor placemarkDescriptor) throws IOException {
        final char[] magicBytes = new char[5];
        try (PushbackReader inputReader = new PushbackReader(reader, magicBytes.length)) {
            inputReader.read(magicBytes);
            inputReader.unread(magicBytes);
            if (XmlWriter.XML_HEADER_LINE.startsWith(new String(magicBytes))) {
                return readPlacemarksFromXMLFile(inputReader, geoCoding, placemarkDescriptor);
            } else {
                return readPlacemarksFromFlatFile(inputReader, geoCoding, placemarkDescriptor);
            }
        }
    }

    private static List<Placemark> readPlacemarksFromFlatFile(Reader reader, GeoCoding geoCoding,
                                                              PlacemarkDescriptor placemarkDescriptor) throws
            IOException {
        ArrayList<Placemark> placemarks = new ArrayList<>();
        try (BufferedReader lineReader = new BufferedReader(reader)) {
            int row = 0;
            List<Integer> stdColIndexes = null;
            int biggestIndex = 0;
            while (true) {
                String line = lineReader.readLine();
                if (line == null) {
                    break;
                }
                line = line.trim(); // cut \n and \r from the end of the line
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] strings = StringUtils.toStringArray(line, "\t");
                if (stdColIndexes == null) {
                    int nameIndex = findColumnIndex(strings, NAME_COL_NAME);
                    int lonIndex = findColumnIndex(strings, LON_COL_NAMES);
                    int latIndex = findColumnIndex(strings, LAT_COL_NAMES);
                    int descIndex = findColumnIndex(strings, DESC_COL_NAMES);
                    int labelIndex = findColumnIndex(strings, LABEL_COL_NAME);
                    int dateTimeIndex = findColumnIndex(strings, DATETIME_COL_NAME);
                    if (nameIndex == -1 || lonIndex == -1 || latIndex == -1) {
                        throw new IOException("Invalid placemark file format:\n" +
                                              "at least the columns 'Name', 'Lon' and 'Lat' must be given.");
                    }
                    biggestIndex = biggestIndex > nameIndex ? biggestIndex : nameIndex;
                    biggestIndex = biggestIndex > lonIndex ? biggestIndex : lonIndex;
                    biggestIndex = biggestIndex > latIndex ? biggestIndex : latIndex;
                    stdColIndexes = new ArrayList<>(6);
                    stdColIndexes.add(INDEX_FOR_NAME, nameIndex);
                    stdColIndexes.add(INDEX_FOR_LON, lonIndex);
                    stdColIndexes.add(INDEX_FOR_LAT, latIndex);
                    stdColIndexes.add(INDEX_FOR_DESC, descIndex);
                    stdColIndexes.add(INDEX_FOR_LABEL, labelIndex);
                    stdColIndexes.add(INDEX_FOR_DATETIME, dateTimeIndex);

                } else {
                    row++;
                    if (strings.length > biggestIndex) {
                        String name = strings[stdColIndexes.get(INDEX_FOR_NAME)];
                        double lon;
                        try {
                            lon = Double.parseDouble(strings[stdColIndexes.get(INDEX_FOR_LON)]);
                        } catch (NumberFormatException ignored) {
                            throw new IOException("Invalid placemark file format:\n" +
                                                  "data row " + row + ": value for 'Lon' is invalid");      /*I18N*/
                        }
                        double lat;
                        try {
                            lat = Double.parseDouble(strings[stdColIndexes.get(INDEX_FOR_LAT)]);
                        } catch (NumberFormatException ignored) {
                            throw new IOException("Invalid placemark file format:\n" +
                                                  "data row " + row + ": value for 'Lat' is invalid");      /*I18N*/
                        }
                        String label = name;
                        final Integer labelIndex = stdColIndexes.get(INDEX_FOR_LABEL);
                        if (labelIndex >= 0 && strings.length > labelIndex) {
                            label = strings[labelIndex];
                        }

                        Placemark placemark = Placemark.createPointPlacemark(placemarkDescriptor, name, label, "", null, new GeoPos(lat, lon),
                                                                             geoCoding);
                        String desc = null;
                        final Integer descIndex = stdColIndexes.get(INDEX_FOR_DESC);
                        if (descIndex >= 0 && strings.length > descIndex) {
                            desc = strings[descIndex];
                        }
                        if (desc != null) {
                            placemark.setDescription(desc);
                        }

                        final Integer dateTimeIndex = stdColIndexes.get(INDEX_FOR_DATETIME);
                        if (dateTimeIndex >= 0 && strings.length > dateTimeIndex) {
                            try {
                                final Date date = dateFormat.parse(strings[dateTimeIndex]);
                                placemark.getFeature().setAttribute(Placemark.PROPERTY_NAME_DATETIME, date);
                            } catch (ParseException ignored) {
                            }
                        }
                        placemarks.add(placemark);
                    } else {
                        throw new IOException("Invalid placemark file format:\n" +
                                              "data row " + row + ": values for 'Name', 'Lon' and 'Lat' must be given.");   /*I18N*/
                    }
                }
            }
        }

        return placemarks;
    }

    static int findColumnIndex(String[] strings, String... colNames) {
        int index = -1;
        for (String colName : colNames) {
            index = StringUtils.indexOfIgnoreCase(strings, colName);
            if(index != -1) {
                break;
            }
        }
        return index;
    }

    private static List<Placemark> readPlacemarksFromXMLFile(Reader reader, GeoCoding geoCoding,
                                                             PlacemarkDescriptor placemarkDescriptor) throws
            IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document w3cDocument = builder.parse(new InputSource(reader));
            Document document = new DOMBuilder().build(w3cDocument);
            final Element rootElement = document.getRootElement();
            List children = rootElement.getChildren(DimapProductConstants.TAG_PLACEMARK);
            if (children.isEmpty()) {
                // support for old pin XML format (.pnx)
                children = rootElement.getChildren(DimapProductConstants.TAG_PIN);
            }
            final ArrayList<Placemark> placemarks = new ArrayList<>(children.size());
            for (Object child : children) {
                final Element element = (Element) child;
                try {
                    final Placemark placemark = createPlacemark(element, placemarkDescriptor, geoCoding);
                    placemarks.add(placemark);
                } catch (IllegalArgumentException ignored) {
                }
            }
            return placemarks;
        } catch (FactoryConfigurationError | ParserConfigurationException | SAXException | IOException e) {
            throw new IOException(e.toString(), e);
        }
    }

    public static void writePlacemarksWithAdditionalData(Writer writer,
                                                         String roleLabel, String productName,
                                                         List<Placemark> placemarkList, List<Object[]> valueList,
                                                         String[] standardColumnNames, String[] additionalColumnNames) {
        int columnCountMin = standardColumnNames.length;
        int columnCount = columnCountMin + additionalColumnNames.length;
        try (PrintWriter pw = new PrintWriter(writer)) {
            // Write file header
            pw.println("# SNAP " + roleLabel + " export table");
            pw.println("#");
            pw.println("# Product:\t" + productName);
            pw.println("# Created on:\t" + new Date());
            pw.println();

            Product product = null;
            if (!placemarkList.isEmpty()) {
                Placemark placemark = placemarkList.get(0);
                product = placemark.getProduct();
            }
            if (product != null && additionalColumnNames.length != 0) {
                pw.println(getWavelengthLine(product, standardColumnNames, additionalColumnNames));
            }

            pw.println(getHeaderLine(standardColumnNames, additionalColumnNames));

            for (int i = 0; i < placemarkList.size(); i++) {
                Placemark placemark = placemarkList.get(i);
                Object[] values = valueList.get(i);
                pw.println(getDataLine(placemark, values, columnCountMin, columnCount));
            }
        }
    }

    private static String getDataLine(Placemark placemark, Object[] values, int columnCountMin, int columnCount) {
        StringBuilder sb = new StringBuilder();
        sb.append(placemark.getName());
        for (int col = 0; col < columnCountMin; col++) {
            formatValue(values[col], sb);
        }
        sb.append("\t");
        sb.append(placemark.getDescription());
        for (int col = columnCountMin; col < columnCount; col++) {
            formatValue(values[col], sb);
        }
        return sb.toString();
    }

    private static void formatValue(Object value, StringBuilder sb) {
        String stringValue;
        if (value instanceof Date) {
            stringValue = dateFormat.format(value);
        } else {
            stringValue = value.toString();
        }
        sb.append("\t");
        sb.append(stringValue);
    }

    private static String getHeaderLine(String[] standardColumnNames, String[] additionalColumnNames) {
        StringBuilder sb = new StringBuilder();
        sb.append(NAME_COL_NAME);
        for (String name : standardColumnNames) {
            sb.append("\t");
            sb.append(name);
        }
        sb.append("\t");
        sb.append(DESC_COL_NAMES[0]);
        for (String additionalColumnName : additionalColumnNames) {
            sb.append("\t");
            sb.append(additionalColumnName);
        }
        return sb.toString();
    }

    private static String getWavelengthLine(Product product, String[] standardColumnNames, String[] additionalColumnNames) {
        StringBuilder sb = new StringBuilder();

        sb.append("# Wavelength:"); // --> Name
        for (String ignored : standardColumnNames) {
            sb.append("\t");
        }
        sb.append("\t"); // --> Desc
        for (String additionalColumnName : additionalColumnNames) {
            Band band = product.getBand(additionalColumnName);
            float spectralWavelength = 0.0f;
            if (band != null) {
                spectralWavelength = band.getSpectralWavelength();
            }
            sb.append("\t");
            sb.append(spectralWavelength);
        }
        return sb.toString();
    }

    public static void writePlacemarksFile(Writer writer, List<Placemark> placemarks) throws IOException {

        XmlWriter xmlWriter = new XmlWriter(writer, true);
        final String[] tags = XmlWriter.createTags(0, "Placemarks");
        xmlWriter.println(tags[0]);
        for (Placemark placemark : placemarks) {
            if (placemark != null) {
                writeXML(placemark, xmlWriter, 1);
            }
        }
        xmlWriter.println(tags[1]);
        xmlWriter.close();
    }

    public static void writePlacemarkKmzFile(OutputStream os, List<PlacemarkData> placemarks, ProgressMonitor pm) throws IOException {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(os)) {
            KmlDocument kmlDocument = new KmlDocument("SNAP Placemarks", "");
            for (PlacemarkData placemarkData : placemarks) {
                Placemark placemark = placemarkData.getPlacemark();
                GeoPos geoPos = placemark.getGeoPos();
                String label = StringUtils.isNullOrEmpty(placemark.getLabel()) ? placemark.getName() : placemark.getLabel();
                if (geoPos != null) {
                    final Point2D.Double position = new Point2D.Double(geoPos.getLon(), geoPos.getLat());
                    KmlPlacemark kmlPlacemark = new KmlPlacemark(label, placemark.getDescription(), position);
                    Map<String, Object> extraData = placemarkData.getExtraData();
                    if (extraData != null) {
                        kmlPlacemark.setExtendedData(ExtendedData.create(extraData));
                    }
                    kmlDocument.addChild(kmlPlacemark);
                } else {
                    SystemUtils.LOG.log(Level.WARNING, String.format("Placemark %s has no geo-position and has been skipped during writing",
                                                                     label));
                }
            }

            KmzExporter exporter = new KmzExporter();
            exporter.export(kmlDocument, zipOutputStream, pm);
        }
    }

    public static SnapFileFilter createTextFileFilter() {
        return new SnapFileFilter("PLACEMARK_TEXT_FILE",
                                  new String[]{FILE_EXTENSION_FLAT_TEXT, FILE_EXTENSION_FLAT_OLD},
                                  "Placemark files - flat text format");
    }

    public static SnapFileFilter createPlacemarkFileFilter() {
        return new SnapFileFilter("PLACEMARK_XML_FILE",
                                  new String[]{FILE_EXTENSION_PLACEMARK, FILE_EXTENSION_XML_OLD},
                                  "Placemark files - XML format");
    }

    public static SnapFileFilter createKmzFileFilter() {
        return new SnapFileFilter("GOOGLE_EARTH_KMZ_FILE",
                                  new String[]{FILE_EXTENSION_KMZ},
                                  "KMZ file containing placemarks");
    }

    /**
     * Creates a new placemark from an XML element and a given symbol.
     *
     * @param element    the element.
     * @param descriptor the descriptor of the placemark.
     * @param geoCoding  the geoCoding to used by the placemark. Can be {@code null}.
     * @return the placemark created.
     * @throws NullPointerException     if element is null
     * @throws IllegalArgumentException if element is invalid
     */
    public static Placemark createPlacemark(Element element, PlacemarkDescriptor descriptor, GeoCoding geoCoding) {
        if (!DimapProductConstants.TAG_PLACEMARK.equals(element.getName()) &&
                !DimapProductConstants.TAG_PIN.equals(element.getName())) {
            throw new IllegalArgumentException(MessageFormat.format("Element ''{0}'' or ''{1}'' expected.",
                                                                    DimapProductConstants.TAG_PLACEMARK,
                                                                    DimapProductConstants.TAG_PIN));
        }
        final String name1 = element.getAttributeValue(DimapProductConstants.ATTRIB_NAME);
        if (name1 == null) {
            throw new IllegalArgumentException(MessageFormat.format("Missing attribute ''{0}''.",
                                                                    DimapProductConstants.ATTRIB_NAME));
        }
        if (!ProductNode.isValidNodeName(name1)) {
            throw new IllegalArgumentException(MessageFormat.format("Invalid placemark name ''{0}''.", name1));
        }

        String label = element.getChildTextTrim(DimapProductConstants.TAG_PLACEMARK_LABEL);
        if (label == null) {
            label = name1;
        }
        final String description1 = element.getChildTextTrim(DimapProductConstants.TAG_PLACEMARK_DESCRIPTION);
        final String latText = element.getChildTextTrim(DimapProductConstants.TAG_PLACEMARK_LATITUDE);
        final String lonText = element.getChildTextTrim(DimapProductConstants.TAG_PLACEMARK_LONGITUDE);
        final String posXText = element.getChildTextTrim(DimapProductConstants.TAG_PLACEMARK_PIXEL_X);
        final String posYText = element.getChildTextTrim(DimapProductConstants.TAG_PLACEMARK_PIXEL_Y);

        GeoPos geoPos = null;
        if (latText != null && lonText != null) {
            try {
                double lat = Double.parseDouble(latText);
                double lon = Double.parseDouble(lonText);
                geoPos = new GeoPos(lat, lon);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid geo-position.", e);
            }
        }
        PixelPos pixelPos = null;
        if (posXText != null && posYText != null) {
            try {
                double pixelX = Double.parseDouble(posXText);
                double pixelY = Double.parseDouble(posYText);
                pixelPos = new PixelPos(pixelX, pixelY);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid pixel-position.", e);
            }
        }
        if (geoPos == null && pixelPos == null) {
            throw new IllegalArgumentException("Neither geo-position nor pixel-position given.");
        }

        final Placemark placemark = Placemark.createPointPlacemark(descriptor, name1, label, description1, pixelPos, geoPos, geoCoding);
        String styleCss = element.getChildTextTrim(DimapProductConstants.TAG_PLACEMARK_STYLE_CSS);
        if (styleCss == null) {
            final String placemarkStyleCss = placemark.getStyleCss();
            styleCss = StringUtils.isNullOrEmpty(placemarkStyleCss) ? getStyleCssFromOldFormat(element) :
                    placemarkStyleCss + ";" + getStyleCssFromOldFormat(element);
        }
        placemark.setStyleCss(styleCss);

        return placemark;
    }

    private static String getStyleCssFromOldFormat(Element element) {
        StringBuilder styleCss = new StringBuilder();
        buildStyleCss(getColorProperty(element, TAG_FILL_COLOR, "fill"), styleCss);
        buildStyleCss(getColorProperty(element, TAG_OUTLINE_COLOR, "stroke"), styleCss);
        return styleCss.toString();
    }

    private static void buildStyleCss(Property strokeProperty, StringBuilder styleCss) {
        if (strokeProperty.getValue() != null) {
            if (styleCss.length() > 0) {
                styleCss.append(";");
            }
            styleCss.append(strokeProperty.getName()).append(":").append(strokeProperty.getValueAsText());
        }
    }

    private static Property getColorProperty(Element element, String elementName, String propertyName) {
        final Property fillProperty = Property.create(propertyName, Color.class);
        final Color fillColor = createColor(element.getChild(elementName));
        if (fillColor != null) {
            try {
                fillProperty.setValue(fillColor);
            } catch (ValidationException e) {
                SystemUtils.LOG.warning(e.getMessage());
            }
        }
        return fillProperty;
    }

    private static Color createColor(Element elem) {
        if (elem != null) {
            Element colorElem = elem.getChild(DimapProductConstants.TAG_COLOR);
            if (colorElem != null) {
                try {
                    return DimapProductHelpers.createColor(colorElem);
                } catch (IllegalArgumentException e) {
                    Debug.trace(e);
                }
            }
        }
        return null;
    }

    public static void writeXML(Placemark placemark, XmlWriter writer, int indent) {
        Guardian.assertNotNull("writer", writer);
        Guardian.assertGreaterThan("indent", indent, -1);

        final String[][] attributes = {new String[]{DimapProductConstants.ATTRIB_NAME, placemark.getName()}};
        final String[] pinTags = XmlWriter.createTags(indent, DimapProductConstants.TAG_PLACEMARK, attributes);
        writer.println(pinTags[0]);
        indent++;
        writer.printLine(indent, DimapProductConstants.TAG_PLACEMARK_LABEL, placemark.getLabel());
        writer.printLine(indent, DimapProductConstants.TAG_PLACEMARK_DESCRIPTION, placemark.getDescription());
        final GeoPos geoPos = placemark.getGeoPos();
        if (geoPos != null) {
            writer.printLine(indent, DimapProductConstants.TAG_PLACEMARK_LATITUDE, geoPos.lat);
            writer.printLine(indent, DimapProductConstants.TAG_PLACEMARK_LONGITUDE, geoPos.lon);
        }
        final PixelPos pixelPos = placemark.getPixelPos();
        if (pixelPos != null && pixelPos.isValid()) {
            writer.printLine(indent, DimapProductConstants.TAG_PLACEMARK_PIXEL_X, pixelPos.x);
            writer.printLine(indent, DimapProductConstants.TAG_PLACEMARK_PIXEL_Y, pixelPos.y);
        }
        final String styleCss = placemark.getStyleCss();
        if (styleCss != null && !styleCss.isEmpty()) {
            writer.printLine(indent, DimapProductConstants.TAG_PLACEMARK_STYLE_CSS, styleCss);
        }
        writer.println(pinTags[1]);
    }

    public static void writeColor(final String tagName, final int indent, final Color color, final XmlWriter writer) {
        final String[] colorTags = XmlWriter.createTags(indent, tagName);
        writer.println(colorTags[0]);
        DimapProductHelpers.printColorTag(indent + 1, color, writer);
        writer.println(colorTags[1]);
    }
}
