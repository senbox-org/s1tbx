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
import org.esa.nest.util.GeoUtils;
import org.esa.nest.util.MathUtils;
import org.esa.nest.util.ResourceUtils;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.StringTokenizer;

/**
 * Reads Delft ODR format orbit files
 */
public final class OrbitalDataRecordReader {

    private DataInputStream in = null;

    // header 1
    private String productSpecifier;
    private String satelliteName;
    private int arcStart;
    // header 2
    private int lengthOfRepeatCycle;
    private int arcNumber;
    private int numRecords = 0;
    private int version;
    // data records
    private OrbitDataRecord[] dataRecords = null;
    private OrbitPositionRecord[] orbitPositions = null;
    private double[] recordTimes = null;
    private double days1985To2000; // Days from Jan. 1, 1985 to Jan. 1, 2000

    private static final double halfSecond = 0.5 / (24*3600); // in days

    public static final int invalidArcNumber = -1;

    public boolean readOrbitFile(String path) throws Exception {

        if(OpenOrbitFile(path)) {
            parseHeader1();
            parseHeader2();

            if(numRecords > 0) {
                dataRecords = new OrbitDataRecord[numRecords];
                orbitPositions = new OrbitPositionRecord[numRecords];
                recordTimes = new double[numRecords];
                days1985To2000 = ProductData.UTC.parse("01-JAN-1985 00:00:00").getMJD();

                for(int i=0; i < numRecords; ++i) {
                    dataRecords[i] = parseDataRecord();
                    orbitPositions[i] = computeOrbitPosition(dataRecords[i]);
                    recordTimes[i] = orbitPositions[i].utcTime;
                }
            }

            return true;
        }

        return false;
    }

    boolean OpenOrbitFile(String path) {

        try {
            in = new DataInputStream(new BufferedInputStream(ResourceUtils.getResourceAsStream(path)));
        } catch(Exception e) {
            in = null;
            return false;
        } 
        return true;
    }

    void parseHeader1() {
        if(in == null) return;
        
        try {
            // Product specifier ('@ODR' or 'xODR').
            productSpecifier = readAn(4);
            // Satellite name.
            satelliteName = readAn(8);
            // Advised start of the data arc (UTC secs past 1.0 Jan 1985)
            arcStart = in.readInt();
        } catch (IOException e) {

            System.out.print(e);
        }
    }

    void parseHeader2() {
        if(in == null) return;

        try {
            // Length of the repeat cycle in 10^-3 days.
            lengthOfRepeatCycle = in.readInt();
            // Arc number. This number refers to the orbital solution arcs as processed by DUT/DEOS.
            arcNumber = in.readInt();
            // Number of data records following the two headers.
            numRecords = in.readInt();

            // Version ID
            //Field number 4 of the second header record may contain a positive number
            //which refers to the list below.
            //
            // ID  Explanation
            //-------------------------------------------------------------------------------
            //  1  Orbits generated 'after the event'. Though generated with the operational
            //     software, they do not contain predictions. The gravity model used is
            //     GEM-T2.
            //  2  As 1. The gravity model used is PGS4591.
            //  3  Orbits generated with operational software in operational mode. Thus they
            //     also contain predictions. The gravity model used is GEM-T2.
            //  4  As 3. The gravity model used is PGS4591.
            //  5. Orbits generated with enhanced operational software (Version 920110),
            //     which is able to integrate over small orbital maneuvers. Thus predictions
            //     are accurate in case the magnitude of the orbital maneuver was know in
            //     advance. GEM-T2 model.
            //  6. As 5. With the PGS4591 gravity model.
            //  7. Orbits generated at 5 minute intervals. GEM-T2 model.
            //  9. Orbits generated with enhanced operational software (Version 920110),
            //     which is able to integrate over small orbital maneuvers.
            //     These arcs do not contain predictions. The 5.5-day arcs cover data
            //     until the end. GEM-T2 model.
            // 10. As 9. The gravity model used is PGS4591.
            //105. As 5. But with predictions removed. Arcs cut at 5.5 days.
            //201. 5.5-day arcs. No predictions. JGM-1 gravity model. Geodyn II version 9208.
            //     SLR-only solution.
            //202. 5.5-day arcs. No predictions. JGM-1 gravity model. Geodyn II version 9208.
            //     Solution based on SLR and altimeter tracking data.
            //301. JGM-2 gravity model, improved station coordinates, new GM.
            //     SLR-only solution.
            //302. Operational. JGM-3 gravity model, SLR-only solution.
            //303. as 301, altimeter (IGDR) xover data included as tracking data.
            //322. Precise, JGM-3 gravity model, Quick-look SLR, OPR single satellite xovers
            //304. JGM-3 gravity field, Quick-look SLR, OPR single satellite xovers
            //     DUT(LSC)95L03 coordinate solution.
            //305. as 404. Adds also OPR altimetric height above a seasonal mean sea
            //             surface as tracking data. 6-hourly drag parameters.
            //323. as 304.
            //324. as 304. Includes also ERS-1/2 dual satellite xovers. 6-hourly drag
            //             parameters.
            //404. as 304. DGM-E04 gravity field model used instead of JGM-3
            //405. as 305. DGM-E04 gravity field model used instead of JGM-3
            //424. as 324. DGM-E04 gravity field model used instead of JGM-3
            //504. as 304. EGM96 gravity field model used instead of JGM-3
            //505. as 305. EGM96 gravity field model used instead of JGM-3
            //524. as 324. EGM96 gravity field model used instead of JGM-3
            version = in.readInt();
            
        } catch (IOException e) {

            System.out.print(e);
        }
    }

