/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.io.orbits.k5;

import Jama.Matrix;
import org.esa.s1tbx.io.orbits.BaseOrbitFile;
import org.esa.s1tbx.io.orbits.OrbitFile;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Orbits;
import org.esa.snap.engine_utilities.util.Maths;

import java.io.*;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Kompsat-5 POD Orbit File
 */
public class K5OrbitFile extends BaseOrbitFile implements OrbitFile {

    public final static String PRECISE = "Kompsat5 Precise";

    private final static String remoteURL = "ftp://aopod-ftp.kasi.re.kr/kompsat5rt/level1b/leoOrb/";

    private final int polyDegree;
    private final DateFormat orbitDateFormat = ProductData.UTC.createDateFormat("yyyy-MM-dd HH:mm:ss");

    private List<Orbits.OrbitVector> osvList;

    public K5OrbitFile(final MetadataElement absRoot, final int polyDegree) {
        super(absRoot);
        this.polyDegree = polyDegree;
    }

    public String[] getAvailableOrbitTypes() {
        return new String[]{PRECISE};
    }

    public File retrieveOrbitFile(final String orbitType) throws Exception {
        final double stateVectorTime = absRoot.getAttributeUTC(AbstractMetadata.STATE_VECTOR_TIME).getMJD();
        final Calendar calendar = absRoot.getAttributeUTC(AbstractMetadata.STATE_VECTOR_TIME).getAsCalendar();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1; // zero based
        final int day = calendar.get(Calendar.DAY_OF_MONTH);

        orbitFile = findOrbitFile(orbitType, stateVectorTime, year, month);

        if (orbitFile == null) {
            String timeStr = absRoot.getAttributeUTC(AbstractMetadata.STATE_VECTOR_TIME).format();
            final File destFolder = getDestFolder(orbitType, year, month);
            throw new IOException("No valid orbit file found for " + timeStr +
                                                "\nOrbit files may be downloaded from " + remoteURL
                                                + "\nand placed in " + destFolder.getAbsolutePath());
        }

        // read content of the orbit file
        osvList = readOrbitFile(orbitFile);

        return orbitFile;
    }

    private static File getDestFolder(final String orbitType, final int year, final int month) {
        //final String prefOrbitPath = Settings.getPath("OrbitFiles.k5POEOrbitPath");

        return SystemUtils.getAuxDataPath().resolve("Orbits").resolve("K5").resolve("POEORB").toFile();
    }

    private File findOrbitFile(final String orbitType,
                                      final double stateVectorTime, final int year, final int month) throws Exception {
        final String prefix = "LEOORB_" + year;

        final File orbitFileFolder = getDestFolder(orbitType, year, month);

        if (!orbitFileFolder.exists()) {
            orbitFileFolder.mkdirs();
        }
        final File[] files = orbitFileFolder.listFiles(new K5OrbitFileFilter(prefix));
        if (files == null || files.length == 0)
            return null;

        for (File file : files) {
            List<Orbits.OrbitVector> orbitVectors = readOrbitFile(file);
            if (isWithinRange(orbitVectors, stateVectorTime)) {
                return file;
            }
        }
        return null;
    }

    private static boolean isWithinRange(final List<Orbits.OrbitVector> orbitVectors, final double stateVectorTime) {
        return orbitVectors.get(0).utcMJD <= stateVectorTime &&
                orbitVectors.get(orbitVectors.size()-1).utcMJD > stateVectorTime;
    }

    public Orbits.OrbitVector[] getOrbitData(final double startUTC, final double endUTC) throws Exception {

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
    public Orbits.OrbitVector getOrbitData(final double utc) throws Exception {

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

    private List<Orbits.OrbitVector> readOrbitFile(final File file) throws Exception {
        final List<Orbits.OrbitVector> osvList = new ArrayList<>();
        try (final BufferedReader lineReader = new BufferedReader(new FileReader(file))) {

            for (String line = lineReader.readLine(); line != null; line = lineReader.readLine()) {
                if (line.startsWith("*")) {
                    ProductData.UTC time = parseTime(line);

                    Orbits.OrbitVector orbitVector = new Orbits.OrbitVector(time.getMJD());
                    line = lineReader.readLine();
                    parsePosition(line, orbitVector);
                    line = lineReader.readLine();
                    parseVelocity(line, orbitVector);

                    osvList.add(orbitVector);
                }
            }
        }
        return osvList;
    }

    private ProductData.UTC parseTime(final String line) throws Exception {
        final StringTokenizer tokenizer = new StringTokenizer(line, " ");
        tokenizer.nextToken();

        int year = Integer.parseInt(tokenizer.nextToken());
        int month = Integer.parseInt(tokenizer.nextToken());
        int day = Integer.parseInt(tokenizer.nextToken());
        int hour = Integer.parseInt(tokenizer.nextToken());
        int minute = Integer.parseInt(tokenizer.nextToken());

        String date = "" + year +'-'+ month +'-'+ day +' '+ hour +':'+ minute +':' + 0;
        return ProductData.UTC.parse(date, orbitDateFormat);
    }

    private static void parsePosition(final String line, final Orbits.OrbitVector orbitVector) {
        final StringTokenizer tokenizer = new StringTokenizer(line, " ");
        tokenizer.nextToken();

        orbitVector.xPos = Double.parseDouble(tokenizer.nextToken())*1000;
        orbitVector.yPos = Double.parseDouble(tokenizer.nextToken())*1000;
        orbitVector.zPos = Double.parseDouble(tokenizer.nextToken())*1000;
    }

    private static void parseVelocity(final String line, final Orbits.OrbitVector orbitVector) {
        final StringTokenizer tokenizer = new StringTokenizer(line, " ");
        tokenizer.nextToken();

        orbitVector.xVel = Double.parseDouble(tokenizer.nextToken())/10.0;
        orbitVector.yVel = Double.parseDouble(tokenizer.nextToken())/10.0;
        orbitVector.zVel = Double.parseDouble(tokenizer.nextToken())/10.0;
    }

    private static class K5OrbitFileFilter implements FilenameFilter {
        private final String prefix;

        public K5OrbitFileFilter(final String prefix) {
            this.prefix = prefix;
        }

        @Override
        public boolean accept(File dir, String name) {
            name = name.toUpperCase();
            return (name.endsWith(".ZIP") || name.endsWith("SP3")) && name.startsWith(prefix);
        }
    }
}
