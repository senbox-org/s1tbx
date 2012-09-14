/*
 * Copyright (C) 2012 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dataio;

import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.util.io.FileUtils;
import org.esa.nest.util.MathUtils;
import org.esa.nest.util.ResourceUtils;
import org.esa.nest.util.ZipUtils;

import java.io.*;
import java.util.Arrays;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: lveci
 * Date: Feb 26, 2008
 * To change this template use File | Settings | File Templates.
 */
public final class PrareOrbitReader {

    private DataInputStream in = null;

    private DataSetIdentificationRecord dataSetIdentificationRecord = null;
    private DataHeaderRecord dataHeaderRecord = null;
    private QualityParameterRecord[] qualityParameterRecords = null;

    private OrbitVector[] orbitVectors = null;
    private double[] recordTimes = null;
    private int numOfTrajectoryRecords;
    private int numOfQualityParameterRecords;

    private static final int sizeOfDataSetIdentificationRecord = 130;
    private static final int sizeOfDataHeaderRecord = 130;
    private static final int sizeOfTrajectoryRecord = 130;
    private static final int sizeOfQualityParameterRecord = 130;
    private static final int maxNumOfQualityParameterRecords = 20;

    private static final double millimeterToMeter = 0.001;
    private static final double microMeterToMeter = 0.000001;
    private static final double microSecondToSecond = 0.000001;
    private static final double secondToDay = 1.0 / (24*3600);

    /**
     * Read Data Set Identification Record and Data Header Record from the orbit file.
     * @param file The orbit file.
     * @throws IOException The exceptions.
     */
    public void readOrbitHeader(File file) throws IOException {

        final BufferedReader reader = getBufferedReader(file);

        readDataSetIdentificationRecord(reader);

        readDataHeaderRecord(reader);

        reader.close();
    }

    /**
     * Get buffered reader for given file ASCII.
     * @param file The file.
     * @return The reader.
     * @throws IOException if can't be read
     */
    private static BufferedReader getBufferedReader(final File file) throws IOException {

        String filePath = file.getAbsolutePath();
        if(ZipUtils.isZipped(file))  {
            try {
                // check if previously unzipped
                final File tempFolder = ResourceUtils.getApplicationUserTempDataDir();
                final File prevFile = new File(tempFolder, FileUtils.getFilenameWithoutExtension(file));
                if(prevFile.exists()) {
                    filePath = prevFile.getAbsolutePath();   
                } else {
                    // open zip
                    final File[] files = ZipUtils.unzipToFolder(file, tempFolder);
                    filePath = files[0].getAbsolutePath();
                }
            } catch(Exception e) {
                throw new IOException(e.getMessage() +": "+ file.getAbsolutePath());
            }
        }

        FileInputStream stream;
        try {
            stream = new FileInputStream(filePath);
        } catch(FileNotFoundException e) {
            throw new OperatorException("File not found: " + filePath);
        }

        return new BufferedReader(new InputStreamReader(stream));
    }

    /**
     * Read Data Set Identification Record.
     * @param reader The buffered reeader.
     * @throws IOException The exceptions.
     */
    private void readDataSetIdentificationRecord(BufferedReader reader) throws IOException {

        final char[] recKey = new char[6];
        final char[] prodID = new char[15];
        final char[] datTyp = new char[6];

        reader.read(recKey, 0, 6);
        reader.read(prodID, 0, 15);
        reader.read(datTyp, 0, 6);
        reader.skip(103);

        dataSetIdentificationRecord = new DataSetIdentificationRecord();
        dataSetIdentificationRecord.recKey = new String(recKey);
        dataSetIdentificationRecord.prodID = new String(prodID);
        dataSetIdentificationRecord.datTyp = new String(datTyp);
    }