    OrbitDataRecord parseDataRecord() {
        final OrbitDataRecord data = new OrbitDataRecord();

        try {
            data.time = in.readInt();
            
            //latitude
            //@ODR: in microdegrees.
            //xODR: in 0.1 microdegrees.
            data.latitude = in.readInt();

            //longitude
            //@ODR: in microdegrees, interval 0 to 360 degrees.
	        //xODR: in 0.1 microdegrees, interval -180 to 180 degrees.
            data.longitude = in.readInt();

            data.heightOfCenterOfMass = in.readInt();
        } catch (IOException e) {

            System.out.print(e);
        }
        return data;
    }

    public String getProductSpecifier() {
        return productSpecifier;
    }

    public String getSatelliteName() {
        return satelliteName;
    }

    public int getArcStart() {
        return arcStart;
    }

    public int getLengthOfRepeatCycle() {
        return lengthOfRepeatCycle;
    }

    public int getArcNumber() {
        return arcNumber;
    }

    public int getNumRecords() {
        return numRecords;
    }

    public int getVersion() {
        return version;
    }

    public OrbitDataRecord[] getDataRecords() {
        return dataRecords;
    }

    String readAn(final int n) throws IOException {

        final byte[] bytes = new byte[n];
        final int bytesRead;

        bytesRead = in.read(bytes);

        if (bytesRead != n) {
            final String message = "Error parsing file: expecting " + n + " bytes but got " + bytesRead;
            throw new IOException(message);
        }
        return new String(bytes);
    }

