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
package org.esa.beam.dataio.ceos.records;

import org.esa.beam.dataio.ceos.CeosFileReader;
import org.esa.beam.dataio.ceos.CeosHelper;
import org.esa.beam.dataio.ceos.IllegalCeosFormatException;

import java.io.IOException;

public abstract class Ancillary1Record extends BaseRecord {

    private String _referenceEllipsoid;
    private double _semimajorAxisOfReferenceEllipsoid;
    private double _semiminorAxisOfReferenceEllipsoid;
    private String _GeodeticCoordinateName;
    private double[][] _L1B2Coeffs;

    public Ancillary1Record(final CeosFileReader reader) throws IOException,
                                                                IllegalCeosFormatException {
        this(reader, -1);
    }

    public Ancillary1Record(final CeosFileReader reader, final long startPos) throws
                                                                              IOException,
                                                                              IllegalCeosFormatException {
        super(reader, startPos);

        readGeneralFields(reader);

        reader.seek(getAbsolutPosition(getRecordLength())); // seek to end of record
    }

    private void readGeneralFields(final CeosFileReader reader) throws IOException,
                                                                       IllegalCeosFormatException {
        // skip bytes of values which must be read dynamically
        reader.skipBytes(752);
        _referenceEllipsoid = reader.readAn(16);
        _semimajorAxisOfReferenceEllipsoid = reader.readFn(16);
        _semiminorAxisOfReferenceEllipsoid = reader.readFn(16);
        _GeodeticCoordinateName = reader.readAn(16);
        // skip bytes of values which must be read dynamically
    }

    public long getNumNominalPixelsPerLine_1A_1B1() throws IOException,
                                                           IllegalCeosFormatException {
        return readIn(16, 12);
    }

    public long getNumNominalLinesPerScene_1A_1B1() throws IOException,
                                                           IllegalCeosFormatException {
        return readIn(16, 28);
    }

    public double getNominalInterPixelDistance_1A_1B1() throws IOException,
                                                               IllegalCeosFormatException {
        return readF16(44);
    }

    public double getNominalInterLineDistance_1A_1B1() throws IOException,
                                                              IllegalCeosFormatException {
        return readF16(61);
    }

    public double getImageSkew() throws IOException,
                                        IllegalCeosFormatException {
        return readF16(76);
    }

    public int getHemisphere() throws IOException,
                                      IllegalCeosFormatException {
        getReader().seek(getAbsolutPosition(92));
        return getReader().readI4();
    }

    public long getUTMZoneNumber() throws IOException,
                                          IllegalCeosFormatException {
        return readIn(12, 96);
    }

    public double getSceneCenterNorthing() throws IOException,
                                                  IllegalCeosFormatException {
        return readF16(140);
    }

    public double getSceneCenterEasting() throws IOException,
                                                 IllegalCeosFormatException {
        return readF16(156);
    }

    public double getAngleBetweenMapUTMVerticalAndTrueNorth() throws IOException,
                                                                     IllegalCeosFormatException {
        return readF16(204);
    }

    public double getMapProjOriginLat() throws IOException,
                                               IllegalCeosFormatException {
        return readF16(332);
    }

    public double getMapProjOriginLon() throws IOException,
                                               IllegalCeosFormatException {
        return readF16(348);
    }

    public double getPSReferenceLat() throws IOException,
                                             IllegalCeosFormatException {
        return readF16(364);
    }

    public double getPSReferenceLon() throws IOException,
                                             IllegalCeosFormatException {
        return readF16(380);
    }

    public double getSceneCenterX() throws IOException,
                                           IllegalCeosFormatException {
        return readF16(428);
    }

    public double getSceneCenterY() throws IOException,
                                           IllegalCeosFormatException {
        return readF16(444);
    }

    public double getAngleBetweenMapPSVerticalAndTrueNorth() throws IOException,
                                                                    IllegalCeosFormatException {
        return readF16(492);
    }

    public double getNumNominalPixelsPerLine() throws IOException,
                                                      IllegalCeosFormatException {

        return readF16(508);
    }

    public double getNumNominalLinesPerScene() throws IOException,
                                                      IllegalCeosFormatException {
        return readF16(524);
    }

