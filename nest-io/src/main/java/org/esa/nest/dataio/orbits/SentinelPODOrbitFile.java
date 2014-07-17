/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dataio.orbits;

import org.esa.beam.framework.datamodel.ProductData;
import org.esa.nest.datamodel.Orbits;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Sentinel POD Orbit File
 */
public class SentinelPODOrbitFile implements OrbitFile {

    // 5 types of orbits:
    // 1) Predicted
    // 2) Restituted
    // 3) MOE
    // 4) POE
    // 5) NRT Restituted

    private File orbitFile = null;

    private final class FixedHeader {

        // We can add more members as needed

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

    private FixedHeader fixedHeader = null;

    private final class OSV {

        private final String tai;
        private final String utc;
        private final String ut1;
        private final int absoluteOrbit;
        private final float x;
        private final float y;
        private final float z;
        private final float vx;
        private final float vy;
        private final float vz;
        private final String quality;

        private double utcMJD;

        OSV(final double utcMJD) {

            this.tai = "";
            this.utc = "";
            this.ut1 = "";
            this.absoluteOrbit = 0;
            this.x = 0.0F;
            this.y = 0.0F;
            this.z = 0.0F;
            this.vx = 0.0F;
            this.vy = 0.0F;
            this.vz = 0.0F;
            this.quality = "";

            this.utcMJD = utcMJD;
        }

        OSV(final String tai, final String utc, final String ut1, final int absoluteOrbit, final float x, final float y, final float z, final float vx, final float vy, final float vz, final String quality) throws Exception{

            this.tai = tai;
            this.utc = utc;
            this.ut1 = ut1;
            this.absoluteOrbit = absoluteOrbit;
            this.x = x;
            this.y = y;
            this.z = z;
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
            this.quality = quality;

            this.utcMJD = ProductData.UTC.parse(convertUTC(this.utc)).getMJD();
        }

        void dump() {

            System.out.println("SentinelPODOrbitFile.OSV:");
            System.out.println("  tai " + tai);
            System.out.println("  utc " + utc);
            System.out.println("  ut1 " + ut1);
            System.out.println("  absoluteOrbit " + absoluteOrbit);
            System.out.println("  x = " + x + " y = " + y + " z = " + z);
            System.out.println("  vx = " + vx + " vy = " + vy + " vz = " + vz);
            System.out.println("  quality = " + quality);
            System.out.println("  utcMJD = " + utcMJD);
        }
    }

    private final List<OSV> osvList = new ArrayList<>();

    Comparator<OSV> osvComparator = new Comparator<OSV>() {
        @Override
        public int compare(OSV osv1, OSV osv2) {
            if (osv1.utcMJD < osv2.utcMJD) {
                return -1;
            } else if (osv1.utcMJD > osv2.utcMJD) {
                return 1;
            } else {
                return 0;
            }
        }
    };

    public SentinelPODOrbitFile(final String fullFilePath) throws Exception {

        final File orbitFile = findPODOrbitFile(fullFilePath);

        if (orbitFile == null) {
            throw new IOException("SentinelPODOrbitFile: Unable to find POD orbit file");
        }
    }

    public File getOrbitFile() {
        return orbitFile;
    }

    /**
     * Get orbit information for given time.
     *
     * @param utc The UTC in days.
     * @return The orbit information.
     * @throws Exception The exceptions.
     */
    public Orbits.OrbitData getOrbitData(final double utc) throws Exception {

        final OSV osv = new OSV(utc);
        int idx = Collections.binarySearch(osvList, osv, osvComparator);

        if (idx < 0) {

            // insertionPt is the index of the first element greater than the key
            final int insertionPt = -(idx+1);

            if (insertionPt == osvList.size()) {

                // All points are less than utc, so return the last point which is the greatest.
                idx = insertionPt - 1;

            } else if (insertionPt == 0) {

                // All points are greater than utc, so return the first point which is the smallest.
                idx = 0;

            } else {

                // utc is between insertionPt and the point before it.
                final OSV osv1 = osvList.get(insertionPt-1);
                final OSV osv2 = osvList.get(insertionPt);

                if (Math.abs(osv1.utcMJD - utc) > Math.abs(osv2.utcMJD - utc)) {
                    idx = insertionPt;
                } else {
                    idx = insertionPt-1;
                }
            }
        }

        final Orbits.OrbitData orbitData = new Orbits.OrbitData();

        orbitData.xPos = osvList.get(idx).x;
        orbitData.yPos = osvList.get(idx).y;
        orbitData.zPos = osvList.get(idx).z;
        orbitData.xVel = osvList.get(idx).vx;
        orbitData.yVel = osvList.get(idx).vy;
        orbitData.zVel = osvList.get(idx).vz;

        return orbitData;
    }



