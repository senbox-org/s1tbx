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

package org.esa.beam.dataio.placemark;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.ValidationException;
import org.esa.beam.dataio.dimap.DimapProductConstants;
import org.esa.beam.dataio.dimap.DimapProductHelpers;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.PlacemarkDescriptor;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.util.Debug;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.XmlWriter;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.util.logging.BeamLogManager;
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
import java.io.BufferedReader;
import java.io.IOException;
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

/**
 * Utility class, that reads and writes placemarks from and to various plain text formats.
 *
 * @author Nobody wants to be responsible for this mess.
 * @since BEAM 1.0
 * @deprecated since BEAM 4.10, Placemark API is being fully renewed
 */
@Deprecated
public class PlacemarkIO {

    public static final String FILE_EXTENSION_FLAT_OLD = ".pnf";
    public static final String FILE_EXTENSION_XML_OLD = ".pnx";
    public static final String FILE_EXTENSION_FLAT_TEXT = ".txt";
    public static final String FILE_EXTENSION_PLACEMARK = ".placemark";

    private static final int INDEX_FOR_NAME = 0;
    private static final int INDEX_FOR_LON = 1;
    private static final int INDEX_FOR_LAT = 2;
    private static final int INDEX_FOR_DESC = 3;
    private static final int INDEX_FOR_LABEL = 4;
    private static final int INDEX_FOR_DATETIME = 5;

    private static final String LABEL_COL_NAME = "Label";
    private static final String LON_COL_NAME = "Lon";
    private static final String LAT_COL_NAME = "Lat";
    private static final String NAME_COL_NAME = "Name";
    private static final String DESC_COL_NAME = "Desc";
    private static final String DATETIME_COL_NAME = "DateTime";

    private static final String ISO8601_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
    private static final DateFormat dateFormat = ProductData.UTC.createDateFormat(ISO8601_PATTERN);

    private PlacemarkIO() {

    }

    public static List<Placemark> readPlacemarks(Reader reader, GeoCoding geoCoding,
                                                 PlacemarkDescriptor placemarkDescriptor) throws IOException {
        final char[] magicBytes = new char[5];
        PushbackReader inputReader = new PushbackReader(reader, magicBytes.length);
        try {
            inputReader.read(magicBytes);
            inputReader.unread(magicBytes);
            if (XmlWriter.XML_HEADER_LINE.startsWith(new String(magicBytes))) {
                return readPlacemarksFromXMLFile(inputReader, geoCoding, placemarkDescriptor);
            } else {
                return readPlacemarksFromFlatFile(inputReader, geoCoding, placemarkDescriptor);
            }
        } finally {
            inputReader.close();
        }
    }

    private static List<Placemark> readPlacemarksFromFlatFile(Reader reader, GeoCoding geoCoding,
                                                              PlacemarkDescriptor placemarkDescriptor) throws
            IOException {
        ArrayList<Placemark> placemarks = new ArrayList<Placemark>();
        BufferedReader lineReader = new BufferedReader(reader);
        try {
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
                    int nameIndex = StringUtils.indexOf(strings, NAME_COL_NAME);
                    int lonIndex = StringUtils.indexOf(strings, LON_COL_NAME);
                    int latIndex = StringUtils.indexOf(strings, LAT_COL_NAME);
                    int descIndex = StringUtils.indexOf(strings, DESC_COL_NAME);
                    int labelIndex = StringUtils.indexOf(strings, LABEL_COL_NAME);
                    int dateTimeIndex = StringUtils.indexOf(strings, DATETIME_COL_NAME);
                    if (nameIndex == -1 || lonIndex == -1 || latIndex == -1) {
                        throw new IOException("Invalid placemark file format:\n" +
                                                      "at least the columns 'Name', 'Lon' and 'Lat' must be given.");
                    }
                    biggestIndex = biggestIndex > nameIndex ? biggestIndex : nameIndex;
                    biggestIndex = biggestIndex > lonIndex ? biggestIndex : lonIndex;
                    biggestIndex = biggestIndex > latIndex ? biggestIndex : latIndex;
                    stdColIndexes = new ArrayList<Integer>(6);
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
                        float lon;
                        try {
                            lon = Float.parseFloat(strings[stdColIndexes.get(INDEX_FOR_LON)]);
                        } catch (NumberFormatException ignored) {
                            throw new IOException("Invalid placemark file format:\n" +
                                                          "data row " + row + ": value for 'Lon' is invalid");      /*I18N*/
                        }
                        float lat;
                        try {
                            lat = Float.parseFloat(strings[stdColIndexes.get(INDEX_FOR_LAT)]);
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
        } finally {
            lineReader.close();
        }

        return placemarks;
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
            final ArrayList<Placemark> placemarks = new ArrayList<Placemark>(children.size());
            for (Object child : children) {
                final Element element = (Element) child;
                try {
                    final Placemark placemark = createPlacemark(element, placemarkDescriptor, geoCoding);
                    placemarks.add(placemark);
                } catch (IllegalArgumentException ignored) {
                }
            }
            return placemarks;
        } catch (FactoryConfigurationError e) {
            throw new IOException(e.toString(), e);
        } catch (ParserConfigurationException e) {
            throw new IOException(e.toString(), e);
        } catch (SAXException e) {
            throw new IOException(e.toString(), e);
        } catch (IOException e) {
            throw new IOException(e.toString(), e);
        }
    }