    /**
     * Convert satellite position from deodetic coordinate to global cartesian coordinate.
     * @param dataRecord The data record read from delft orbit file.
     * @return The data record in cartesian coordinate.
     */
    private OrbitPositionRecord computeOrbitPosition(OrbitDataRecord dataRecord) {

        // record time in UTC seconds past 1.0 January 1985.
        final double time = (double)dataRecord.time / 86400.0; // to days

        // record time in days past since Jan.1, 2000
        final double utcTime = time + days1985To2000; // days1985To2000 is negative

        // Height of the nominal center of mass above the GRS80 reference ellipsoid (in millimeters)
        final double alt = (double)dataRecord.heightOfCenterOfMass / 1000.0; // millimeters to meters

        double lat, lon;
        if (productSpecifier.contains("xODR")) {
            lat = (double)dataRecord.latitude / 10000000.0; // xODR: 0.1 microdegrees to degrees
            lon = (double)dataRecord.longitude / 10000000.0; // xODR: 0.1 microdegrees, [-180, 180]
        } else if (productSpecifier.contains("@ODR")) {
            lat = (double)dataRecord.latitude / 1000000.0; //  @ODR: microdegrees to degrees
            lon = (double)dataRecord.longitude / 1000000.0; // @ODR: in microdegrees, [0, 360]
            if (lon > 180) { // convert to interval [-180, 180]
                lon -= 360;
            }
        } else {
            throw new OperatorException("Invalid product specifier: " + productSpecifier);
        }

        final double[] xyz = new double[3];
        GeoUtils.geo2xyz(lat, lon, alt, xyz, GeoUtils.EarthModel.GRS80);

        final OrbitPositionRecord orbitPosition = new OrbitPositionRecord();
        orbitPosition.utcTime = utcTime;
        orbitPosition.xPos = xyz[0];
        orbitPosition.yPos = xyz[1];
        orbitPosition.zPos = xyz[2];

        return orbitPosition;
    }

    /**
     * Get orbit position for given UTC time using cubic interpolation.
     * @param utc The UTC time.
     * @return The orbit position.
     * @throws Exception The exceptions.
     */
    private OrbitPositionRecord getOrbitPosition(double utc) throws Exception {

        final int n = Arrays.binarySearch(recordTimes, utc);

		if (n >= 0) {
			return orbitPositions[n];
		}

		final int n2 = -n - 1;
        final int n0 = n2 - 2;
        final int n1 = n2 - 1;
        final int n3 = n2 + 1;

        if (n0 < 0 || n1 < 0 || n2 >= recordTimes.length || n3 >= recordTimes.length) {
            throw new Exception("Incorrect UTC time");
        }

        final double[] timeArray = {recordTimes[n0], recordTimes[n1], recordTimes[n2], recordTimes[n3]};
        final double[] xPosArray = {orbitPositions[n0].xPos, orbitPositions[n1].xPos, orbitPositions[n2].xPos, orbitPositions[n3].xPos};
        final double[] yPosArray = {orbitPositions[n0].yPos, orbitPositions[n1].yPos, orbitPositions[n2].yPos, orbitPositions[n3].yPos};
        final double[] zPosArray = {orbitPositions[n0].zPos, orbitPositions[n1].zPos, orbitPositions[n2].zPos, orbitPositions[n3].zPos};

        final OrbitPositionRecord orbitPosition = new OrbitPositionRecord();
        orbitPosition.utcTime = utc;
        orbitPosition.xPos = MathUtils.lagrangeInterpolatingPolynomial(timeArray, xPosArray, utc);
        orbitPosition.yPos = MathUtils.lagrangeInterpolatingPolynomial(timeArray, yPosArray, utc);
        orbitPosition.zPos = MathUtils.lagrangeInterpolatingPolynomial(timeArray, zPosArray, utc);
        /*
        final double mu = (utc - recordTimes[n1]) / (recordTimes[n2] - recordTimes[n1]);

        OrbitPositionRecord orbitPosition = new OrbitPositionRecord();

        orbitPosition.utcTime = MathUtils.interpolationCubic(orbitPositions[n0].utcTime,
                                                             orbitPositions[n1].utcTime,
                                                             orbitPositions[n2].utcTime,
                                                             orbitPositions[n3].utcTime,
                                                             mu);

        orbitPosition.xPos = MathUtils.interpolationCubic(orbitPositions[n0].xPos,
                                                          orbitPositions[n1].xPos,
                                                          orbitPositions[n2].xPos,
                                                          orbitPositions[n3].xPos,
                                                          mu);

        orbitPosition.yPos = MathUtils.interpolationCubic(orbitPositions[n0].yPos,
                                                          orbitPositions[n1].yPos,
                                                          orbitPositions[n2].yPos,
                                                          orbitPositions[n3].yPos,
                                                          mu);

        orbitPosition.zPos = MathUtils.interpolationCubic(orbitPositions[n0].zPos,
                                                          orbitPositions[n1].zPos,
                                                          orbitPositions[n2].zPos,
                                                          orbitPositions[n3].zPos,
                                                          mu);
        */
        return orbitPosition;
    }