    /**
     * Find POD orbit file.
     */
    private File findPODOrbitFile(final String fullFilePath)
            throws Exception {

        orbitFile = new File(fullFilePath);
        if (!orbitFile.exists()) {
                return null;
        }

        // read content of the orbit file
        readOrbitFile();

        return orbitFile;
    }

    private void readOrbitFile() throws Exception {

        try {
            final DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
            final Document doc = documentBuilder.parse(orbitFile);

            if (doc == null) {

                System.out.println("SentinelPODOrbitFile.readOrbitFile: ERROR failed to create Document for orbit file");
                return;
            }

            doc.getDocumentElement().normalize();

            final NodeList nodeList = doc.getElementsByTagName("Earth_Explorer_File");
            if (nodeList.getLength() != 1) {
                System.out.println("SentinelPODOrbitFile.readOrbitFile: ERROR found this many Earth_Explorer_File " + nodeList.getLength());
                return;
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

                        }  else if (headerChildNode.getNodeName().equals("Variable_Header")) {

                            variableHeaderNode = headerChildNode;
                        }
                    }

                }  else if (fileChildNode.getNodeName().equals("Data_Block")) {

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

            // Don't need anything from Variable_Header.

            if (listOfOSVsNode != null) {

                readOSVList(listOfOSVsNode);
            }

        } catch (IOException e) {

            System.out.println("SentinelPODOrbitFile.readOrbitFile: IOException " + e.getMessage());

        } catch (ParserConfigurationException e) {

            System.out.println("SentinelPODOrbitFile.readOrbitFile: ParserConfigurationException " + e.getMessage());

        } catch (SAXException e) {

            System.out.println("SentinelPODOrbitFile.readOrbitFile: SAXException " + e.getMessage());
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

            if (fixedHeaderChildNode.getNodeName().equals("Mission")) {

                mission = fixedHeaderChildNode.getTextContent();

            } else if (fixedHeaderChildNode.getNodeName().equals("File_Type")) {

                fileType = fixedHeaderChildNode.getTextContent();

            } else if (fixedHeaderChildNode.getNodeName().equals("Validity_Period")) {

                final NodeList validityPeriodChildNodes = fixedHeaderChildNode.getChildNodes();

                for (int j = 0; j < validityPeriodChildNodes.getLength(); j++) {

                    final org.w3c.dom.Node validityPeriodChildNode = validityPeriodChildNodes.item(j);

                    if (validityPeriodChildNode.getNodeName().equals("Validity_Start")) {

                        validityStart = validityPeriodChildNode.getTextContent();

                    } else if (validityPeriodChildNode.getNodeName().equals("Validity_Stop")) {

                        validityStop = validityPeriodChildNode.getTextContent();
                    }
                }
            }

            if (mission != null && fileType != null && validityStart != null && validityStop != null) {

                fixedHeader = new FixedHeader(mission, fileType, validityStart, validityStop);
                break;
            }
        }
    }

    private void readOSVList(final org.w3c.dom.Node listOfOSVsNode) throws Exception {

        final org.w3c.dom.Node attrCount = getAttributeFromNode(listOfOSVsNode, "count");

        if (attrCount == null) {
            return;
        }

        final int count = Integer.parseInt(attrCount.getTextContent());

        //System.out.println("SentinelPODOrbitFile.readOSVList: List_of_OSVs count = " + count);

        org.w3c.dom.Node childNode = listOfOSVsNode.getFirstChild();
        int osvCnt = 0;

        while (childNode != null) {

            if (childNode.getNodeName().equals("OSV")) {

                osvCnt++;
                readOneOSV(childNode);
            }

            childNode = childNode.getNextSibling();
        }

        Collections.sort(osvList, osvComparator);

        //System.out.println("SentinelPODOrbitFile.readOSVList: osvCnt = " + osvCnt);

        if (count != osvCnt) {

            System.out.println("SentinelPODOrbitFile.readOSVList: WARNING List_of_OSVs count = " + count + " but found only " + osvCnt + " OSV");
        }
    }

