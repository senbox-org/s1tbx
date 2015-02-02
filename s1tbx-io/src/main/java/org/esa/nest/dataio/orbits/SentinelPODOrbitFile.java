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

import Jama.Matrix;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.datamodel.Orbits;
import org.esa.snap.util.Maths;
import org.esa.snap.util.Settings;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.*;

/**
 * Sentinel POD Orbit File
 */
public class SentinelPODOrbitFile extends BaseOrbitFile implements OrbitFile {

    // 5 types of orbits:
    // 1) Predicted
    // 2) Restituted
    // 3) MOE
    // 4) POE
    // 5) NRT Restituted

    public final static String RESTITUTED = "Sentinel Restituded";
    public final static String PRECISE = "Sentinel Precise";

    private final File orbitFile;
    private final int polyDegree;

    private final static DateFormat dateFormat = ProductData.UTC.createDateFormat("yyyyMMdd-HHmmss");
    private final static DateFormat orbitDateFormat = ProductData.UTC.createDateFormat("yyyy-MM-dd HH:mm:ss");

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

        private final String utc;
        private final double x, y, z;
        private final double vx, vy, vz;
        private double utcMJD;

        OSV(final double utcMJD) {

            this.utc = "";
            this.x = 0.0;
            this.y = 0.0;
            this.z = 0.0;
            this.vx = 0.0;
            this.vy = 0.0;
            this.vz = 0.0;

            this.utcMJD = utcMJD;
        }

        OSV(final String utc, final double x, final double y, final double z,
            final double vx, final double vy, final double vz) throws Exception{

            this.utc = utc;
            this.x = x;
            this.y = y;
            this.z = z;
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;

            this.utcMJD = toUTC(utc).getMJD();
        }