    /**
     * Get orbit vector for given UTC time.
     * @param utc The UTC time.
     * @throws Exception for incorrect time.
     * @return The orbit vector.
     */
    public OrbitVector getOrbitVector(double utc) throws Exception {

        final OrbitPositionRecord orbitPos = getOrbitPosition(utc);
        final OrbitPositionRecord orbitPosFw = getOrbitPosition(utc + halfSecond);
        final OrbitPositionRecord orbitPosBw = getOrbitPosition(utc - halfSecond);

        final OrbitVector orbitVector = new OrbitVector();
        orbitVector.utcTime = orbitPos.utcTime;
        orbitVector.xPos = orbitPos.xPos;
        orbitVector.yPos = orbitPos.yPos;
        orbitVector.zPos = orbitPos.zPos;

        // compute velocity as suggested by http://www.deos.tudelft.nl/ers/precorbs/faq.shtml
        orbitVector.xVel = orbitPosFw.xPos - orbitPosBw.xPos;
        orbitVector.yVel = orbitPosFw.yPos - orbitPosBw.yPos;
        orbitVector.zVel = orbitPosFw.zPos - orbitPosBw.zPos;

        return orbitVector;
    }
    
    /**
     * Get the arc number from the arclist file for a given product date.
     * @param file The arclist file.
     * @param productDate The product date.
     * @return The arc number.
     * @throws IOException The exceptions.
     */
    public static int getArcNumber(File file, Date productDate) {

        final String fileName = file.getAbsolutePath();

        // get reader
        FileInputStream stream;
        try {
            stream = new FileInputStream(fileName);
        } catch(FileNotFoundException e) {
            throw new OperatorException("File not found: " + fileName);
        }

        final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

        // read data from file and compare each record with product date
        final String titleLine = "Arc# ------- Arc interval ------ -SLR-xover-altim  Repeat Ver  ---- Begin ----";
        final SimpleDateFormat dateformat = new SimpleDateFormat("yyMMdd HH:mm");
        int arcNum = invalidArcNumber;
        Date startDate;
        Date endDate;
        String line = "";
        StringTokenizer st;

        try {
            // get the title line
            while((line = reader.readLine()) != null) {
                if (line.equals(titleLine)) {
                    break;
                }
            }

            // get the rest arc record lines
            while((line = reader.readLine()) != null) {

                st = new StringTokenizer(line);

                // get arc number
                final int recordArcNum = Integer.parseInt(st.nextToken());

                // get start date and start time
                try {
                    startDate = dateformat.parse(st.nextToken() + " " + st.nextToken());
                } catch (ParseException e) {
                    throw new OperatorException(e);
                }

                // get a hyphen (-)
                final String hyphen = st.nextToken();

                // get end date and end time
                try {
                    endDate = dateformat.parse(st.nextToken() + " " + st.nextToken());
                } catch (ParseException e) {
                    throw new OperatorException(e);
                }

                if (productDate.compareTo(startDate) >= 0 && productDate.compareTo(endDate) < 0) {
                    arcNum = recordArcNum;
                    break;
                }
            }

            reader.close();
            stream.close();

        } catch (IOException e) {
            throw new OperatorException(e);
        }

        return arcNum;
    }

    static class OrbitDataRecord {
        int time;
        int latitude;
        int longitude;
        int heightOfCenterOfMass;
    }

    static class OrbitPositionRecord {
        double utcTime = 0;
        double xPos = 0;
        double yPos = 0;
        double zPos = 0;
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
    public static OrbitalDataRecordReader getInstance() {
        return Holder.instance;
    }

    /** Initialization on demand holder idiom
     */
    private static class Holder {
        private static final OrbitalDataRecordReader instance = new OrbitalDataRecordReader();
    }
}