    public static void writePlacemarksWithAdditionalData(Writer writer,
                                                         String roleLabel, String productName,
                                                         List<Placemark> placemarkList, List<Object[]> valueList,
                                                         String[] standardColumnNames, String[] additionalColumnNames) {
        int columnCountMin = standardColumnNames.length;
        int columnCount = columnCountMin + additionalColumnNames.length;
        final PrintWriter pw = new PrintWriter(writer);
        try {
            // Write file header
            pw.println("# BEAM " + roleLabel + " export table");
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
        } finally {
            pw.close();
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
        sb.append(DESC_COL_NAME);
        for (String additionalColumnName : additionalColumnNames) {
            sb.append("\t");
            sb.append(additionalColumnName);
        }
        return sb.toString();
    }

    private static String getWavelengthLine(Product product, String[] standardColumnNames, String[] additionalColumnNames) {
        StringBuilder sb = new StringBuilder();

        sb.append("# Wavelength:"); // --> Name
        for (String standardColumnName : standardColumnNames) {
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

    public static BeamFileFilter createTextFileFilter() {
        return new BeamFileFilter("PLACEMARK_TEXT_FILE",
                                  new String[]{FILE_EXTENSION_FLAT_TEXT, FILE_EXTENSION_FLAT_OLD},
                                  "Placemark files - flat text format");
    }

    public static BeamFileFilter createPlacemarkFileFilter() {
        return new BeamFileFilter("PLACEMARK_XML_FILE",
                                  new String[]{FILE_EXTENSION_PLACEMARK, FILE_EXTENSION_XML_OLD},
                                  "Placemark files - XML format");
    }

    /**
     * Creates a new placemark from an XML element and a given symbol.
     *
     * @param element    the element.
     * @param descriptor the descriptor of the placemark.
     * @param geoCoding  the geoCoding to used by the placemark. Can be <code>null</code>.
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
                float lat = Float.parseFloat(latText);
                float lon = Float.parseFloat(lonText);
                geoPos = new GeoPos(lat, lon);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid geo-position.", e);
            }
        }
        PixelPos pixelPos = null;
        if (posXText != null && posYText != null) {
            try {
                float pixelX = Float.parseFloat(posXText);
                float pixelY = Float.parseFloat(posYText);
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
        if(styleCss == null) {
            final String placemarkStyleCss = placemark.getStyleCss();
            styleCss = StringUtils.isNullOrEmpty(placemarkStyleCss) ? getStyleCssFromOldFormat(element) :
                       placemarkStyleCss + ";" + getStyleCssFromOldFormat(element);
        }
        placemark.setStyleCss(styleCss);

        return placemark;
    }

    private static String getStyleCssFromOldFormat(Element element) {
        StringBuilder styleCss = new StringBuilder();
        buildStyleCss(getColorProperty(element, DimapProductConstants.TAG_PLACEMARK_FILL_COLOR, "fill"), styleCss);
        buildStyleCss(getColorProperty(element, DimapProductConstants.TAG_PLACEMARK_OUTLINE_COLOR, "stroke"), styleCss);
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
                BeamLogManager.getSystemLogger().warning(e.getMessage());
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
                } catch (NumberFormatException e) {
                    Debug.trace(e);
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