        void dump() {

            System.out.println("SentinelPODOrbitFile.OSV:");
            System.out.println("  utc " + utc);
            System.out.println("  x = " + x + " y = " + y + " z = " + z);
            System.out.println("  vx = " + vx + " vy = " + vy + " vz = " + vz);
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

    public SentinelPODOrbitFile(final String orbitType, final MetadataElement absRoot,
                                final Product sourceProduct, final int polyDegree) throws Exception {
        super(orbitType, absRoot);
        this.orbitFile = findOrbitFile();
        this.polyDegree = polyDegree;
        if(orbitFile == null) {
            String timeStr = absRoot.getAttributeUTC(AbstractMetadata.STATE_VECTOR_TIME).format();
            throw new OperatorException("No valid orbit file found for "+timeStr);
        }

        if (!orbitFile.exists()) {
            throw new IOException("SentinelPODOrbitFile: Unable to find POD orbit file");
        }

        // read content of the orbit file
        readOrbitFile();
    }

    private File findOrbitFile() {
        final String prefix;
        final File orbitFileFolder;
        if(orbitType.endsWith(RESTITUTED)) {
            prefix = "S1A_OPER_AUX_RESORB";
            orbitFileFolder = new File(Settings.instance().get("OrbitFiles.sentinelResOrbitPath"));
        } else {
            prefix = "S1A_OPER_AUX_POEORB";
            orbitFileFolder = new File(Settings.instance().get("OrbitFiles.sentinelPOEOrbitPath"));
        }

        final File[] files = orbitFileFolder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                name = name.toUpperCase();
                return (name.endsWith(".ZIP") || name.endsWith(".EOF")) && name.startsWith(prefix);
            }
        });
        if(files == null || files.length == 0)
            return null;

        final double stateVectorTime = absRoot.getAttributeUTC(AbstractMetadata.STATE_VECTOR_TIME).getMJD();
        for(File file : files) {
            try {
                final String filename = file.getName();
                final ProductData.UTC utcStart = SentinelPODOrbitFile.getValidityStartFromFilenameUTC(filename);
                final ProductData.UTC utcEnd = SentinelPODOrbitFile.getValidityStopFromFilenameUTC(filename);
                if(utcStart != null && utcEnd != null) {
                    if(stateVectorTime >= utcStart.getMJD() && stateVectorTime < utcEnd.getMJD()) {
                        return file;
                    }
                }
            } catch (ParseException ignored) {
            }
        }
        return null;
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
    public Orbits.OrbitData getOrbitDataOld(final double utc) throws Exception {

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
     * Get orbit state vector for given time using polynomial fitting.
     *
     * @param utc The UTC in days.
     * @return The orbit state vector.
     * @throws Exception The exceptions.
     */
    public Orbits.OrbitData getOrbitData(final double utc) throws Exception {

        final int numVectors = osvList.size();
        final double t0 = osvList.get(0).utcMJD;
        final double tN = osvList.get(numVectors - 1).utcMJD;

        final int numVecPolyFit = 4;
        final int[] vectorIndices = new int[numVecPolyFit];

        final int vecIdx = (int)((utc - t0) / (tN - t0) * (numVectors - 1));
        if (vecIdx <= 0) {
            for (int i = 0; i < numVecPolyFit; i++) {
                vectorIndices[i] = i;
            }
        } else if (vecIdx >= numVectors - 2) {
            for (int i = 0; i < numVecPolyFit; i++) {
                vectorIndices[i] = numVectors - 4 + i;
            }
        } else {
            for (int i = 0; i < numVecPolyFit; i++) {
                vectorIndices[i] = vecIdx - 1 + i;
            }
        }

        double[] timeArray = new double[numVecPolyFit];
        double[] xPosArray = new double[numVecPolyFit];
        double[] yPosArray = new double[numVecPolyFit];
        double[] zPosArray = new double[numVecPolyFit];
        double[] xVelArray = new double[numVecPolyFit];
        double[] yVelArray = new double[numVecPolyFit];
        double[] zVelArray = new double[numVecPolyFit];

        for (int i = 0; i < numVecPolyFit; i++) {
            timeArray[i] = osvList.get(vectorIndices[i]).utcMJD - t0;
            xPosArray[i] = osvList.get(vectorIndices[i]).x;
            yPosArray[i] = osvList.get(vectorIndices[i]).y;
            zPosArray[i] = osvList.get(vectorIndices[i]).z;
            xVelArray[i] = osvList.get(vectorIndices[i]).vx;
            yVelArray[i] = osvList.get(vectorIndices[i]).vy;
            zVelArray[i] = osvList.get(vectorIndices[i]).vz;
        }

        final Matrix A = Maths.createVandermondeMatrix(timeArray, polyDegree);
        final double[] xPosCoeff = Maths.polyFit(A, xPosArray);
        final double[] yPosCoeff = Maths.polyFit(A, yPosArray);
        final double[] zPosCoeff = Maths.polyFit(A, zPosArray);
        final double[] xVelCoeff = Maths.polyFit(A, xVelArray);
        final double[] yVelCoeff = Maths.polyFit(A, yVelArray);
        final double[] zVelCoeff = Maths.polyFit(A, zVelArray);

        final Orbits.OrbitData orbitData = new Orbits.OrbitData();
        final double normalizedTime = utc - t0;
        orbitData.xPos = Maths.polyVal(normalizedTime, xPosCoeff);
        orbitData.yPos = Maths.polyVal(normalizedTime, yPosCoeff);
        orbitData.zPos = Maths.polyVal(normalizedTime, zPosCoeff);
        orbitData.xVel = Maths.polyVal(normalizedTime, xVelCoeff);
        orbitData.yVel = Maths.polyVal(normalizedTime, yVelCoeff);
        orbitData.zVel = Maths.polyVal(normalizedTime, zVelCoeff);

        return orbitData;
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

        OSV osv = new OSV(utc, x, y, z, vx, vy, vz);
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

    private static String convertUTC(String utc) {

        return utc.replace("UTC=","").replace("T"," ");
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

    private static String extractTimeFromFilename(final String filename, final int offset) {

        return filename.substring(offset,offset+15).replace("T","-");
    }

    public static ProductData.UTC getValidityStartFromFilenameUTC(String filename) throws ParseException {

        if (filename.substring(41,42).equals("V")) {

            String val = extractTimeFromFilename(filename, 42);
            return ProductData.UTC.parse(val, dateFormat);
        }
        return null;
    }

    public static ProductData.UTC getValidityStopFromFilenameUTC(String filename) throws ParseException {

        if (filename.substring(41,42).equals("V")) {

            String val = extractTimeFromFilename(filename, 58);
            return ProductData.UTC.parse(val, dateFormat);
        }
        return null;
    }

    public static String getValidityStartFromFilename(String filename) {

        if (filename.substring(41,42).equals("V")) {

            return extractUTCTimeFromFilename(filename, 42);
        }
        return null;
    }

    public static String getValidityStopFromFilename(String filename) {

        if (filename.substring(41,42).equals("V")) {

            return extractUTCTimeFromFilename(filename, 58);
        }
        return null;
    }

    public String getMissionFromHeader() {

        if (fixedHeader != null) {
            return fixedHeader.mission;
        }
        return null;
    }

    public String getFileTypeFromHeader() {

        if (fixedHeader != null) {
            return fixedHeader.fileType;
        }
        return null;
    }

    public String getValidityStartFromHeader() {

        if (fixedHeader != null) {
            return fixedHeader.validityStart;
        }
        return null;
    }

    public String getValidityStopFromHeader() {

        if (fixedHeader != null) {
            return fixedHeader.validityStop;
        }
        return null;
    }

    public static ProductData.UTC toUTC(final String str) throws ParseException {
        return ProductData.UTC.parse(convertUTC(str), orbitDateFormat);
    }
}