    /**
     * Read Data Header Record.
     * @param reader The buffered reeader.
     * @throws IOException The exceptions.
     */
    private void readDataHeaderRecord(BufferedReader reader) throws IOException {

        final char[] recKey = new char[6];
        final char[] start = new char[6]; // 0.1 days
        final char[] end = new char[6];   // 0.1 days
        final char[] obsTyp = new char[6];
        final char[] obsLev = new char[6];
        final char[] modID = new char[2]; // 0, 1, 2
        final char[] relID = new char[2];
        final char[] rmsFit = new char[4]; // mm
        final char[] sigPos = new char[4]; // mm
        final char[] sigVel = new char[4]; // micro meter / s
        final char[] qualit = new char[1];
        final char[] tdtUtc = new char[5]; // s
        final char[] cmmnt = new char[78];

        reader.read(recKey, 0, 6);
        reader.read(start, 0, 6);
        reader.read(end, 0, 6);
        reader.read(obsTyp, 0, 6);
        reader.read(obsLev, 0, 6);
        reader.read(modID, 0, 2);
        reader.read(relID, 0, 2);
        reader.read(rmsFit, 0, 4);
        reader.read(sigPos, 0, 4);
        reader.read(sigVel, 0, 4);
        reader.read(qualit, 0, 1);
        reader.read(tdtUtc, 0, 5);
        reader.read(cmmnt, 0, 78);

        dataHeaderRecord = new DataHeaderRecord();
        dataHeaderRecord.recKey = new String(recKey);
        dataHeaderRecord.start = Float.parseFloat(new String(start).trim());
        dataHeaderRecord.end = Float.parseFloat(new String(end).trim());
        dataHeaderRecord.obsTyp = new String(obsTyp);
        dataHeaderRecord.obsLev = new String(obsLev);
        dataHeaderRecord.modID = Integer.parseInt(new String(modID).trim());
        dataHeaderRecord.relID = Integer.parseInt(new String(relID).trim());
        dataHeaderRecord.rmsFit = Integer.parseInt(new String(rmsFit).trim());
        dataHeaderRecord.sigPos = Integer.parseInt(new String(sigPos).trim());
        dataHeaderRecord.sigVel = Integer.parseInt(new String(sigVel).trim());
        String qualitStr = new String(qualit).trim();
        dataHeaderRecord.qualit = qualitStr.isEmpty() ? 0 : Integer.parseInt(qualitStr);
        dataHeaderRecord.tdtUtc = Float.parseFloat(new String(tdtUtc).trim());
        dataHeaderRecord.cmmnt = new String(cmmnt);
    }

    /**
     * Read complete CTS orbit data records from the orbit file.
     * @param file The PRARE orbit file.
     * @throws IOException The exceptions.
     */
    public void readOrbitData(File file) throws IOException {

        computeNumberOfRecords(file);

        orbitVectors = new OrbitVector[numOfTrajectoryRecords];

        recordTimes = new double[numOfTrajectoryRecords];

        final int numCharactersSkip = sizeOfDataSetIdentificationRecord + sizeOfDataHeaderRecord +
                                      numOfTrajectoryRecords * sizeOfTrajectoryRecord;

        final BufferedReader reader = getBufferedReader(file);

        reader.skip((long)numCharactersSkip);

        for (int i = 0; i < numOfTrajectoryRecords; i++) {

            final TrajectoryRecord dataRecord = readTrajectoryRecord(reader);

            orbitVectors[i] = new OrbitVector();
            // convert TDT time to UTC time
            orbitVectors[i].utcTime = TDT2UTC((double)dataRecord.tTagD*0.1 +  0.5 +
                                              (double)dataRecord.tTagMs * microSecondToSecond * secondToDay);
            orbitVectors[i].xPos = (double)dataRecord.xSat * millimeterToMeter;
            orbitVectors[i].yPos = (double)dataRecord.ySat * millimeterToMeter;
            orbitVectors[i].zPos = (double)dataRecord.zSat * millimeterToMeter;
            orbitVectors[i].xVel = (double)dataRecord.xDSat * microMeterToMeter;
            orbitVectors[i].yVel = (double)dataRecord.yDSat * microMeterToMeter;
            orbitVectors[i].zVel = (double)dataRecord.zDSat * microMeterToMeter;

            recordTimes[i] = orbitVectors[i].utcTime;
        }

        if(numOfQualityParameterRecords > 0) {
            qualityParameterRecords = new QualityParameterRecord[numOfQualityParameterRecords];
            for (int j = 0; j < numOfQualityParameterRecords; j++) {
                qualityParameterRecords[j] = readQualityParameterRecord(reader);
            }
        }

        reader.close();
    }

