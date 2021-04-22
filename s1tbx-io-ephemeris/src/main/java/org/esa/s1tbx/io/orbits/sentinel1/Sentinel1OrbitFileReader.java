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
import org.esa.snap.core.dataop.downloadable.XMLSupport;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.datamodel.Orbits;
import org.jdom2.Document;
import org.jdom2.Element;

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

        final Document doc;
        if (orbitFile.getName().toLowerCase().endsWith(".zip")) {
            final ZipFile productZip = new ZipFile(orbitFile, ZipFile.OPEN_READ);
            final Enumeration<? extends ZipEntry> entries = productZip.entries();
            final ZipEntry zipEntry = entries.nextElement();

            doc = XMLSupport.LoadXML(productZip.getInputStream(zipEntry));
        } else {
            doc = XMLSupport.LoadXML(orbitFile.getPath());
        }

        final Element earthExplorer = doc.getRootElement();
        final Element earthExplorerHeader = earthExplorer.getChild("Earth_Explorer_Header");
        if(earthExplorerHeader != null) {
            final Element fixedHeaderElem = earthExplorerHeader.getChild("Fixed_Header");
            if(fixedHeaderElem != null) {
                fixedHeader = readFixedHeader(fixedHeaderElem);
            }
        }

        final Element dataBlock = earthExplorer.getChild("Data_Block");
        if(dataBlock != null) {
            final Element listOfOSVs = dataBlock.getChild("List_of_OSVs");
            osvList = readOSVList(listOfOSVs);
        }
    }

    private static FixedHeader readFixedHeader(final Element fixedHeaderElem) {

        String mission = fixedHeaderElem.getChild("Mission").getText();
        String fileType = fixedHeaderElem.getChild("File_Type").getText();

        final Element validityPeriodElem = fixedHeaderElem.getChild("Validity_Period");
        String validityStart = validityPeriodElem.getChild("Validity_Start").getText();
        String validityStop = validityPeriodElem.getChild("Validity_Stop").getText();

        final Element sourceElem = fixedHeaderElem.getChild("Source");
        String version = sourceElem.getChild("Creator_Version").getText();

        return new FixedHeader(mission, fileType, validityStart, validityStop, version);
    }

    private static List<Orbits.OrbitVector> readOSVList(final Element listOfOSVsNode) throws Exception {

        final int count = Integer.parseInt(listOfOSVsNode.getAttributeValue("count"));
        final List<Orbits.OrbitVector> osvList = new ArrayList<>();

        int osvCnt = 0;
        final List<Element> osvElemList = listOfOSVsNode.getChildren("OSV");
        for(Element osvElem : osvElemList) {
            osvList.add(readOneOSV(osvElem));
            osvCnt++;
        }

        osvList.sort(new Orbits.OrbitComparator());

        if (count != osvCnt) {
            SystemUtils.LOG.warning("SentinelPODOrbitFile.readOSVList: WARNING List_of_OSVs count = " + count + " but found only " + osvCnt + " OSV");
        }

        return osvList;
    }

    private static Orbits.OrbitVector readOneOSV(final Element osvNode) throws Exception {

        final String utc = osvNode.getChild("UTC").getText();
        double x = Double.parseDouble(osvNode.getChild("X").getText());
        double y = Double.parseDouble(osvNode.getChild("Y").getText());
        double z = Double.parseDouble(osvNode.getChild("Z").getText());
        double vx = Double.parseDouble(osvNode.getChild("VX").getText());
        double vy = Double.parseDouble(osvNode.getChild("VY").getText());
        double vz = Double.parseDouble(osvNode.getChild("VZ").getText());

        final double utcTime = toUTC(utc).getMJD();

        return new Orbits.OrbitVector(utcTime, x, y, z, vx, vy, vz);
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

    public String getFileVersion() {
        return fixedHeader.version;
    }

    private static final class FixedHeader {

        private final String mission;
        private final String fileType;
        private final String validityStart;
        private final String validityStop;
        private final String version;

        FixedHeader(final String mission, final String fileType,
                    final String validityStart, final String validityStop, final String version) {

            this.mission = mission;
            this.fileType = fileType;
            this.validityStart = validityStart;
            this.validityStop = validityStop;
            this.version = version;
        }
    }
}
