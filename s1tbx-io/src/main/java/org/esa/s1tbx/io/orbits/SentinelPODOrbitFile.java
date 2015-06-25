/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.io.orbits;

import Jama.Matrix;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.datamodel.DownloadableArchive;
import org.esa.snap.datamodel.Orbits;
import org.esa.snap.framework.datamodel.MetadataElement;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductData;
import org.esa.snap.framework.gpf.OperatorException;
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
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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

    public final static String RESTITUTED = "Sentinel Restituted";
    public final static String PRECISE = "Sentinel Precise";

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

    private final List<Orbits.OrbitVector> osvList = new ArrayList<>();

    public SentinelPODOrbitFile(final String orbitType, final MetadataElement absRoot,
                                final Product sourceProduct, final int polyDegree) throws Exception {
        super(orbitType, absRoot);
        this.polyDegree = polyDegree;
    }

    public File retrieveOrbitFile() throws Exception {
        final double stateVectorTime = absRoot.getAttributeUTC(AbstractMetadata.STATE_VECTOR_TIME).getMJD();
        final Calendar calendar = absRoot.getAttributeUTC(AbstractMetadata.STATE_VECTOR_TIME).getAsCalendar();
        final int year = calendar.get(Calendar.YEAR);
        final int month = calendar.get(Calendar.MONTH) + 1; // zero based

        this.orbitFile = findOrbitFile(stateVectorTime, year);

        if(orbitFile == null) {
            getRemoteFiles(year, month);
            this.orbitFile = findOrbitFile(stateVectorTime, year);
            if(orbitFile == null) {
                String timeStr = absRoot.getAttributeUTC(AbstractMetadata.STATE_VECTOR_TIME).format();
                final File destFolder = getDestFolder(year);
                throw new OperatorException("No valid orbit file found for " + timeStr + "\nOrbit files may be downloaded from https://qc.sentinel1.eo.esa.int/"
                                            +"\nand placed in "+destFolder.getAbsolutePath());
            }
        }

        if (!orbitFile.exists()) {
            throw new IOException("SentinelPODOrbitFile: Unable to find POD orbit file");
        }

        // read content of the orbit file
        readOrbitFile();

        checkOrbitFileValidity();

        return orbitFile;
    }

    private File getDestFolder(final int year) {
        final File orbitFileFolder;
        if(orbitType.startsWith(RESTITUTED)) {
            orbitFileFolder = new File(Settings.instance().get("OrbitFiles.sentinel1RESOrbitPath")+File.separator+year);
        } else {
            orbitFileFolder = new File(Settings.instance().get("OrbitFiles.sentinel1POEOrbitPath")+File.separator+year);
        }
        return orbitFileFolder;
    }

    private File findOrbitFile(final double stateVectorTime, final int year) {

        final String prefix;
        final File orbitFileFolder;
        if(orbitType.startsWith(RESTITUTED)) {
            prefix = "S1A_OPER_AUX_RESORB_OPOD_";
            orbitFileFolder = new File(Settings.instance().get("OrbitFiles.sentinel1RESOrbitPath")+File.separator+year);
        } else {
            prefix = "S1A_OPER_AUX_POEORB_OPOD_";
            orbitFileFolder = new File(Settings.instance().get("OrbitFiles.sentinel1POEOrbitPath")+File.separator+year);
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

    private void getRemoteFiles(final int year, final int month) throws Exception {

        final File localFolder;
        final URL remotePath;
        if(orbitType.startsWith(RESTITUTED)) {
            localFolder = new File(Settings.instance().get("OrbitFiles.sentinel1RESOrbitPath"), String.valueOf(year));
            remotePath = new URL(Settings.instance().getPath("OrbitFiles.sentinel1RESOrbit_remotePath"));
        } else {
            localFolder = new File(Settings.instance().get("OrbitFiles.sentinel1POEOrbitPath"), String.valueOf(year));
            remotePath = new URL(Settings.instance().getPath("OrbitFiles.sentinel1POEOrbit_remotePath"));
        }

        final File localFile = new File(localFolder, year + "-" + month + ".zip");
        final DownloadableArchive archive = new DownloadableArchive(localFile, remotePath);
        archive.getContentFiles();
    }

    /**
     * Check if product acquisition time is within the validity period of the orbit file.
     * @throws Exception
     */
    private void checkOrbitFileValidity() throws Exception {

        final double stateVectorTime = absRoot.getAttributeUTC(AbstractMetadata.STATE_VECTOR_TIME).getMJD();
        final String validityStartTimeStr = getValidityStartFromHeader();
        final String validityStopTimeStr = getValidityStopFromHeader();
        final double validityStartTimeMJD = SentinelPODOrbitFile.toUTC(validityStartTimeStr).getMJD();
        final double validityStopTimeMJD = SentinelPODOrbitFile.toUTC(validityStopTimeStr).getMJD();

        if (stateVectorTime < validityStartTimeMJD || stateVectorTime > validityStopTimeMJD) {
            throw new OperatorException("Product acquisition time is not within the validity period of the orbit");
        }
    }

    public Orbits.OrbitVector[] getOrbitData(final double startUTC, final double endUTC) throws Exception {

        final Orbits.OrbitVector startOSV = new Orbits.OrbitVector(startUTC);
        int startIdx = Collections.binarySearch(osvList, startOSV, new Orbits.OrbitComparator());

        if (startIdx < 0) {
            final int insertionPt = -(startIdx+1);
            if (insertionPt == osvList.size()) {
                startIdx = insertionPt - 1;
            } else if (insertionPt <= 0) {
                startIdx = 0;
            } else {
                startIdx = insertionPt - 1;
            }
        }

        final Orbits.OrbitVector endOSV = new Orbits.OrbitVector(endUTC);
        int endIdx = Collections.binarySearch(osvList, endOSV, new Orbits.OrbitComparator());

        if (endIdx < 0) {
            final int insertionPt = -(endIdx+1);
            if (insertionPt == osvList.size()) {
                endIdx = insertionPt - 1;
            } else if (insertionPt == 0) {
                endIdx = 0;
            } else {
                endIdx = insertionPt;
            }
        }

        startIdx -= 3;
        endIdx += 3;

        final int numOSV = endIdx - startIdx + 1;
        final Orbits.OrbitVector[] orbitDataList = new Orbits.OrbitVector[numOSV];
        int idx = startIdx;
        for (int i = 0; i < numOSV; i++) {
            orbitDataList[i] = osvList.get(idx);
            idx++;
        }

        return orbitDataList;
    }

    /**
     * Get orbit state vector for given time using polynomial fitting.
     *
     * @param utc The UTC in days.
     * @return The orbit state vector.
     * @throws Exception The exceptions.
     */
    public Orbits.OrbitVector getOrbitData(final double utc) throws Exception {

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
            xPosArray[i] = osvList.get(vectorIndices[i]).xPos;
            yPosArray[i] = osvList.get(vectorIndices[i]).yPos;
            zPosArray[i] = osvList.get(vectorIndices[i]).zPos;
            xVelArray[i] = osvList.get(vectorIndices[i]).xVel;
            yVelArray[i] = osvList.get(vectorIndices[i]).yVel;
            zVelArray[i] = osvList.get(vectorIndices[i]).zVel;
        }

        final Matrix A = Maths.createVandermondeMatrix(timeArray, polyDegree);
        final double[] xPosCoeff = Maths.polyFit(A, xPosArray);
        final double[] yPosCoeff = Maths.polyFit(A, yPosArray);
        final double[] zPosCoeff = Maths.polyFit(A, zPosArray);
        final double[] xVelCoeff = Maths.polyFit(A, xVelArray);
        final double[] yVelCoeff = Maths.polyFit(A, yVelArray);
        final double[] zVelCoeff = Maths.polyFit(A, zVelArray);

        final double normalizedTime = utc - t0;

        return new Orbits.OrbitVector(utc,
                Maths.polyVal(normalizedTime, xPosCoeff),
                Maths.polyVal(normalizedTime, yPosCoeff),
                Maths.polyVal(normalizedTime, zPosCoeff),
                Maths.polyVal(normalizedTime, xVelCoeff),
                Maths.polyVal(normalizedTime, yVelCoeff),
                Maths.polyVal(normalizedTime, zVelCoeff));
    }

    private void readOrbitFile() throws Exception {

        try {
            final DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();

            final Document doc;
            if(orbitFile.getName().toLowerCase().endsWith(".zip")) {
                final ZipFile productZip = new ZipFile(orbitFile, ZipFile.OPEN_READ);
                final Enumeration<? extends ZipEntry> entries = productZip.entries();
                final ZipEntry zipEntry = entries.nextElement();

                doc = documentBuilder.parse(productZip.getInputStream(zipEntry));
            } else {
                doc = documentBuilder.parse(orbitFile);
            }

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

        Collections.sort(osvList, new Orbits.OrbitComparator());

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

        final double utcTime =  toUTC(utc).getMJD();

        osvList.add(new Orbits.OrbitVector(utcTime, x, y, z, vx, vy, vz));
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
