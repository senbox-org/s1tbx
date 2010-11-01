/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.esa.beam.dataio.dimap.DimapProductConstants;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.PlacemarkDescriptor;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.XmlWriter;
import org.esa.beam.util.io.BeamFileFilter;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.DOMBuilder;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.Writer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

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
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat(ISO8601_PATTERN, Locale.getDefault());

    static {
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private PlacemarkIO() {

    }

    public static List<Placemark> readPlacemarks(Reader reader, GeoCoding geoCoding,
                                                 PlacemarkDescriptor placemarkDescriptor) throws IOException {
        final char[] magicBytes = new char[5];
        PushbackReader inputReader = new PushbackReader(reader, magicBytes.length);
        try {
            inputReader.read(magicBytes); // todo - BAD PRACTICE HERE!!!
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
            Map<String, Integer> additionalCols = new HashMap<String, Integer>();
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

                        Placemark placemark = new Placemark(name, label, "", null, new GeoPos(lat, lon),
                                                            placemarkDescriptor, geoCoding);
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
                    final Placemark placemark = Placemark.createPlacemark(element, placemarkDescriptor, geoCoding);
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

            // Write header columns
            pw.print(NAME_COL_NAME + "\t");
            for (String name : standardColumnNames) {
                pw.print(name + "\t");
            }
            pw.print(DESC_COL_NAME + "\t");
            for (String additionalColumnName : additionalColumnNames) {
                pw.print(additionalColumnName + "\t");
            }
            pw.println();

            for (int i = 0; i < placemarkList.size(); i++) {
                Placemark placemark = placemarkList.get(i);
                Object[] values = valueList.get(i);
                pw.print(placemark.getName() + "\t");
                for (int col = 0; col < columnCountMin; col++) {
                    printValue(pw, values[col]);
                }
                pw.print(placemark.getDescription() + "\t");
                for (int col = columnCountMin; col < columnCount; col++) {
                    printValue(pw, values[col]);
                }
                pw.println();

            }
        } finally {
            pw.close();
        }
    }

    private static void printValue(PrintWriter pw, Object value) {
        String stringValue;
        if (value instanceof Date) {
            stringValue = dateFormat.format(value);
        } else {
            stringValue = value.toString();
        }
        pw.print(stringValue + "\t");
    }

    public static void writePlacemarksFile(Writer writer, List<Placemark> placemarks) throws IOException {

        XmlWriter xmlWriter = new XmlWriter(writer, true);
        final String[] tags = XmlWriter.createTags(0, "Placemarks");
        xmlWriter.println(tags[0]);
        for (Placemark placemark : placemarks) {
            if (placemark != null) {
                placemark.writeXML(xmlWriter, 1);
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
}
