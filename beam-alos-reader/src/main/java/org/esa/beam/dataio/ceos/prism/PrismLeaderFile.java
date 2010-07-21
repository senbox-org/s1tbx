/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.beam.dataio.ceos.prism;

import org.esa.beam.dataio.ceos.CeosFileReader;
import org.esa.beam.dataio.ceos.IllegalCeosFormatException;
import org.esa.beam.dataio.ceos.prism.records.LeaderFileDescriptorRecord;
import org.esa.beam.dataio.ceos.prism.records.PrismAncillary1Record;
import org.esa.beam.dataio.ceos.prism.records.PrismAncillary2Record;
import org.esa.beam.dataio.ceos.prism.records.SceneHeaderRecord;
import org.esa.beam.dataio.ceos.records.Ancillary1Record;
import org.esa.beam.dataio.ceos.records.Ancillary3Record;
import org.esa.beam.framework.datamodel.GeoPos;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.Calendar;

class PrismLeaderFile {

    private final LeaderFileDescriptorRecord _leaderFDR;
    private final SceneHeaderRecord _sceneHeaderRecord;
    private final PrismAncillary1Record _ancillary1Record;
    private final PrismAncillary2Record _ancillary2Record;
    private final Ancillary3Record _ancillary3Record;
    private final CeosFileReader _reader;

    public PrismLeaderFile(final ImageInputStream leaderStream) throws IOException,
                                                                       IllegalCeosFormatException {
        _reader = new CeosFileReader(leaderStream);
        _leaderFDR = new LeaderFileDescriptorRecord(_reader);
        _sceneHeaderRecord = new SceneHeaderRecord(_reader);
        _ancillary1Record = new PrismAncillary1Record(_reader);
        _ancillary2Record = new PrismAncillary2Record(_reader);
        _ancillary3Record = new Ancillary3Record(_reader);
    }

    public Ancillary1Record getAncillary1Record() {
        return _ancillary1Record;
    }

    public PrismAncillary2Record getAncillary2Record() {
        return _ancillary2Record;
    }

    public Ancillary3Record getAncillary3Record() {
        return _ancillary3Record;
    }

    public SceneHeaderRecord getSceneHeaderRecord() {
        return _sceneHeaderRecord;
    }

    public LeaderFileDescriptorRecord getLeaderFileDescriptorRecord() {
        return _leaderFDR;
    }

    public String getProductLevel() {
        return _sceneHeaderRecord.getProductLevel();
    }

    public int getSceneWidth() {
        return (int) _sceneHeaderRecord.getNumPixelsPerLineInImage();
    }

    public int getSceneHeight() {
        return (int) _sceneHeaderRecord.getNumLinesInImage();
    }

    public Calendar getDateImageWasTaken() {
        return _sceneHeaderRecord.getDateImageWasTaken();
    }

    public String getProductName() {
        return _sceneHeaderRecord.getSceneId().trim()
               + "-"
               + _sceneHeaderRecord.getProductId().trim();
    }

    public String getProductType() {
        return "PRM" + getProductLevel();
    }

    public void close() throws IOException {
        _reader.close();
    }

    public String getDatumName() throws IOException,
                                        IllegalCeosFormatException {
        return _ancillary1Record.getGeodeticCoordinateName();
    }

    public double getSemiMinorAxis() throws IOException,
                                            IllegalCeosFormatException {
        return _ancillary1Record.getSemiminorAxisOfReferenceEllipsoid();
    }

    public double getSemiMajorAxis() throws IOException,
                                            IllegalCeosFormatException {
        return _ancillary1Record.getSemimajorAxisOfReferenceEllipsoid();
    }

    public long getUTMZoneIndex() throws IOException,
                                         IllegalCeosFormatException {
        return _ancillary1Record.getUTMZoneNumber();
    }

    public boolean isUTMSouthHemisphere() throws IOException,
                                                 IllegalCeosFormatException {
        return _ancillary1Record.getHemisphere() == 1;
    }

    public double getUTMEasting() throws IOException,
                                         IllegalCeosFormatException {
        return _ancillary1Record.getSceneCenterEasting();
    }

    public double getUTMNorthing() throws IOException,
                                          IllegalCeosFormatException {
        return _ancillary1Record.getSceneCenterNorthing();
    }

    public long getNominalPixelsPerLine_1A_1B1() throws IOException,
                                                        IllegalCeosFormatException {
        return _ancillary1Record.getNumNominalPixelsPerLine_1A_1B1();
    }

    public long getNominalLinesPerScene_1A_1B1() throws IOException,
                                                        IllegalCeosFormatException {
        return _ancillary1Record.getNumNominalLinesPerScene_1A_1B1();
    }

    public double getNominalInterPixelDistance_1A_1B1() throws IOException,
                                                               IllegalCeosFormatException {
        return _ancillary1Record.getNominalInterPixelDistance_1A_1B1();
    }

    public double getNominalInterLineDistance_1A_1B1() throws IOException,
                                                              IllegalCeosFormatException {
        return _ancillary1Record.getNominalInterLineDistance_1A_1B1();
    }

    public double getImageSkew() throws IOException,
                                        IllegalCeosFormatException {
        return _ancillary1Record.getImageSkew();
    }

    /**
     * Gets the pixel size in x direction in meters.
     *
     * @return the pixel size in x direction
     *
     * @throws IOException
     * @throws IllegalCeosFormatException
     */
    public double getNominalInterPixelDistance() throws IOException,
                                                        IllegalCeosFormatException {
        return _ancillary1Record.getNominalInterPixelDistance();
    }

    /**
     * Gets the pixel size in y direction in meters.
     *
     * @return the pixel size in y direction
     *
     * @throws IOException
     * @throws IllegalCeosFormatException
     */
    public double getNominalInterLineDistance() throws IOException,
                                                       IllegalCeosFormatException {
        return _ancillary1Record.getNominalInterLineDistance();
    }

    public double getUTMOrientationAngle() throws IOException,
                                                  IllegalCeosFormatException {
        return _ancillary1Record.getAngleBetweenMapUTMVerticalAndTrueNorth();
    }

    public double[][] getCorrectedTransformationCoeffs()
            throws
            IOException,
            IllegalCeosFormatException {
        return _ancillary1Record.getTransformationCoeffsL1B2();
    }

    public double[][] getUncorrectedTransformationCoeffs(final int ccdNumber) throws IOException,
                                                                                     IllegalCeosFormatException {
        return _ancillary1Record.getTransformationCoefficientsFor(ccdNumber);
    }

    public GeoPos getPSReferencePoint() throws IOException,
                                               IllegalCeosFormatException {
        final float referenceLat = (float) _ancillary1Record.getPSReferenceLat();
        final float referenceLon = (float) _ancillary1Record.getPSReferenceLon();
        return new GeoPos(referenceLat, referenceLon);
    }

    public GeoPos getPSProjectionOrigin() throws IOException,
                                                 IllegalCeosFormatException {
        final float originLat = (float) _ancillary1Record.getMapProjOriginLat();
        final float originLon = (float) _ancillary1Record.getMapProjOriginLon();
        return new GeoPos(originLat, originLon);
    }

    public double getPSXCoordinate() throws IOException,
                                            IllegalCeosFormatException {
        return _ancillary1Record.getSceneCenterX();
    }

    public double getPSYCoordinate() throws IOException,
                                            IllegalCeosFormatException {
        return _ancillary1Record.getSceneCenterY();
    }

    public double getPSOrientationAngle() throws IOException,
                                                 IllegalCeosFormatException {
        return _ancillary1Record.getAngleBetweenMapPSVerticalAndTrueNorth();
    }
}