    /**
     * Compute the number of Trajectory Records in the orbit file.
     * @param file The orbit file.
     * @throws IOException The exceptions.
     */
    private void computeNumberOfRecords(File file) throws IOException {

        final int fileSize = (int)file.length();

        final int numRecordsApprox = (fileSize - sizeOfDataSetIdentificationRecord - sizeOfDataHeaderRecord -
                maxNumOfQualityParameterRecords*sizeOfQualityParameterRecord) / (2*sizeOfTrajectoryRecord);

        final int numCharactersSkip = sizeOfDataSetIdentificationRecord + sizeOfDataHeaderRecord +
                                numRecordsApprox * sizeOfTrajectoryRecord;

        final BufferedReader reader = getBufferedReader(file);

        reader.skip((long)numCharactersSkip);

        final char[] dataRecord = new char[sizeOfTrajectoryRecord];

        int k = 0;
        while (true) {
            reader.read(dataRecord, 0, sizeOfTrajectoryRecord);
            if (dataRecord[0] == 'S' && dataRecord[1] == 'T' && dataRecord[2] == 'T' &&
                dataRecord[3] == 'E' && dataRecord[4] == 'R' && dataRecord[5] == 'R') {
                break;
            }
            k++;
        }
        reader.close();

        numOfTrajectoryRecords = numRecordsApprox + k;

        numOfQualityParameterRecords = maxNumOfQualityParameterRecords - 2*k;
    }

    /**
     * Read one Trajectory Record from the orbit file.
     * @param reader The buffered reeader.
     * @return The Trajectory Record.
     * @throws IOException The exceptions.
     */
    private static TrajectoryRecord readTrajectoryRecord(BufferedReader reader) throws IOException {

        final char[] recKey = new char[6];
        final char[] satID = new char[7];
        char[] orbTyp = new char[1];
        final char[] tTagD = new char[6];   // 0.1 days
        final char[] tTagMs = new char[11]; // micro seconds
        final char[] xSat = new char[12];   // mm
        final char[] ySat = new char[12];   // mm
        final char[] zSat = new char[12];   // mm
        final char[] xDSat = new char[11];  // micro seconds / s
        final char[] yDSat = new char[11];  // micro seconds / s
        final char[] zDSat = new char[11];  // micro seconds / s
        final char[] roll = new char[6];    // 0.001 deg
        final char[] pitch = new char[6];   // 0.001 deg
        final char[] yaw = new char[6];     // 0.001 deg
        final char[] ascArc = new char[2];
        final char[] check = new char[3];
        final char[] quali = new char[1];
        final char[] radCor = new char[4];

        reader.read(recKey, 0, 6);
        reader.read(satID, 0, 7);
        reader.read(orbTyp, 0, 1);
        reader.read(tTagD, 0, 6);
        reader.read(tTagMs, 0, 11);
        reader.read(xSat, 0, 12);
        reader.read(ySat, 0, 12);
        reader.read(zSat, 0, 12);
        reader.read(xDSat, 0, 11);
        reader.read(yDSat, 0, 11);
        reader.read(zDSat, 0, 11);
        reader.read(roll, 0, 6);
        reader.read(pitch, 0, 6);
        reader.read(yaw, 0, 6);
        reader.read(ascArc, 0, 2);
        reader.read(check, 0, 3);
        reader.read(quali, 0, 1);
        reader.read(radCor, 0, 4);
        reader.skip(2);

        final TrajectoryRecord trajectoryRecord = new TrajectoryRecord();
        trajectoryRecord.recKey = new String(recKey);
        trajectoryRecord.satID = Integer.parseInt(new String(satID).trim());
        trajectoryRecord.orbTyp = new String(orbTyp);
        trajectoryRecord.tTagD = Float.parseFloat(new String(tTagD).trim());
        trajectoryRecord.tTagMs = Long.parseLong(new String(tTagMs).trim());
        trajectoryRecord.xSat = Long.parseLong(new String(xSat).trim());
        trajectoryRecord.ySat = Long.parseLong(new String(ySat).trim());
        trajectoryRecord.zSat = Long.parseLong(new String(zSat).trim());
        trajectoryRecord.xDSat = Long.parseLong(new String(xDSat).trim());
        trajectoryRecord.yDSat = Long.parseLong(new String(yDSat).trim());
        trajectoryRecord.zDSat = Long.parseLong(new String(zDSat).trim());
        trajectoryRecord.roll = Float.parseFloat(new String(roll).trim());
        trajectoryRecord.pitch = Float.parseFloat(new String(pitch).trim());
        trajectoryRecord.yaw = Float.parseFloat(new String(yaw).trim());
        trajectoryRecord.ascArc = Integer.parseInt(new String(ascArc).trim());
        trajectoryRecord.check = Integer.parseInt(new String(check).trim());
        trajectoryRecord.quali = Integer.parseInt(new String(quali).trim());
        trajectoryRecord.radCor = Integer.parseInt(new String(radCor).trim());

        return trajectoryRecord;
    }

