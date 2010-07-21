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

import junit.framework.TestCase;
import org.esa.beam.dataio.ceos.CeosFileReader;
import org.esa.beam.dataio.ceos.CeosTestHelper;
import org.esa.beam.dataio.ceos.IllegalCeosFormatException;

import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public abstract class Ancillary1RecordTest extends TestCase {

    private static final int RECORD_LENGTH = 4680;

    private MemoryCacheImageOutputStream _ios;
    private String _prefix;
    private CeosFileReader _reader;

    @Override
    protected void setUp() throws Exception {
        final ByteArrayOutputStream os = new ByteArrayOutputStream(24);
        _ios = new MemoryCacheImageOutputStream(os);
        _prefix = "fdkjglsdkfhierr.m b9b0970w34";
        _ios.writeBytes(_prefix);
        _reader = new CeosFileReader(_ios);
    }

    public void testInitRecord_SimpleConstructor() throws IOException,
                                                          IllegalCeosFormatException {
        writeRecordData(_ios);
        _ios.writeBytes("nq3tf9ß8nvnvpdi er 0 324p3f"); // as suffix
        _reader.seek(_prefix.length());

        final Ancillary1Record record = createAncillary1Record(_reader);

        assertRecord(record);
    }

    public void testInitRecord() throws IOException,
                                        IllegalCeosFormatException {
        writeRecordData(_ios);
        _ios.writeBytes("nq3tf9ß8nvnvpdi er 0 324p3f"); // as suffix

        final Ancillary1Record record = createAncillary1Record(_reader, _prefix.length());

        assertRecord(record);
    }

    public void testInitRecord_exceptions() throws IOException,
                                                   IllegalCeosFormatException,
                                                   NoSuchMethodException,
                                                   IllegalAccessException {
        writeRecordDataForExceptions(_ios);
        _ios.writeBytes("nq3tf9ß8nvnvpdi er 0 324p3f"); // as suffix

        final Ancillary1Record record = createAncillary1Record(new CeosFileReader(_ios), _prefix.length());

        assertExceptions(record);
    }

    private void assertRecord(final Ancillary1Record record) throws IOException,
                                                                    IllegalCeosFormatException {
        BaseRecordTest.assertRecord(record);
        assertEquals(_prefix.length(), record.getStartPos());
        assertEquals(_prefix.length() + RECORD_LENGTH, _ios.getStreamPosition());

        assertEquals(16959, record.getNumNominalPixelsPerLine_1A_1B1());
        assertEquals(14000, record.getNumNominalLinesPerScene_1A_1B1());
        assertEquals(2.5, record.getNominalInterPixelDistance_1A_1B1(), 1e-8);
        assertEquals(3.5000000, record.getNominalInterLineDistance_1A_1B1(), 1e-8);
        assertEquals(4.5000000, record.getImageSkew(), 1e-8);
        assertEquals(1, record.getHemisphere());
        assertEquals(35, record.getUTMZoneNumber());
        assertEquals(5.5000000, record.getSceneCenterNorthing(), 1e-8);
        assertEquals(6.5000000, record.getSceneCenterEasting(), 1e-8);
        assertEquals(7.5000000, record.getAngleBetweenMapUTMVerticalAndTrueNorth(), 1e-8);
        assertEquals(8.5000000, record.getMapProjOriginLat(), 1e-8);
        assertEquals(9.5000000, record.getMapProjOriginLon(), 1e-8);
        assertEquals(10.5000000, record.getPSReferenceLat(), 1e-8);
        assertEquals(11.5000000, record.getPSReferenceLon(), 1e-8);
        assertEquals(12.5000000, record.getSceneCenterX(), 1e-8);
        assertEquals(13.5000000, record.getSceneCenterY(), 1e-8);
        assertEquals(14.5000000, record.getAngleBetweenMapPSVerticalAndTrueNorth(), 1e-8);

        assertEquals(15.5000000, record.getNumNominalPixelsPerLine(), 1e-8);
        assertEquals(16.5000000, record.getNumNominalLinesPerScene(), 1e-8);
        assertEquals(17.5000000, record.getNominalInterPixelDistance(), 1e-8);
        assertEquals(18.5000000, record.getNominalInterLineDistance(), 1e-8);
        assertEquals(19.5000000, record.getAngleBetweenMapVerticalAndTrueNorth(), 1e-8);
        assertEquals(20.5000000, record.getNominalSateliteOrbitInclination(), 1e-8);
        assertEquals(21.5000000, record.getNominalAscendingNodeLon(), 1e-8);
        assertEquals(22.5000000, record.getNominalSateliteAltitude(), 1e-8);
        assertEquals(23.5000000, record.getNominalGroundSpeed(), 1e-8);
        assertEquals(24.5000000, record.getSatteliteHeadingAngleIncludingEarthRotationOfSceneCenter(), 1e-8);
        assertEquals(25.5000000, record.getSwathAngle(), 1e-8);
        assertEquals(26.5000000, record.getNominalScanRate(), 1e-8);

        assertEquals("kjfojmekmvdoklms", record.getReferenceEllipsoid());
        assertEquals(27.5000000, record.getSemimajorAxisOfReferenceEllipsoid(), 1e-8);
        assertEquals(28.5000000, record.getSemiminorAxisOfReferenceEllipsoid(), 1e-8);
        assertEquals("uih0897n434kljss", record.getGeodeticCoordinateName());

        final double[] latCoeffs = record.getLatCoeffs_1B2();
        assertNotNull(latCoeffs);
        assertEquals(10, latCoeffs.length);
        assertEquals(3.51762825378903869E+01, latCoeffs[0], 1e-50);
        assertEquals(-3.72525633118785182E-06, latCoeffs[1], 1e-50);
        assertEquals(-2.22154479999701052E-05, latCoeffs[2], 1e-50);
        assertEquals(6.75514377303297257E-13, latCoeffs[3], 1e-50);
        assertEquals(-3.08409114105194292E-12, latCoeffs[4], 1e-50);
        assertEquals(-6.47199691979237372E-14, latCoeffs[5], 1e-50);
        assertEquals(2.33148210051737742E-18, latCoeffs[6], 1e-50);
        assertEquals(-8.25004293603135684E-19, latCoeffs[7], 1e-50);
        assertEquals(3.89695931047396857E-19, latCoeffs[8], 1e-50);
        assertEquals(7.62881323267747713E-20, latCoeffs[9], 1e-50);

        final double[] lonCoeffs = record.getLonCoeffs_1B2();
        assertNotNull(lonCoeffs);
        assertEquals(10, lonCoeffs.length);
        assertEquals(1.38257153591810379E+02, lonCoeffs[0], 1e-50);
        assertEquals(2.70569048022997204E-05, lonCoeffs[1], 1e-50);
        assertEquals(-4.53710988950547022E-06, lonCoeffs[2], 1e-50);
        assertEquals(-7.30238602559696922E-12, lonCoeffs[3], 1e-50);
        assertEquals(-1.03702398000958172E-12, lonCoeffs[4], 1e-50);
        assertEquals(1.03698394927313316E-12, lonCoeffs[5], 1e-50);
        assertEquals(1.91908097005926553E-18, lonCoeffs[6], 1e-50);
        assertEquals(3.69386366596817251E-18, lonCoeffs[7], 1e-50);
        assertEquals(-1.23128778809667902E-18, lonCoeffs[8], 1e-50);
        assertEquals(-6.39669024583341597E-19, lonCoeffs[9], 1e-50);

        final double[] xCoeffs = record.getXCoeffs_1B2();
        assertNotNull(xCoeffs);
        assertEquals(10, xCoeffs.length);
        assertEquals(-4.17920143679666659E+06, xCoeffs[0], 1e-50);
        assertEquals(-1.45568010437627992E+05, xCoeffs[1], 1e-50);
        assertEquals(3.86479338596612215E+04, xCoeffs[2], 1e-50);
        assertEquals(-1.23862070427656249E+03, xCoeffs[3], 1e-50);
        assertEquals(7.24502994104848858E+03, xCoeffs[4], 1e-50);
        assertEquals(2.32251035691070371E+02, xCoeffs[5], 1e-50);
        assertEquals(1.57097184834409962E+01, xCoeffs[6], 1e-50);
        assertEquals(-1.06636783607930674E+00, xCoeffs[7], 1e-50);
        assertEquals(-8.96152721026457897E+01, xCoeffs[8], 1e-50);
        assertEquals(-5.65286440987201266E-01, xCoeffs[9], 1e-50);

        final double[] yCoeffs = record.getYCoeffs_1B2();
        assertNotNull(yCoeffs);
        assertEquals(10, yCoeffs.length);
        assertEquals(2.15921675754298194E+05, yCoeffs[0], 1e-50);
        assertEquals(1.04608585586919016E+04, yCoeffs[1], 1e-50);
        assertEquals(5.76886918796067948E+03, yCoeffs[2], 1e-50);
        assertEquals(5.65591484966379028E+01, yCoeffs[3], 1e-50);
        assertEquals(-1.84864330812192475E+03, yCoeffs[4], 1e-50);
        assertEquals(7.36076275871487837E+01, yCoeffs[5], 1e-50);
        assertEquals(9.12008243626968529E+00, yCoeffs[6], 1e-50);
        assertEquals(-2.15733856129907675E+00, yCoeffs[7], 1e-50);
        assertEquals(5.52849921945469802E+00, yCoeffs[8], 1e-50);
        assertEquals(-4.26207159240670652E-01, yCoeffs[9], 1e-50);

        final double[] f4FunctionCoeffs = record.getF4FunctionCoeffs_1B2();
        assertNotNull(f4FunctionCoeffs);
        assertEquals(6, f4FunctionCoeffs.length);
        assertEquals(10.23456 + 0, f4FunctionCoeffs[0], 1e-6);
        assertEquals(10.23456 + 1, f4FunctionCoeffs[1], 1e-6);
        assertEquals(10.23456 + 2, f4FunctionCoeffs[2], 1e-6);
        assertEquals(10.23456 + 3, f4FunctionCoeffs[3], 1e-6);
        assertEquals(10.23456 + 4, f4FunctionCoeffs[4], 1e-6);
        assertEquals(10.23456 + 5, f4FunctionCoeffs[5], 1e-6);

        assertCoefficients(record);

    }

    private void writeRecordData(final ImageOutputStream ios) throws IOException {
        BaseRecordTest.writeRecordData(ios);

        ios.writeBytes("           16959"); // numNominalPixelsPerLine // I16
        ios.writeBytes("           14000"); // numNominalLinesPerScene // I16
        ios.writeBytes("       2.5000000"); // sceneCenterInterPixelDistance // F16.7
        ios.writeBytes("       3.5000000"); // sceneCenterInterLineDistance // F16.7
        ios.writeBytes("       4.5000000"); // sceneCenterImageSkew // F16.7
        ios.writeBytes("   1"); // hemisphere // I4
        ios.writeBytes("35          "); // UTM_ZoneNumber // I12
        CeosTestHelper.writeBlanks(ios, 16); // 16 x blank
        CeosTestHelper.writeBlanks(ios, 16); // 16 x blank
        ios.writeBytes("       5.5000000"); // sceneCenterNorthing // F16.7
        ios.writeBytes("       6.5000000"); // sceneCenterEasting // F16.7
        CeosTestHelper.writeBlanks(ios, 16); // 16 x blank
        CeosTestHelper.writeBlanks(ios, 16); // 16 x blank
        ios.writeBytes("       7.5000000"); // angleBetweenMapUTMVerticalAndTrueNorth // F16.7
        CeosTestHelper.writeBlanks(ios, 112); // 112 x blank
        ios.writeBytes("       8.5000000"); // mapProjOriginLat // F16.7
        ios.writeBytes("       9.5000000"); // mapProjOriginLon // F16.7
        ios.writeBytes("      10.5000000"); // reference latitude // F16.7
        ios.writeBytes("      11.5000000"); // reference longitude // F16.7
        CeosTestHelper.writeBlanks(ios, 16); // 16 x blank
        CeosTestHelper.writeBlanks(ios, 16); // 16 x blank
        ios.writeBytes("      12.5000000"); // X coordinates of the scene center (km) // F16.7
        ios.writeBytes("      13.5000000"); // Y coordinates of the scene center (km) // F16.7
        CeosTestHelper.writeBlanks(ios, 16); // 16 x blank
        CeosTestHelper.writeBlanks(ios, 16); // 16 x blank
        ios.writeBytes("      14.5000000"); // angleBetweenMapPSVerticalAndTrueNorth // F16.7

        ios.writeBytes("      15.5000000"); // numNominalPixelsPerLine // F16.7
        ios.writeBytes("      16.5000000"); // numNominalLinesPerScene // F16.7
        ios.writeBytes("      17.5000000"); // nominalOututInterPixelDistance // F16.7
        ios.writeBytes("      18.5000000"); // nominalOutputInterLineDistance // F16.7
        CeosTestHelper.writeBlanks(ios, 16); // 16 x blank
        CeosTestHelper.writeBlanks(ios, 16); // 16 x blank
        CeosTestHelper.writeBlanks(ios, 16); // 16 x blank
        ios.writeBytes("      19.5000000"); // angleBetweenMapVerticalAndTrueNorth // F16.7
        ios.writeBytes("      20.5000000"); // nominalSateliteOrbitInclination // F16.7
        ios.writeBytes("      21.5000000"); // nominalAscendingNodeLon // F16.7
        ios.writeBytes("      22.5000000"); // nominalSateliteAltitude // F16.7
        ios.writeBytes("      23.5000000"); // nominalGroundSpeed // F16.7
        ios.writeBytes("      24.5000000"); // satteliteHeadingAngleIncludingEarthRotationOfSceneCenter // F16.7
        CeosTestHelper.writeBlanks(ios, 16); // 16 x blank
        ios.writeBytes("      25.5000000"); // swathAngle // F16.7
        ios.writeBytes("      26.5000000"); // nominalScanRate // F16.7

        ios.writeBytes("kjfojmekmvdoklms"); // nameReferenceEllipsoid // A16
        ios.writeBytes("      27.5000000"); // semimajorAxisOfReferenceEllipsoid // F16.7
        ios.writeBytes("      28.5000000"); // semiminorAxisOfReferenceEllipsoid // F16.7
        ios.writeBytes("uih0897n434kljss"); // nameGeodeticCoordinates // A16

        CeosTestHelper.writeBlanks(ios, 128); // 128 x blank

        // write latitude coeffs
        ios.writeBytes(" 3.51762825378903869E+01" +
                       "-3.72525633118785182E-06" +
                       "-2.22154479999701052E-05" +
                       " 6.75514377303297257E-13" +
                       "-3.08409114105194292E-12" +
                       "-6.47199691979237372E-14" +
                       " 2.33148210051737742E-18" +
                       "-8.25004293603135684E-19" +
                       " 3.89695931047396857E-19" +
                       " 7.62881323267747713E-20");

        // write longitude coeffs
        ios.writeBytes(" 1.38257153591810379E+02" +
                       " 2.70569048022997204E-05" +
                       "-4.53710988950547022E-06" +
                       "-7.30238602559696922E-12" +
                       "-1.03702398000958172E-12" +
                       " 1.03698394927313316E-12" +
                       " 1.91908097005926553E-18" +
                       " 3.69386366596817251E-18" +
                       "-1.23128778809667902E-18" +
                       "-6.39669024583341597E-19");

        // write X coeffs
        ios.writeBytes("-4.17920143679666659E+06" +
                       "-1.45568010437627992E+05" +
                       " 3.86479338596612215E+04" +
                       "-1.23862070427656249E+03" +
                       " 7.24502994104848858E+03" +
                       " 2.32251035691070371E+02" +
                       " 1.57097184834409962E+01" +
                       "-1.06636783607930674E+00" +
                       "-8.96152721026457897E+01" +
                       "-5.65286440987201266E-01");

        // write Y coeffs
        ios.writeBytes(" 2.15921675754298194E+05" +
                       " 1.04608585586919016E+04" +
                       " 5.76886918796067948E+03" +
                       " 5.65591484966379028E+01" +
                       "-1.84864330812192475E+03" +
                       " 7.36076275871487837E+01" +
                       " 9.12008243626968529E+00" +
                       "-2.15733856129907675E+00" +
                       " 5.52849921945469802E+00" +
                       "-4.26207159240670652E-01");

        // write 6 coefficients for F4 function
        writeIncrementingDoubles(10.23456, 6, ios);

        writeCoefficients(ios);

        CeosTestHelper.writeBlanks(ios, (int) (RECORD_LENGTH - ios.getStreamPosition()));
    }

    protected final void writeIncrementingDoubles(final double v, final int count, final ImageOutputStream ios) throws
                                                                                                                IOException {
        for (int i = 0; i < count; i++) {
            ios.writeDouble(v + i);
        }
    }

    private void writeRecordDataForExceptions(final MemoryCacheImageOutputStream ios) throws IOException {
        BaseRecordTest.writeRecordData(ios);

        CeosTestHelper.writeBlanks(ios, 752);

        ios.writeBytes("kjfojmekmvdoklms"); // nameReferenceEllipsoid // A16
        ios.writeBytes("      27.5000000"); // semimajorAxisOfReferenceEllipsoid // F16.7
        ios.writeBytes("      28.5000000"); // semiminorAxisOfReferenceEllipsoid // F16.7
        ios.writeBytes("uih0897n434kljss"); // nameGeodeticCoordinates // A16

        CeosTestHelper.writeBlanks(ios, 3852);
    }

    private void assertExceptions(final Ancillary1Record record) throws IOException,
                                                                        NoSuchMethodException,
                                                                        IllegalAccessException {
        BaseRecordTest.assertRecord(record);
        assertEquals(_prefix.length(), record.getStartPos());
        assertEquals(_prefix.length() + RECORD_LENGTH, _ios.getStreamPosition());

        assertException(record, "getNumNominalPixelsPerLine_1A_1B1");
        assertException(record, "getNumNominalLinesPerScene_1A_1B1");
        assertException(record, "getNominalInterPixelDistance_1A_1B1");
        assertException(record, "getNominalInterLineDistance_1A_1B1");
        assertException(record, "getImageSkew");
        assertException(record, "getHemisphere");
        assertException(record, "getUTMZoneNumber");
        assertException(record, "getSceneCenterNorthing");
        assertException(record, "getSceneCenterEasting");
        assertException(record, "getAngleBetweenMapUTMVerticalAndTrueNorth");
        assertException(record, "getMapProjOriginLat");
        assertException(record, "getMapProjOriginLon");
        assertException(record, "getPSReferenceLat");
        assertException(record, "getPSReferenceLon");
        assertException(record, "getSceneCenterX");
        assertException(record, "getSceneCenterY");
        assertException(record, "getAngleBetweenMapPSVerticalAndTrueNorth");

        assertException(record, "getNumNominalPixelsPerLine");
        assertException(record, "getNumNominalLinesPerScene");
        assertException(record, "getNominalInterPixelDistance");
        assertException(record, "getNominalInterLineDistance");
        assertException(record, "getAngleBetweenMapVerticalAndTrueNorth");
        assertException(record, "getNominalSateliteOrbitInclination");
        assertException(record, "getNominalAscendingNodeLon");
        assertException(record, "getNominalSateliteAltitude");
        assertException(record, "getNominalGroundSpeed");
        assertException(record, "getSatteliteHeadingAngleIncludingEarthRotationOfSceneCenter");
        assertException(record, "getSwathAngle");
        assertException(record, "getNominalScanRate");

        assertEquals("kjfojmekmvdoklms", record.getReferenceEllipsoid());
        assertEquals(27.5000000, record.getSemimajorAxisOfReferenceEllipsoid(), 1e-8);
        assertEquals(28.5000000, record.getSemiminorAxisOfReferenceEllipsoid(), 1e-8);
        assertEquals("uih0897n434kljss", record.getGeodeticCoordinateName());

        assertException(record, "getLatCoeffs_1B2");
        assertException(record, "getLonCoeffs_1B2");
        assertException(record, "getXCoeffs_1B2");
        assertException(record, "getYCoeffs_1B2");
    }

    private void assertException(final Ancillary1Record record, final String methodName) throws NoSuchMethodException,
                                                                                                IllegalAccessException {
        final Method method = record.getClass().getMethod(methodName);
        final String exceptionMessage = "IllegalCeosFormatException expected for method '";
        try {
            method.invoke(record);
            fail(exceptionMessage + methodName + "'");
        } catch (InvocationTargetException e) {
            if (!(e.getTargetException() instanceof IllegalCeosFormatException)) {
                fail(exceptionMessage + methodName + "'");
            }
        }
    }

    protected abstract Ancillary1Record createAncillary1Record(final CeosFileReader reader) throws IOException,
                                                                                                   IllegalCeosFormatException;

    protected abstract Ancillary1Record createAncillary1Record(final CeosFileReader reader, final int startPos) throws
                                                                                                                IOException,
                                                                                                                IllegalCeosFormatException;

    protected abstract void writeCoefficients(final ImageOutputStream ios) throws IOException;

    protected abstract void assertCoefficients(final Ancillary1Record record) throws IOException,
                                                                                     IllegalCeosFormatException;


}
