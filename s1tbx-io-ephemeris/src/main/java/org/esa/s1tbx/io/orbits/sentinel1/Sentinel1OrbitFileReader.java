/*
 * Copyright (C) 2021 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.esa.s1tbx.io.orbits.sentinel1;

import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.datamodel.Orbits;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Sentinel1OrbitFileReader {

    final static DateFormat dateFormat = ProductData.UTC.createDateFormat("yyyyMMdd-HHmmss");
    final static DateFormat orbitDateFormat = ProductData.UTC.createDateFormat("yyyy-MM-dd HH:mm:ss");

    private FixedHeader fixedHeader = null;
    private final File orbitFile;
    private List<Orbits.OrbitVector> osvList = new ArrayList<>();

    Sentinel1OrbitFileReader(final File file) {
        this.orbitFile = file;
    }

    public List<Orbits.OrbitVector> getOrbitStateVectors() {
        return osvList;
    }

    void read() throws Exception {
        final DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();

        final Document doc;
        if (orbitFile.getName().toLowerCase().endsWith(".zip")) {
            final ZipFile productZip = new ZipFile(orbitFile, ZipFile.OPEN_READ);
            final Enumeration<? extends ZipEntry> entries = productZip.entries();
            final ZipEntry zipEntry = entries.nextElement();

            doc = documentBuilder.parse(productZip.getInputStream(zipEntry));
        } else {
            doc = documentBuilder.parse(orbitFile);
        }

        doc.getDocumentElement().normalize();

        final NodeList nodeList = doc.getElementsByTagName("Earth_Explorer_File");
        if (nodeList.getLength() != 1) {
            throw new Exception("SentinelPODOrbitFile.readOrbitFile: ERROR found too many Earth_Explorer_File " + nodeList.getLength());
        }

        org.w3c.dom.Node fixedHeaderNode = null;
        org.w3c.dom.Node variableHeaderNode = null;
        org.w3c.dom.Node listOfOSVsNode = null;

        final NodeList fileChildNodes = nodeList.item(0).getChildNodes();

        for (int i = 0; i < fileChildNodes.getLength(); i++) {

            final org.w3c.dom.Node fileChildNode = fileChildNodes.item(i);

            if (fileChildNode.getNodeName().equals("Earth_Explorer_Header")) {

                final NodeList headerChildNodes = fileChildNode.getChildNodes();

                for (int j = 0; j < headerChildNodes.getLength(); j++) {

                    final org.w3c.dom.Node headerChildNode = headerChildNodes.item(j);

                    if (headerChildNode.getNodeName().equals("Fixed_Header")) {

                        fixedHeaderNode = headerChildNode;

                    } else if (headerChildNode.getNodeName().equals("Variable_Header")) {

                        variableHeaderNode = headerChildNode;
                    }
                }

            } else if (fileChildNode.getNodeName().equals("Data_Block")) {

                final NodeList dataBlockChildNodes = fileChildNode.getChildNodes();

                for (int j = 0; j < dataBlockChildNodes.getLength(); j++) {

                    final org.w3c.dom.Node dataBlockChildNode = dataBlockChildNodes.item(j);

                    if (dataBlockChildNode.getNodeName().equals("List_of_OSVs")) {

                        listOfOSVsNode = dataBlockChildNode;
                    }
                }
            }

            if (fixedHeaderNode != null && variableHeaderNode != null && listOfOSVsNode != null) {
                break;
            }
        }

        if (fixedHeaderNode != null) {

            readFixedHeader(fixedHeaderNode);
        }

        if (listOfOSVsNode != null) {

            osvList = readOSVList(listOfOSVsNode);
        }
    }

    private void readFixedHeader(final org.w3c.dom.Node fixedHeaderNode) {

        final NodeList fixedHeaderChildNodes = fixedHeaderNode.getChildNodes();

        String mission = null;
        String fileType = null;
        String validityStart = null;
        String validityStop = null;

        for (int i = 0; i < fixedHeaderChildNodes.getLength(); i++) {

            final org.w3c.dom.Node fixedHeaderChildNode = fixedHeaderChildNodes.item(i);

            switch (fixedHeaderChildNode.getNodeName()) {
                case "Mission":

                    mission = fixedHeaderChildNode.getTextContent();

                    break;
                case "File_Type":

                    fileType = fixedHeaderChildNode.getTextContent();

                    break;
                case "Validity_Period":

                    final NodeList validityPeriodChildNodes = fixedHeaderChildNode.getChildNodes();

                    for (int j = 0; j < validityPeriodChildNodes.getLength(); j++) {

                        final org.w3c.dom.Node validityPeriodChildNode = validityPeriodChildNodes.item(j);

                        if (validityPeriodChildNode.getNodeName().equals("Validity_Start")) {

                            validityStart = validityPeriodChildNode.getTextContent();

                        } else if (validityPeriodChildNode.getNodeName().equals("Validity_Stop")) {

                            validityStop = validityPeriodChildNode.getTextContent();
                        }
                    }
                    break;
            }

            if (mission != null && fileType != null && validityStart != null && validityStop != null) {

                fixedHeader = new FixedHeader(mission, fileType, validityStart, validityStop);
                break;
            }
        }
    }

    private static List<Orbits.OrbitVector> readOSVList(final org.w3c.dom.Node listOfOSVsNode) throws Exception {

        final org.w3c.dom.Node attrCount = getAttributeFromNode(listOfOSVsNode, "count");
        final int count = Integer.parseInt(attrCount.getTextContent());
        final List<Orbits.OrbitVector> osvList = new ArrayList<>();

        org.w3c.dom.Node childNode = listOfOSVsNode.getFirstChild();
        int osvCnt = 0;

        while (childNode != null) {

            if (childNode.getNodeName().equals("OSV")) {

                osvCnt++;
                osvList.add(readOneOSV(childNode));
            }

            childNode = childNode.getNextSibling();
        }

        osvList.sort(new Orbits.OrbitComparator());

        if (count != osvCnt) {
            SystemUtils.LOG.warning("SentinelPODOrbitFile.readOSVList: WARNING List_of_OSVs count = " + count + " but found only " + osvCnt + " OSV");
        }

        return osvList;
    }

    private static Orbits.OrbitVector readOneOSV(final org.w3c.dom.Node osvNode) throws Exception {

        String utc = "";
        double x = 0.0;
        double y = 0.0;
        double z = 0.0;
        double vx = 0.0;
        double vy = 0.0;
        double vz = 0.0;

        org.w3c.dom.Node childNode = osvNode.getFirstChild();

        while (childNode != null) {

            switch (childNode.getNodeName()) {
                case "UTC": {
                    utc = childNode.getTextContent();
                }
                break;
                case "X": {
                    x = Double.parseDouble(childNode.getTextContent());
                }
                break;
                case "Y": {
                    y = Double.parseDouble(childNode.getTextContent());
                }
                break;
                case "Z": {
                    z = Double.parseDouble(childNode.getTextContent());
                }
                break;
                case "VX": {
                    vx = Double.parseDouble(childNode.getTextContent());
                }
                break;
                case "VY": {
                    vy = Double.parseDouble(childNode.getTextContent());
                }
                break;
                case "VZ": {
                    vz = Double.parseDouble(childNode.getTextContent());
                }
                break;
                default:
                    break;
            }

            childNode = childNode.getNextSibling();
        }

        final double utcTime = toUTC(utc).getMJD();

        return new Orbits.OrbitVector(utcTime, x, y, z, vx, vy, vz);
    }

    // TODO This is copied from Sentinel1Level0Reader.java; may be we should put in in some utilities class.
    private static org.w3c.dom.Node getAttributeFromNode(final org.w3c.dom.Node node, final String attrName) {

        // Will look for attribute called attrName in the given node

        NamedNodeMap attr = node.getAttributes();

        org.w3c.dom.Node attrNode = null;

        for (int j = 0; j < attr.getLength(); j++) {

            if (attr.item(j).getNodeName().equals(attrName)) {
                if (attrNode == null) {
                    attrNode = attr.item(j);
                } else {
                    // Should not be possible
                    SystemUtils.LOG.warning("SentinelPODOrbitFile.getAttributeFromNode: WARNING more than one " + attrName + " in " + node.getNodeName());
                }
            }
        }

        if (attrNode == null) {
            SystemUtils.LOG.warning("SentinelPODOrbitFile.getAttributeFromNode: Failed to find " + attrName + " in " + node.getNodeName());
        }

        return attrNode;
    }

    private static String convertUTC(String utc) {

        return utc.replace("UTC=", "").replace("T", " ");
    }

    private static String extractUTCTimeFromFilename(final String filename, final int offset) {

        final String yyyy = filename.substring(offset, offset + 4);
        final String mmDate = filename.substring(offset + 4, offset + 6);
        final String dd = filename.substring(offset + 6, offset + 8);
        final String hh = filename.substring(offset + 9, offset + 11);
        final String mmTime = filename.substring(offset + 11, offset + 13);
        final String ss = filename.substring(offset + 13, offset + 15);

        return "UTC=" + yyyy + '-' + mmDate + '-' + dd + 'T' + hh + ':' + mmTime + ':' + ss;
    }

    private static String extractTimeFromFilename(final String filename, final int offset) {

        return filename.substring(offset, offset + 15).replace("T", "-");
    }

    static ProductData.UTC getValidityStartFromFilenameUTC(String filename) throws ParseException {

        if (filename.charAt(41) == 'V') {

            String val = extractTimeFromFilename(filename, 42);
            return ProductData.UTC.parse(val, dateFormat);
        }
        return null;
    }

    static boolean isWithinRange(final String filename, final ProductData.UTC stateVectorTime) {
        try {
            final ProductData.UTC utcStart = Sentinel1OrbitFileReader.getValidityStartFromFilenameUTC(filename);
            final ProductData.UTC utcEnd = Sentinel1OrbitFileReader.getValidityStopFromFilenameUTC(filename);
            if (utcStart != null && utcEnd != null) {
                double stateVectorMJD = stateVectorTime.getMJD();
                return stateVectorMJD >= utcStart.getMJD() && stateVectorMJD < utcEnd.getMJD();
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    static ProductData.UTC getValidityStopFromFilenameUTC(String filename) throws ParseException {

        if (filename.charAt(41) == 'V') {

            String val = extractTimeFromFilename(filename, 58);
            return ProductData.UTC.parse(val, dateFormat);
        }
        return null;
    }

    static String getValidityStartFromFilename(String filename) {

        if (filename.charAt(41) == 'V') {

            return extractUTCTimeFromFilename(filename, 42);
        }
        return null;
    }

    static String getValidityStopFromFilename(String filename) {

        if (filename.charAt(41) == 'V') {

            return extractUTCTimeFromFilename(filename, 58);
        }
        return null;
    }

    String getMissionFromHeader() {

        if (fixedHeader != null) {
            return fixedHeader.mission;
        }
        return null;
    }

    String getFileTypeFromHeader() {

        if (fixedHeader != null) {
            return fixedHeader.fileType;
        }
        return null;
    }

    String getValidityStartFromHeader() {

        if (fixedHeader != null) {
            return fixedHeader.validityStart;
        }
        return null;
    }

    String getValidityStopFromHeader() {

        if (fixedHeader != null) {
            return fixedHeader.validityStop;
        }
        return null;
    }

    static ProductData.UTC toUTC(final String str) throws ParseException {
        return ProductData.UTC.parse(convertUTC(str), orbitDateFormat);
    }

    private static final class FixedHeader {

        private final String mission;
        private final String fileType;
        private final String validityStart;
        private final String validityStop;

        FixedHeader(final String mission, final String fileType, final String validityStart, final String validityStop) {

            this.mission = mission;
            this.fileType = fileType;
            this.validityStart = validityStart;
            this.validityStop = validityStop;
        }
    }
}