    /**
     * Read one Quality Parameter Record from the orbit file.
     * @param reader The buffered reeader.
     * @return The Quality Parameter Record.
     * @throws IOException The exceptions.
     */
    private static QualityParameterRecord readQualityParameterRecord(BufferedReader reader) throws IOException {

        final char[] recKey = new char[6];
        final char[] qPName = new char[25];
        final char[] qPValue = new char[10];
        final char[] qPUnit = new char[26];
        final char[] qPRefVal = new char[10];

        reader.read(recKey, 0, 6);
        reader.read(qPName, 0, 25);
        reader.skip(3);
        reader.read(qPValue, 0, 10);
        reader.read(qPUnit, 0, 26);
        reader.read(qPRefVal, 0, 10);
        reader.skip(50);

        final QualityParameterRecord qualityParameterRecord = new QualityParameterRecord();
        qualityParameterRecord.recKey = new String(recKey);
        qualityParameterRecord.qPName = new String(qPName);
        qualityParameterRecord.qPValue = new String(qPValue);
        qualityParameterRecord.qPUnit = new String(qPUnit);
        qualityParameterRecord.qPRefVal = new String(qPRefVal);

        return qualityParameterRecord;
    }

    /**
     * Get the start time of the Arc in MJD (in days).
     * @return The start time.
     * @throws IOException The exception.
     */
    public float getSensingStart() throws IOException {
        // The start time is given as Julian days since 1.1.2000 12h in TDT.
        // Add 0.5 days to make it as Julian days since 1.1.2000 0h in TDT. 
        // convert TDT time to UTC time
        return (float)TDT2UTC(dataHeaderRecord.start*0.1f + 0.5f); // 0.1 days to days
    }

    /**
     * Get the end time of the Arc in MJD (in days).
     * @return The end time.
     * @throws IOException The exception.
     */
    public float getSensingStop() throws IOException {
        // The end time is given as Julian days since 1.1.2000 12h in TDT.
        // Add 0.5 days to make it as Julian days since 1.1.2000 0h in TDT.
        // convert TDT time to UTC time
        return (float)TDT2UTC(dataHeaderRecord.end*0.1f + 0.5f); // 0.1 days to days
    }

