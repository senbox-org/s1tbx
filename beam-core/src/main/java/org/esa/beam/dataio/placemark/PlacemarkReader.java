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
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.DOMBuilder;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

public class PlacemarkReader {

    private static final int INDEX_FOR_NAME = 0;
    private static final int INDEX_FOR_LON = 1;
    private static final int INDEX_FOR_LAT = 2;
    private static final int INDEX_FOR_DESC = 3;
    private static final int INDEX_FOR_LABEL = 4;

    private static final String LABEL_COL_NAME = "Label";
    private static final String LON_COL_NAME = "Lon";
    private static final String LAT_COL_NAME = "Lat";
    public static final String NAME_COL_NAME = "Name";
    public static final String DESC_COL_NAME = "Desc";

    private PlacemarkReader() {
    }

    public static Placemark[] readPlacemarks(File inputFile, GeoCoding geoCoding,
                                             PlacemarkDescriptor placemarkDescriptor) throws IOException {
        final byte[] magicBytes = new byte[5];
        final DataInputStream dataInputStream = new DataInputStream(new FileInputStream(inputFile));
        try {
            //noinspection ResultOfMethodCallIgnored
            dataInputStream.read(magicBytes);  // todo - BAD PRACTICE HERE!!!
        } finally {
            dataInputStream.close();
        }
        if (XmlWriter.XML_HEADER_LINE.startsWith(new String(magicBytes))) {
            return readPlacemarksFromXMLFile(inputFile, geoCoding, placemarkDescriptor);
        } else {
            return readPlacemarksFromFlatFile(inputFile, geoCoding, placemarkDescriptor);
        }
    }

    static Placemark[] readPlacemarksFromFlatFile(File inputFile, GeoCoding geoCoding,
                                                  PlacemarkDescriptor placemarkDescriptor) throws IOException {
        assert inputFile != null;
        ArrayList<Placemark> placemarks = new ArrayList<Placemark>();
        final RandomAccessFile file = new RandomAccessFile(inputFile, "r");
        try {
            int row = 0;
            int[] columnIndexes = null;
            int biggestIndex = 0;
            while (true) {
                String line = file.readLine();
                if (line == null) {
                    break;
                }
                line = line.trim(); // cut \n and \r from the end of the line
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] strings = StringUtils.toStringArray(line, "\t");
                if (columnIndexes == null) {
                    int nameIndex = StringUtils.indexOf(strings, NAME_COL_NAME);
                    int lonIndex = StringUtils.indexOf(strings, LON_COL_NAME);
                    int latIndex = StringUtils.indexOf(strings, LAT_COL_NAME);
                    int descIndex = StringUtils.indexOf(strings, DESC_COL_NAME);
                    int labelIndex = StringUtils.indexOf(strings, LABEL_COL_NAME);
                    if (nameIndex == -1 || lonIndex == -1 || latIndex == -1) {
                        throw new IOException("Invalid placemark file format:\n" +
                                              "at least the columns 'Name', 'Lon' and 'Lat' must be given.");
                    }
                    biggestIndex = biggestIndex > nameIndex ? biggestIndex : nameIndex;
                    biggestIndex = biggestIndex > lonIndex ? biggestIndex : lonIndex;
                    biggestIndex = biggestIndex > latIndex ? biggestIndex : latIndex;
                    columnIndexes = new int[5];
                    columnIndexes[INDEX_FOR_NAME] = nameIndex;
                    columnIndexes[INDEX_FOR_LON] = lonIndex;
                    columnIndexes[INDEX_FOR_LAT] = latIndex;
                    columnIndexes[INDEX_FOR_DESC] = descIndex;
                    columnIndexes[INDEX_FOR_LABEL] = labelIndex;
                } else {
                    row++;
                    if (strings.length > biggestIndex) {
                        String name = strings[columnIndexes[INDEX_FOR_NAME]];
                        float lon;
                        try {
                            lon = Float.parseFloat(strings[columnIndexes[INDEX_FOR_LON]]);
                        } catch (NumberFormatException ignored) {
                            throw new IOException("Invalid placemark file format:\n" +
                                                  "data row " + row + ": value for 'Lon' is invalid");      /*I18N*/
                        }
                        float lat;
                        try {
                            lat = Float.parseFloat(strings[columnIndexes[INDEX_FOR_LAT]]);
                        } catch (NumberFormatException ignored) {
                            throw new IOException("Invalid placemark file format:\n" +
                                                  "data row " + row + ": value for 'Lat' is invalid");      /*I18N*/
                        }
                        String desc = null;
                        if (columnIndexes[INDEX_FOR_DESC] >= 0 && strings.length > columnIndexes[INDEX_FOR_DESC]) {
                            desc = strings[columnIndexes[INDEX_FOR_DESC]];
                        }
                        String label = name;
                        if (columnIndexes[INDEX_FOR_LABEL] >= 0 && strings.length > columnIndexes[INDEX_FOR_LABEL]) {
                            label = strings[columnIndexes[INDEX_FOR_LABEL]];
                        }
                        Placemark placemark = new Placemark(name, label, "", null, new GeoPos(lat, lon),
                                                            placemarkDescriptor, geoCoding);
                        if (desc != null) {
                            placemark.setDescription(desc);
                        }
                        placemarks.add(placemark);
                    } else {
                        throw new IOException("Invalid placemark file format:\n" +
                                              "data row " + row + ": values for 'Name', 'Lon' and 'Lat' must be given.");   /*I18N*/
                    }
                }
            }
        } finally {
            file.close();
        }

        return placemarks.toArray(new Placemark[placemarks.size()]);
    }

    static Placemark[] readPlacemarksFromXMLFile(File inputFile, GeoCoding geoCoding,
                                                 PlacemarkDescriptor placemarkDescriptor) throws IOException {
        assert inputFile != null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document w3cDocument = builder.parse(inputFile);
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
            return placemarks.toArray(new Placemark[placemarks.size()]);
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
}
