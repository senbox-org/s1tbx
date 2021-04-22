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

import Jama.Matrix;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.esa.s1tbx.io.orbits.BaseOrbitFile;
import org.esa.s1tbx.io.orbits.OrbitFile;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Orbits;
import org.esa.snap.engine_utilities.util.Maths;
import org.esa.snap.engine_utilities.util.Settings;

import java.io.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

    private List<Orbits.OrbitVector> osvList = new ArrayList<>();
    private String fileVersion;

    private static LoadingCache<File, Sentinel1OrbitFileReader> cache;

    public SentinelPODOrbitFile(final MetadataElement absRoot, final int polyDegree) {
        super(absRoot);
        this.polyDegree = polyDegree;
    }

    public String[] getAvailableOrbitTypes() {
        return new String[]{PRECISE, RESTITUTED};
    }

    public File retrieveOrbitFile(final String orbitType) throws Exception {
        final ProductData.UTC stateVectorTime = absRoot.getAttributeUTC(AbstractMetadata.STATE_VECTOR_TIME);
        final Calendar calendar = absRoot.getAttributeUTC(AbstractMetadata.STATE_VECTOR_TIME).getAsCalendar();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1; // zero based
        final int day = calendar.get(Calendar.DAY_OF_MONTH);
        final String missionPrefix = getMissionPrefix(absRoot);

        final File localFolder = getDestFolder(missionPrefix, orbitType, year, month);

        orbitFile = findOrbitFile(missionPrefix, orbitType, stateVectorTime, year, month);

        if (orbitFile == null) {
            try {
                final GnssOrbitFileDownloader gnssOrbitFileDownloader = new GnssOrbitFileDownloader();
                orbitFile = gnssOrbitFileDownloader.download(localFolder, "Sentinel-1", missionPrefix,
                        orbitType, year, month, day, stateVectorTime);
            } catch(Exception e) {
                // try next
            }
        }
        if (orbitFile == null) {
            try {
                final OrbitFileScraper scraper = new OrbitFileScraper.Step(orbitType);
                orbitFile = scraper.download(localFolder, missionPrefix, orbitType, year, month, day, stateVectorTime);
            } catch(Exception e) {
                // try next
            }
        }

        if (orbitFile == null) {
            String timeStr = absRoot.getAttributeUTC(AbstractMetadata.STATE_VECTOR_TIME).format();
            final File destFolder = getDestFolder(missionPrefix, orbitType, year, month);
            throw new IOException("No valid orbit file found for " + timeStr +
                    "\nOrbit files may be downloaded from https://qc.sentinel1.eo.esa.int/ or http://aux.sentinel1.eo.esa.int/"
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

    static File getDestFolder(final String missionPrefix, final String orbitType, final int year, final int month) {
        final String prefOrbitPath;
        if (orbitType.startsWith(RESTITUTED)) {
            String def = SystemUtils.getAuxDataPath().resolve("Orbits").resolve("Sentinel-1").resolve("RESORB").toString();
            prefOrbitPath = Settings.instance().get("OrbitFiles.sentinel1RESOrbitPath", def);
        } else {
            String def = SystemUtils.getAuxDataPath().resolve("Orbits").resolve("Sentinel-1").resolve("POEORB").toString();
            prefOrbitPath = Settings.instance().get("OrbitFiles.sentinel1POEOrbitPath", def);
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

    static File findOrbitFile(final String missionPrefix, final String orbitType,
                              final ProductData.UTC stateVectorTime, final int year, final int month) {
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
            if (Sentinel1OrbitFileReader.isWithinRange(file.getName(), stateVectorTime)) {
                return file;
            }
        }
        return null;
    }

    /**
     * Check if product acquisition time is within the validity period of the orbit file.
     *
     * @throws Exception
     */
    private void checkOrbitFileValidity(final Sentinel1OrbitFileReader orbitFileReader) throws Exception {

        final double stateVectorTime = absRoot.getAttributeUTC(AbstractMetadata.STATE_VECTOR_TIME).getMJD();
        final String validityStartTimeStr = orbitFileReader.getValidityStartFromHeader();
        final String validityStopTimeStr = orbitFileReader.getValidityStopFromHeader();
        final double validityStartTimeMJD = Sentinel1OrbitFileReader.toUTC(validityStartTimeStr).getMJD();
        final double validityStopTimeMJD = Sentinel1OrbitFileReader.toUTC(validityStopTimeStr).getMJD();

        if (stateVectorTime < validityStartTimeMJD || stateVectorTime > validityStopTimeMJD) {
            throw new IOException("Product acquisition time is not within the validity period of the orbit");
        }
    }

    @Override
    public String getVersion() {
        return fileVersion;
    }

    /**
     * Get orbit state vector for given time using polynomial fitting.
     *
     * @param utc The UTC in days.
     * @return The orbit state vector.
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

        final Sentinel1OrbitFileReader orbitFileReader = getCache().get(orbitFile);

        checkOrbitFileValidity(orbitFileReader);

        osvList = orbitFileReader.getOrbitStateVectors();
        fileVersion = orbitFileReader.getFileVersion();
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

    private LoadingCache<File, Sentinel1OrbitFileReader> getCache() {
        if(cache == null) {
            cache = createCache();
        }
        return cache;
    }

    private static LoadingCache<File, Sentinel1OrbitFileReader> createCache() {
        return CacheBuilder.newBuilder().maximumSize(100).initialCapacity(100)
                .expireAfterAccess(20, TimeUnit.MINUTES)
                .build(new CacheLoader<File, Sentinel1OrbitFileReader>() {
                           @Override
                           public Sentinel1OrbitFileReader load(File key) throws Exception {
                               Sentinel1OrbitFileReader orbitFileReader = new Sentinel1OrbitFileReader(key);
                               orbitFileReader.read();

                               return orbitFileReader;
                           }
                       }
                );
    }
}