    public double getNominalInterPixelDistance() throws IOException,
                                                        IllegalCeosFormatException {
        return readF16(540);
    }

    public double getNominalInterLineDistance() throws IOException,
                                                       IllegalCeosFormatException {
        return readF16(556);
    }

    public double getAngleBetweenMapVerticalAndTrueNorth() throws IOException,
                                                                  IllegalCeosFormatException {
        return readF16(620);
    }

    public double getNominalSateliteOrbitInclination() throws IOException,
                                                              IllegalCeosFormatException {
        return readF16(636);
    }

    public double getNominalAscendingNodeLon() throws IOException,
                                                      IllegalCeosFormatException {
        return readF16(652);
    }

    public double getNominalSateliteAltitude() throws IOException,
                                                      IllegalCeosFormatException {
        return readF16(668);
    }

    public double getNominalGroundSpeed() throws IOException,
                                                 IllegalCeosFormatException {
        return readF16(684);
    }

    public double getSatteliteHeadingAngleIncludingEarthRotationOfSceneCenter() throws IOException,
                                                                                       IllegalCeosFormatException {
        return readF16(700);
    }

    public double getSwathAngle() throws IOException,
                                         IllegalCeosFormatException {
        return readF16(732);
    }

    public double getNominalScanRate() throws IOException,
                                              IllegalCeosFormatException {
        return readF16(748);
    }

    public String getReferenceEllipsoid() {
        return _referenceEllipsoid;
    }

    public double getSemimajorAxisOfReferenceEllipsoid() {
        return _semimajorAxisOfReferenceEllipsoid;
    }

    public double getSemiminorAxisOfReferenceEllipsoid() {
        return _semiminorAxisOfReferenceEllipsoid;
    }

    public String getGeodeticCoordinateName() {
        return _GeodeticCoordinateName;
    }

    public double[] getLatCoeffs_1B2() throws IOException,
                                              IllegalCeosFormatException {
        return readDoubles(10, 956);
    }

    public double[] getLonCoeffs_1B2() throws IOException,
                                              IllegalCeosFormatException {
        return readDoubles(10, 1196);
    }

    public double[] getXCoeffs_1B2() throws IOException,
                                            IllegalCeosFormatException {
        return readDoubles(10, 1436);
    }

    public double[] getYCoeffs_1B2() throws IOException,
                                            IllegalCeosFormatException {
        return readDoubles(10, 1676);
    }

    public double[][] getTransformationCoeffsL1B2() throws IOException,
                                                           IllegalCeosFormatException {
        if (_L1B2Coeffs == null) {
            _L1B2Coeffs = new double[4][10];
            final long[] tempLongs = new long[_L1B2Coeffs[0].length];
            getReader().seek(getAbsolutPosition(956));
            for (int i = 0; i < _L1B2Coeffs.length; i++) {
                getReader().readB8(tempLongs);
                _L1B2Coeffs[i] = CeosHelper.convertLongToDouble(tempLongs);
            }
        }
        return _L1B2Coeffs;
    }

    public double[] getF4FunctionCoeffs_1B2() throws IOException,
                                                     IllegalCeosFormatException {
        final long[] longs = readLongs(6, 1916);
        return CeosHelper.convertLongToDouble(longs);
    }

    public abstract double[][] getTransformationCoefficientsFor(final int index) throws IOException,
                                                                                        IllegalCeosFormatException;

    private double readF16(final int relativePosition) throws IOException,
                                                              IllegalCeosFormatException {
        getReader().seek(getAbsolutPosition(relativePosition));
        return getReader().readFn(16);
    }

    private double[] readDoubles(final int numDoubles, final int relativePosition) throws
                                                                                   IOException,
                                                                                   IllegalCeosFormatException {
        final double[] coeffs = new double[numDoubles];
        getReader().seek(getAbsolutPosition(relativePosition));
        getReader().readGn(24, coeffs);
        return coeffs;
    }

    private long readIn(final int n, final int relativePosition) throws IOException,
                                                                        IllegalCeosFormatException {
        getReader().seek(getAbsolutPosition(relativePosition));
        return getReader().readIn(n);
    }

}
