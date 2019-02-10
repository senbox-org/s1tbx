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
package org.esa.s1tbx.io.orbits.sentinel1;

import Jama.Matrix;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.esa.s1tbx.io.orbits.BaseOrbitFile;
import org.esa.s1tbx.io.orbits.OrbitFile;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Orbits;
import org.esa.snap.engine_utilities.download.DownloadableArchive;
import org.esa.snap.engine_utilities.download.DownloadableContentImpl;
import org.esa.snap.engine_utilities.util.Maths;
import org.esa.snap.engine_utilities.util.Settings;
import org.esa.snap.engine_utilities.util.ZipUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.TimeUnit;
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

    private FixedHeader fixedHeader = null;

    private List<Orbits.OrbitVector> osvList = new ArrayList<>();

    private static LoadingCache<File, List<Orbits.OrbitVector>> cache;

    public SentinelPODOrbitFile(final MetadataElement absRoot, final int polyDegree) throws Exception {
        super(absRoot);
        this.polyDegree = polyDegree;
    }

    public String[] getAvailableOrbitTypes() {
        return new String[]{PRECISE, RESTITUTED};
    }

    public File retrieveOrbitFile(final String orbitType) throws Exception {
        final double stateVectorTime = absRoot.getAttributeUTC(AbstractMetadata.STATE_VECTOR_TIME).getMJD();
        final Calendar calendar = absRoot.getAttributeUTC(AbstractMetadata.STATE_VECTOR_TIME).getAsCalendar();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1; // zero based
        final int day = calendar.get(Calendar.DAY_OF_MONTH);
        final int hour = calendar.get(Calendar.HOUR_OF_DAY);
        final int minute = calendar.get(Calendar.MINUTE);
        final int second = calendar.get(Calendar.SECOND);
        final String missionPrefix = getMissionPrefix(absRoot);

        orbitFile = findOrbitFile(missionPrefix, orbitType, stateVectorTime, year, month);

        if (orbitFile == null) {
            orbitFile = downloadFromQCRestAPI(missionPrefix, orbitType, year, month, day, hour, minute, second, stateVectorTime);
        }
        if (orbitFile == null) {
            //orbitFile = downloadArchive(missionPrefix, orbitType, year, month, day, stateVectorTime);
            orbitFile = downloadFromStepAuxdata(missionPrefix, orbitType, year, month, day, stateVectorTime);
        }
        //if (orbitFile == null) {
            //orbitFile = downloadFromQCWebsite(missionPrefix, orbitType, year, month, day, stateVectorTime);
        //}

        if (orbitFile == null) {
            String timeStr = absRoot.getAttributeUTC(AbstractMetadata.STATE_VECTOR_TIME).format();
            final File destFolder = getDestFolder(missionPrefix, orbitType, year, month);
            throw new IOException("No valid orbit file found for " + timeStr + "\nOrbit files may be downloaded from https://qc.sentinel1.eo.esa.int/"
                    + "\nand placed in " + destFolder.getAbsolutePath());
        }

        if (!orbitFile.exists()) {
            throw new IOException("SentinelPODOrbitFile: Unable to find POD orbit file");
        }

        // read content of the orbit file
        readOrbitFile();

        return orbitFile;
    }

    private static String getMissionPrefix(final MetadataElement absRoot) {
        final String mission = absRoot.getAttributeString(AbstractMetadata.MISSION);
        return "S1" + mission.substring(mission.length() - 1);
    }

    private static File downloadArchive(final String missionPrefix, final String orbitType,
                                        int year, int month, final int day,
                                        final double stateVectorTime) throws Exception {
        getRemoteFiles(missionPrefix, orbitType, year, month);
        File orbitFile = findOrbitFile(missionPrefix, orbitType, stateVectorTime, year, month);
        if (orbitFile == null) {
            NewDate newDate = getNeighouringMonth(year, month, day);
            getRemoteFiles(missionPrefix, orbitType, newDate.year, newDate.month);
            orbitFile = findOrbitFile(missionPrefix, orbitType, stateVectorTime, newDate.year, newDate.month);
        }
        return orbitFile;
    }

    private static File downloadFromQCRestAPI(final String missionPrefix, final String orbitType,
                                              int year, int month, final int day,
                                              final int hour, final int minute, final int second,
                                              final double stateVectorTime) throws Exception {
        final String orbProductType = orbitType.equals(RESTITUTED) ? "AUX_RESORB" : "AUX_POEORB";
        final String date = year+"-"+month+"-"+day;

        String endPoint = "https://qc.sentinel1.eo.esa.int/api/v1/?product_type="+orbProductType;
        endPoint += "&validity_stop__gt="+date+"T23:59:59";
        endPoint += "&validity_start__lt="+date+"T"+hour+":"+minute+":"+second;
        endPoint += "&ordering=-creation_date&page_size=1";

        try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
            final HttpGet httpGet = new HttpGet(endPoint);

            final StringBuilder outputBuilder = new StringBuilder();
            try (final CloseableHttpResponse response = httpClient.execute(httpGet)) {
                final int status = response.getStatusLine().getStatusCode();

                try (final BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))) {
                    String line;
                    while ((line = rd.readLine()) != null) {
                        outputBuilder.append(line);
                    }
                }
                response.close();

                if (status == 200) {
                    final JSONParser parser = new JSONParser();
                    JSONObject json = (JSONObject) parser.parse(outputBuilder.toString());
                    if(json.containsKey("results")) {
                        JSONArray results = (JSONArray)json.get("results");
                        if(!results.isEmpty()) {
                            JSONObject result = (JSONObject)results.get(0);
                            if(result.containsKey("remote_url")) {
                                String remoteURL = (String)result.get("remote_url");

                                getQCFile(missionPrefix, orbitType, year, month, remoteURL);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("Exception calling QC Rest API:  " + e.getMessage());
                throw e;
            }
        }

        File orbitFile = findOrbitFile(missionPrefix, orbitType, stateVectorTime, year, month);
        return orbitFile;
    }

    private static File downloadFromStepAuxdata(final String missionPrefix, final String orbitType,
                                              int year, int month, final int day,
                                              final double stateVectorTime) throws Exception {
        getStepAuxdataFiles(missionPrefix, orbitType, year, month, stateVectorTime);
        File orbitFile = findOrbitFile(missionPrefix, orbitType, stateVectorTime, year, month);
        if (orbitFile == null) {
            NewDate newDate = getNeighouringMonth(year, month, day);
            getStepAuxdataFiles(missionPrefix, orbitType, newDate.year, newDate.month, stateVectorTime);
            orbitFile = findOrbitFile(missionPrefix, orbitType, stateVectorTime, newDate.year, newDate.month);
        }
        return orbitFile;
    }

    private static File downloadFromQCWebsite(final String missionPrefix, final String orbitType,
                                              int year, int month, final int day,
                                              final double stateVectorTime) throws Exception {
        getQCFiles(missionPrefix, orbitType, year, month, stateVectorTime);
        File orbitFile = findOrbitFile(missionPrefix, orbitType, stateVectorTime, year, month);
        if (orbitFile == null) {
            NewDate newDate = getNeighouringMonth(year, month, day);
            getQCFiles(missionPrefix, orbitType, newDate.year, newDate.month, stateVectorTime);
            orbitFile = findOrbitFile(missionPrefix, orbitType, stateVectorTime, newDate.year, newDate.month);
        }
        return orbitFile;
    }

    private static class NewDate {
        final int month;
        final int year;
        NewDate(int year, int month) {
            this.year = year;
            this.month = month;
        }
    }

    private static NewDate getNeighouringMonth(int year, int month, final int day) {
        if (day < 15) {
            month--;
            if (month < 1) {
                month = 12;
                year--;
            }
        } else {
            month++;
            if (month > 12) {
                month = 1;
                year++;
            }
        }
        return new NewDate(year, month);
    }

    static File getDestFolder(final String missionPrefix, final String orbitType, final int year, final int month) {
        final String prefOrbitPath;
        if (orbitType.startsWith(RESTITUTED)) {
            prefOrbitPath = Settings.getPath("OrbitFiles.sentinel1RESOrbitPath");
        } else {
            prefOrbitPath = Settings.getPath("OrbitFiles.sentinel1POEOrbitPath");
        }
        final File destFolder = new File(prefOrbitPath +
                File.separator + missionPrefix +
                File.separator + year +
                File.separator + StringUtils.padNum(month, 2, '0'));

        if(month < 10) {
            File oldFolder = new File(prefOrbitPath +
                    File.separator + missionPrefix +
                    File.separator + year +
                    File.separator + month);
            if(oldFolder.exists()) {
                // rename
                oldFolder.renameTo(destFolder);
            }
        }
        destFolder.mkdirs();

        return destFolder;
    }

    private static File findOrbitFile(final String missionPrefix, final String orbitType,
                                      final double stateVectorTime, final int year, final int month) {
        final String prefix;
        if (orbitType.startsWith(RESTITUTED)) {
            prefix = missionPrefix + "_OPER_AUX_RESORB_OPOD_";
        } else {
            prefix = missionPrefix + "_OPER_AUX_POEORB_OPOD_";
        }
        final File orbitFileFolder = getDestFolder(missionPrefix, orbitType, year, month);

        if (!orbitFileFolder.exists())
            return null;
        final File[] files = orbitFileFolder.listFiles(new S1OrbitFileFilter(prefix));
        if (files == null || files.length == 0)
            return null;

        for (File file : files) {
            if (isWithinRange(file.getName(), stateVectorTime)) {
                return file;
            }
        }
        return null;
    }

    private static void getRemoteFiles(final String missionPrefix, final String orbitType,
                                       final int year, final int month) throws Exception {
        final URL remotePath;
        if (orbitType.startsWith(RESTITUTED)) {
            remotePath = new URL(Settings.getPath("OrbitFiles.sentinel1RESOrbit_remotePath"));
        } else {
            remotePath = new URL(Settings.getPath("OrbitFiles.sentinel1POEOrbit_remotePath"));
        }
        final File orbitFileFolder = getDestFolder(missionPrefix, orbitType, year, month);
        final File localFile = new File(orbitFileFolder, year + "-" + month + ".zip");

        try {
            final DownloadableArchive archive = new DownloadableArchive(localFile, remotePath);
            archive.getContentFiles();
        } catch (Exception e) {
            if (localFile.exists()) {
                localFile.delete();
                final DownloadableArchive archive = new DownloadableArchive(localFile, remotePath);
                archive.getContentFiles();
            }
        }
    }

    private static void getStepAuxdataFiles(final String missionPrefix, final String orbitType, int year, int month,
                                   final double stateVectorTime) throws Exception {

        final File localFolder = getDestFolder(missionPrefix, orbitType, year, month);

        final StepAuxdataScraper step = new StepAuxdataScraper(orbitType);

        final String[] orbitFiles = step.getFileURLs(missionPrefix, year, month);
        final URL remotePath = new URL(step.getRemoteURL());
        final SSLUtil ssl = new SSLUtil();
        ssl.disableSSLCertificateCheck();

        for (String file : orbitFiles) {
            if (isWithinRange(file, stateVectorTime)) {
                final File localFile = new File(localFolder, file);
                DownloadableContentImpl.getRemoteHttpFile(remotePath, localFile);
                break;
            }
        }

        ssl.enableSSLCertificateCheck();
    }

    private static void getQCFile(final String missionPrefix, final String orbitType, int year, int month,
                                  final String remoteURL) throws Exception {

        final File localFolder = getDestFolder(missionPrefix, orbitType, year, month);
        final int lastSlash = remoteURL.lastIndexOf("/")+1;
        final String remoteFolder = remoteURL.substring(0, lastSlash);
        final String name = remoteURL.substring(lastSlash);
        final URL remotePath = new URL(remoteFolder);

        final SSLUtil ssl = new SSLUtil();
        ssl.disableSSLCertificateCheck();

        final File localFile = new File(localFolder, name);
        DownloadableContentImpl.getRemoteHttpFile(remotePath, localFile);

        ssl.enableSSLCertificateCheck();

        if (localFile.exists()) {
            final File localZipFile = FileUtils.exchangeExtension(localFile, ".EOF.zip");
            ZipUtils.zipFile(localFile, localZipFile);
            localFile.delete();
        }
    }

    private static void getQCFiles(final String missionPrefix, final String orbitType, int year, int month,
                                   final double stateVectorTime) throws Exception {

        final File localFolder = getDestFolder(missionPrefix, orbitType, year, month);

        final QCScraper qc = new QCScraper(orbitType);

        final String[] orbitFiles = qc.getFileURLs(missionPrefix, year, month);
        final URL remotePath = new URL(qc.getRemoteURL());
        final SSLUtil ssl = new SSLUtil();

        ssl.disableSSLCertificateCheck();

        for (String file : orbitFiles) {
            if (isWithinRange(file, stateVectorTime)) {
                final File localFile = new File(localFolder, file);
                DownloadableContentImpl.getRemoteHttpFile(remotePath, localFile);
                if (localFile.exists()) {
                    final File localZipFile = FileUtils.exchangeExtension(localFile, ".EOF.zip");
                    ZipUtils.zipFile(localFile, localZipFile);
                    localFile.delete();
                }
            }
        }

        ssl.enableSSLCertificateCheck();
    }

    /**
     * Check if product acquisition time is within the validity period of the orbit file.
     *
     * @throws Exception
     */
    private void checkOrbitFileValidity() throws Exception {

        final double stateVectorTime = absRoot.getAttributeUTC(AbstractMetadata.STATE_VECTOR_TIME).getMJD();
        final String validityStartTimeStr = getValidityStartFromHeader();
        final String validityStopTimeStr = getValidityStopFromHeader();
        final double validityStartTimeMJD = SentinelPODOrbitFile.toUTC(validityStartTimeStr).getMJD();
        final double validityStopTimeMJD = SentinelPODOrbitFile.toUTC(validityStopTimeStr).getMJD();

        if (stateVectorTime < validityStartTimeMJD || stateVectorTime > validityStopTimeMJD) {
            throw new IOException("Product acquisition time is not within the validity period of the orbit");
        }
    }

    public Orbits.OrbitVector[] getOrbitData(final double startUTC, final double endUTC) {

        final Orbits.OrbitVector startOSV = new Orbits.OrbitVector(startUTC);
        int startIdx = Collections.binarySearch(osvList, startOSV, new Orbits.OrbitComparator());

        if (startIdx < 0) {
            final int insertionPt = -(startIdx + 1);
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
            final int insertionPt = -(endIdx + 1);
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
    public Orbits.OrbitVector getOrbitData(final double utc) {

        final int numVectors = osvList.size();
        final double t0 = osvList.get(0).utcMJD;
        final double tN = osvList.get(numVectors - 1).utcMJD;

        final int numVecPolyFit = polyDegree + 1; //4;
        final int halfNumVecPolyFit = numVecPolyFit / 2;
        final int[] vectorIndices = new int[numVecPolyFit];

        final int vecIdx = (int) ((utc - t0) / (tN - t0) * (numVectors - 1));
        if (vecIdx <= halfNumVecPolyFit - 1) {
            for (int i = 0; i < numVecPolyFit; i++) {
                vectorIndices[i] = i;
            }
        } else if (vecIdx >= numVectors - halfNumVecPolyFit) {
            for (int i = 0; i < numVecPolyFit; i++) {
                vectorIndices[i] = numVectors - numVecPolyFit + i;
            }
        } else {
            for (int i = 0; i < numVecPolyFit; i++) {
                vectorIndices[i] = vecIdx - halfNumVecPolyFit + 1 + i;
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

        List<Orbits.OrbitVector> cachedOSVList = getCache().get(orbitFile);
        if(cachedOSVList != null && !cachedOSVList.isEmpty()) {
            osvList = cachedOSVList;
            return;
        }

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

        // Don't need anything from Variable_Header.

        if (listOfOSVsNode != null) {

            osvList = readOSVList(listOfOSVsNode);
        }

        checkOrbitFileValidity();

        getCache().put(orbitFile, osvList);
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

    static String getMissionIDFromFilename(String filename) {

        return filename.substring(0, 3);
    }

    static String getFileTypeFromFilename(String filename) {

        return filename.substring(9, 19);
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

        if (filename.substring(41, 42).equals("V")) {

            String val = extractTimeFromFilename(filename, 42);
            return ProductData.UTC.parse(val, dateFormat);
        }
        return null;
    }

    static ProductData.UTC getValidityStopFromFilenameUTC(String filename) throws ParseException {

        if (filename.substring(41, 42).equals("V")) {

            String val = extractTimeFromFilename(filename, 58);
            return ProductData.UTC.parse(val, dateFormat);
        }
        return null;
    }

    static String getValidityStartFromFilename(String filename) {

        if (filename.substring(41, 42).equals("V")) {

            return extractUTCTimeFromFilename(filename, 42);
        }
        return null;
    }

    static String getValidityStopFromFilename(String filename) {

        if (filename.substring(41, 42).equals("V")) {

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

    private static class S1OrbitFileFilter implements FilenameFilter {
        private final String prefix;

        S1OrbitFileFilter(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public boolean accept(File dir, String name) {
            name = name.toUpperCase();
            return (name.endsWith(".ZIP") || name.endsWith(".EOF")) && name.startsWith(prefix);
        }
    }

    private static boolean isWithinRange(final String filename, final double stateVectorTime) {
        try {
            final ProductData.UTC utcStart = SentinelPODOrbitFile.getValidityStartFromFilenameUTC(filename);
            final ProductData.UTC utcEnd = SentinelPODOrbitFile.getValidityStopFromFilenameUTC(filename);
            if (utcStart != null && utcEnd != null) {
                return stateVectorTime >= utcStart.getMJD() && stateVectorTime < utcEnd.getMJD();
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private LoadingCache<File, List<Orbits.OrbitVector>> getCache() {
        if(cache == null) {
            cache = createCache();
        }
        return cache;
    }

    private static LoadingCache<File, List<Orbits.OrbitVector>> createCache() {
        return CacheBuilder.newBuilder().maximumSize(6).initialCapacity(6)
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .build(new CacheLoader<File, List<Orbits.OrbitVector>>() {
                           @Override
                           public List<Orbits.OrbitVector> load(File key) throws Exception {
                               return new ArrayList<>();
                           }
                       }
                );
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