    private void readOneOSV(final org.w3c.dom.Node osvNode) throws Exception {

        String tai = "";
        String utc = "";
        String ut1 = "";
        int absoluteOrbit = 0;
        float x = 0.0F;
        float y = 0.0F;
        float z = 0.0F;
        float vx = 0.0F;
        float vy = 0.0F;
        float vz = 0.0F;
        String quality = "";

        org.w3c.dom.Node childNode = osvNode.getFirstChild();

        while (childNode != null) {

            switch (childNode.getNodeName()) {
                case "TAI": {
                    tai = childNode.getTextContent();
                }
                    break;
                case "UTC": {
                    utc = childNode.getTextContent();
                }
                    break;
                case "UT1": {
                    ut1 = childNode.getTextContent();
                }
                    break;
                case "Absolute_Orbit": {
                    absoluteOrbit = Integer.parseInt(childNode.getTextContent());
                }
                    break;
                case "X": {
                    x = Float.parseFloat(childNode.getTextContent());
                }
                    break;
                case "Y": {
                    y = Float.parseFloat(childNode.getTextContent());
                }
                    break;
                case "Z": {
                    z = Float.parseFloat(childNode.getTextContent());
                }
                    break;
                case "VX": {
                    vx = Float.parseFloat(childNode.getTextContent());
                }
                    break;
                case "VY": {
                    vy = Float.parseFloat(childNode.getTextContent());
                }
                    break;
                case "VZ": {
                    vz = Float.parseFloat(childNode.getTextContent());
                }
                    break;
                case "Quality": {
                    quality = childNode.getTextContent();
                }
                    break;
                default:
                    break;
            }

            childNode = childNode.getNextSibling();
        }

        OSV osv = new OSV(tai, utc, ut1, absoluteOrbit, x, y, z, vx, vy, vz, quality);
        osvList.add(osv);

        //osv.dump();
    }

    // TODO This is copied from Sentinel1Level0Reader.java; may be we should put in in some utilities class.
    private org.w3c.dom.Node getAttributeFromNode(final org.w3c.dom.Node node, final String attrName) {

        // Will look for attribute called attrName in the given node

        NamedNodeMap attr = node.getAttributes();

        org.w3c.dom.Node attrNode = null;

        for (int j = 0; j < attr.getLength(); j++) {

            if (attr.item(j).getNodeName().equals(attrName)) {
                if (attrNode == null) {
                    attrNode = attr.item(j);
                } else {
                    // Should not be possible
                    System.out.println("SentinelPODOrbitFile.getAttributeFromNode: WARNING more than one " + attrName + " in " + node.getNodeName());
                }
            }
        }

        if (attrNode == null) {
            System.out.println("SentinelPODOrbitFile.getAttributeFromNode: Failed to find " + attrName + " in " + node.getNodeName());
        }

        return attrNode;
    }

    public static String convertUTC(String utc) {

        final String dd = utc.substring(12, 14);
        final String mm = utc.substring(9, 11);
        final String yyyy = utc.substring(4, 8);
        final String hhmmss = utc.substring(15);

        String MMM = "MMM";

        switch (mm) {
            case "01": {
                MMM = "jan";
            }
                break;
            case "02": {
                MMM = "feb";
            }
                break;
            case "03": {
                MMM = "mar";
            }
                break;
            case "04": {
                MMM = "apr";
            }
                break;
            case "05": {
                MMM = "may";
            }
                break;
            case "06": {
                MMM = "jun";
            }
                break;
            case "07": {
                MMM = "jul";
            }
                break;
            case "08": {
                MMM = "aug";
            }
                break;
            case "09": {
                MMM = "sep";
            }
                break;
            case "10": {
                MMM = "oct";
            }
                break;
            case "11": {
                MMM = "nov";
            }
                break;
            case "12": {
                MMM = "dec";
            }
                break;
            default:
                break;
        }

        return dd + "-" + MMM + "-" + yyyy + " " + hhmmss;
    }

    public static String getMissionIDFromFilename(String filename) {

        return filename.substring(0,3);
    }

    public static String getFileTypeFromFilename(String filename) {

        return filename.substring(9, 19);
    }

    private static String extractUTCTimeFromFilename(final String filename, final int offset) {

        final String yyyy = filename.substring(offset,offset+4);
        final String mmDate = filename.substring(offset+4, offset+6);
        final String dd = filename.substring(offset+6, offset+8);
        final String hh = filename.substring(offset+9, offset+11);
        final String mmTime = filename.substring(offset+11, offset+13);
        final String ss = filename.substring(offset+13, offset+15);

        return "UTC=" + yyyy + "-" + mmDate + "-" + dd + "T" + hh + ":" + mmTime + ":" + ss;
    }

    public static String getValidityStartFromFilename(String filename) {

        if (filename.substring(41,42).equals("V")) {

            return extractUTCTimeFromFilename(filename, 42);

        } else {

            return null;
        }
    }

    public static String getValidityStopFromFilename(String filename) {

        if (filename.substring(41,42).equals("V")) {

            return extractUTCTimeFromFilename(filename, 58);

        } else {

            return null;
        }
    }

    public String getMissionFromHeader() {

        if (fixedHeader != null) {
            return fixedHeader.mission;
        } else {
            return null;
        }
    }

    public String getFileTypeFromHeader() {

        if (fixedHeader != null) {
            return fixedHeader.fileType;
        } else {
            return null;
        }
    }

    public String getValidityStartFromHeader() {

        if (fixedHeader != null) {
            return fixedHeader.validityStart;
        } else {
            return null;
        }
    }

    public String getValidityStopFromHeader() {

        if (fixedHeader != null) {
            return fixedHeader.validityStop;
        } else {
            return null;
        }
    }
}