    /**
     * Convert TDT time to UTC time
     * @param tdt TDT time in days.
     * @return The UTC time.
     * @throws IOException The exception.
     */
    private static double TDT2UTC(double tdt) throws IOException {
        double tai = tdt - 32.184*secondToDay;

        if (tai >= ProductData.UTC.create(new Date(109,1,1), 0).getMJD()) {                 /* 2009 Jan 1 */
            return tai - 34.0*secondToDay;
        } else if (tai >= ProductData.UTC.create(new Date(106,1,1), 0).getMJD()) {          /* 2006 Jan 1 */
            return tai - 33.0*secondToDay;
        } else if (tai >= ProductData.UTC.create(new Date(99,1,1), 0).getMJD()) {          /* 1999 Jan 1 */
            return tai - 32.0*secondToDay;
        } else if (tai >= ProductData.UTC.create(new Date(97,7,1), 0).getMJD()) {          /* 1997 Jul 1 */
            return tai - 31.0*secondToDay;
        } else if (tai >= ProductData.UTC.create(new Date(96,1,1), 0).getMJD()) {          /* 1996 Jan 1 */
            return tai - 30.0*secondToDay;
        } else if (tai >= ProductData.UTC.create(new Date(94,7,1), 0).getMJD()) {          /* 1994 July 1 */
            return tai - 29.0*secondToDay;
        } else if (tai >= ProductData.UTC.create(new Date(93,7,1), 0).getMJD()) {          /* 1993 July 1 */
            return tai - 28.0*secondToDay;
        } else if (tai >= ProductData.UTC.create(new Date(92,7,1), 0).getMJD()) {          /* 1992 July 1 */
            return tai - 27.0*secondToDay;
        } else if (tai >= ProductData.UTC.create(new Date(91,1,1), 0).getMJD()) {          /* 1991 Jan 1 */
            return tai - 26.0*secondToDay;
        } else {
            throw new IOException("Incorrect UTC time");
        }
    }

    /**
     * Get Data Set Identification Record.
     * @return The record.
     */
    public DataSetIdentificationRecord getDataSetIdentificationRecord() {
        return dataSetIdentificationRecord;
    }

    /**
     * Get Data Header Record.
     * @return The record.
     */
    public DataHeaderRecord getDataHeaderRecord() {
        return dataHeaderRecord;
    }

    /**
     * Get Quality Parameter Record.
     * @param n The record index.
     * @return The record.
     */
    public QualityParameterRecord getQualityParameterRecord(int n) {
        return qualityParameterRecords[n];
    }

    /**
     * Get orbit vector.
     * @param n The vector index.
     * @return The orbit vector.
     */
    public OrbitVector getOrbitVector(int n) {
        return orbitVectors[n];
    }

    /**
     * Get orbit vector for given UTC time.
     * @param utc The UTC time.
     * @throws Exception for incorrect time.
     * @return The orbit vector.
     */
    public OrbitVector getOrbitVector(double utc) throws Exception {

        final int n = Arrays.binarySearch(recordTimes, utc);

        if (n >= 0) {
			return orbitVectors[n];
		}

		final int n2 = -n - 1;
        final int n0 = n2 - 2;
        final int n1 = n2 - 1;
        final int n3 = n2 + 1;

        if (n0 < 0 || n1 < 0 || n2 >= recordTimes.length || n3 >= recordTimes.length) {
            throw new Exception("Incorrect UTC time");
        }

        final double[] timeArray = {recordTimes[n0], recordTimes[n1], recordTimes[n2], recordTimes[n3]};
        final double[] xPosArray = {orbitVectors[n0].xPos, orbitVectors[n1].xPos, orbitVectors[n2].xPos, orbitVectors[n3].xPos};
        final double[] yPosArray = {orbitVectors[n0].yPos, orbitVectors[n1].yPos, orbitVectors[n2].yPos, orbitVectors[n3].yPos};
        final double[] zPosArray = {orbitVectors[n0].zPos, orbitVectors[n1].zPos, orbitVectors[n2].zPos, orbitVectors[n3].zPos};
        final double[] xVelArray = {orbitVectors[n0].xVel, orbitVectors[n1].xVel, orbitVectors[n2].xVel, orbitVectors[n3].xVel};
        final double[] yVelArray = {orbitVectors[n0].yVel, orbitVectors[n1].yVel, orbitVectors[n2].yVel, orbitVectors[n3].yVel};
        final double[] zVelArray = {orbitVectors[n0].zVel, orbitVectors[n1].zVel, orbitVectors[n2].zVel, orbitVectors[n3].zVel};

        final OrbitVector orb = new OrbitVector();
        orb.utcTime = utc;
        orb.xPos = MathUtils.lagrangeInterpolatingPolynomial(timeArray, xPosArray, utc);
        orb.yPos = MathUtils.lagrangeInterpolatingPolynomial(timeArray, yPosArray, utc);
        orb.zPos = MathUtils.lagrangeInterpolatingPolynomial(timeArray, zPosArray, utc);
        orb.xVel = MathUtils.lagrangeInterpolatingPolynomial(timeArray, xVelArray, utc);
        orb.yVel = MathUtils.lagrangeInterpolatingPolynomial(timeArray, yVelArray, utc);
        orb.zVel = MathUtils.lagrangeInterpolatingPolynomial(timeArray, zVelArray, utc);
        /*
        final double mu = (utc - recordTimes[n1]) / (recordTimes[n2] - recordTimes[n1]);

        OrbitVector orb = new OrbitVector();

        orb.utcTime = MathUtils.interpolationCubic(
            orbitVectors[n0].utcTime, orbitVectors[n1].utcTime, orbitVectors[n2].utcTime, orbitVectors[n3].utcTime, mu);

        orb.xPos = MathUtils.interpolationCubic(
            orbitVectors[n0].xPos, orbitVectors[n1].xPos, orbitVectors[n2].xPos, orbitVectors[n3].xPos, mu);

        orb.yPos = MathUtils.interpolationCubic(
            orbitVectors[n0].yPos, orbitVectors[n1].yPos, orbitVectors[n2].yPos, orbitVectors[n3].yPos, mu);

        orb.zPos = MathUtils.interpolationCubic(
            orbitVectors[n0].zPos, orbitVectors[n1].zPos, orbitVectors[n2].zPos, orbitVectors[n3].zPos, mu);

        orb.xVel = MathUtils.interpolationCubic(
            orbitVectors[n0].xVel, orbitVectors[n1].xVel, orbitVectors[n2].xVel, orbitVectors[n3].xVel, mu);

        orb.yVel = MathUtils.interpolationCubic(
            orbitVectors[n0].yVel, orbitVectors[n1].yVel, orbitVectors[n2].yVel, orbitVectors[n3].yVel, mu);

        orb.zVel = MathUtils.interpolationCubic(
            orbitVectors[n0].zVel, orbitVectors[n1].zVel, orbitVectors[n2].zVel, orbitVectors[n3].zVel, mu);
        */
        return orb;
    }

    // PRC Data Set Identification Record
    public final static class DataSetIdentificationRecord {
        String recKey;
        String prodID;
        String datTyp;
    }

    // PRC Data Header Record
    public final static class DataHeaderRecord {
        String recKey;
        float start;  // 0.1 days
        float end;    // 0.1 days
        String obsTyp;
        String obsLev;
        int modID;    // 0, 1, 2
        int relID;
        int rmsFit;   // mm
        int sigPos;   // mm
        int sigVel;   // micro meter / s
        int qualit;
        float tdtUtc; // s
        String cmmnt;
    }

    // PRC Trajectory Record (Inertial Frame or Terrestrial Frame)
   public final static class TrajectoryRecord {
        String recKey;
        int satID;
        String orbTyp;
        float tTagD; // 0.1 days
        long tTagMs;  // micro seconds
        long xSat;    // mm
        long ySat;    // mm
        long zSat;    // mm
        long xDSat;   // micro meters / s
        long yDSat;   // micro meters / s
        long zDSat;   // micro meters / s
        float roll;  // 0.001 deg
        float pitch; // 0.001 deg
        float yaw;   // 0.001 deg
        int ascArc;  // 0, 1
        int check;
        int quali;   // 0, 1
        int radCor;  // cm
    }

    // PRC Quality Parameter Record
    public final static class QualityParameterRecord {
        String recKey;
        String qPName;
        String qPValue;
        String qPUnit;
        String qPRefVal;
    }

    public final static class OrbitVector {
        public double utcTime = 0;
        public double xPos = 0;
        public double yPos = 0;
        public double zPos = 0;
        public double xVel = 0;
        public double yVel = 0;
        public double zVel = 0;
    }

    /**
     * Gets the singleton instance of this class.
     * @return the singlton instance
     */
    public static PrareOrbitReader getInstance() {
        return Holder.instance;
    }

    /** Initialization on demand holder idiom
     */
    private static class Holder {
        private static final PrareOrbitReader instance = new PrareOrbitReader();
    }
}
