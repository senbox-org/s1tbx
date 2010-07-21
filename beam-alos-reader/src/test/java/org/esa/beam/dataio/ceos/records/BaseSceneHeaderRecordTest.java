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
import java.util.Calendar;

public abstract class BaseSceneHeaderRecordTest extends TestCase {

    private final int _level1A = 0;
    private final int _level1B1 = 1;
    private final int _level1B2 = 2;

    private String _prefix;
    private ImageOutputStream _ios;

    @Override
    protected void setUp() throws Exception {
        final ByteArrayOutputStream os = new ByteArrayOutputStream(24);
        _ios = new MemoryCacheImageOutputStream(os);
        _prefix = "fdkjglsdkfhierr.m b9b0970w34";
        _ios.writeBytes(_prefix);
    }

    public void testInit_Level1A_SimpleConstructor() throws IOException,
                                                            IllegalCeosFormatException {
        writeRecordData(_ios, _level1A);
        _ios.writeBytes("nq3tf9ß8nvnvpdi er 0 324p3f"); // as suffix
        final CeosFileReader reader = new CeosFileReader(_ios);
        reader.seek(_prefix.length());

        final BaseSceneHeaderRecord record = createSceneHeaderRecord(reader);

        assertRecord(record, _level1A);
    }

    protected abstract BaseSceneHeaderRecord createSceneHeaderRecord(final CeosFileReader reader) throws IOException,
                                                                                                         IllegalCeosFormatException;

    public void testInit_Level1B1() throws IOException,
                                           IllegalCeosFormatException {
        writeRecordData(_ios, _level1B1);
        _ios.writeBytes("nq3tf9ß8nvnvpdi er 0 324p3f"); // as suffix
        final CeosFileReader reader = new CeosFileReader(_ios);

        final BaseSceneHeaderRecord record = createSceneHeaderRecord(reader, _prefix.length());

        assertRecord(record, _level1B1);
    }

    protected abstract BaseSceneHeaderRecord createSceneHeaderRecord(final CeosFileReader reader,
                                                                     final int startPos) throws IOException,
                                                                                                IllegalCeosFormatException;

    public void testInit_Level1B2() throws IOException,
                                           IllegalCeosFormatException {
        writeRecordData(_ios, _level1B2);
        _ios.writeBytes("nq3tf9ß8nvnvpdi er 0 324p3f"); // as suffix
        final CeosFileReader reader = new CeosFileReader(_ios);

        final BaseSceneHeaderRecord record = createSceneHeaderRecord(reader, _prefix.length());

        assertRecord(record, _level1B2);
    }

    private void assertRecord(final BaseSceneHeaderRecord record, final int level) throws IOException {
        BaseRecordTest.assertRecord(record);
        assertEquals(_prefix.length(), record.getStartPos());
        assertEquals(_prefix.length() + 4680, _ios.getStreamPosition());

        assertEquals(4, record.getHeaderRecordNumber());
        if (level == _level1A) {
            assertEquals("O1A___UB        ", record.getProductId());
            assertEquals("1A", record.getProductLevel());
        } else if (level == _level1B1) {
            assertEquals("O1B1__UB        ", record.getProductId());
            assertEquals("1B1", record.getProductLevel());
        } else if (level == _level1B2) {
            assertEquals("O1B2R_UB        ", record.getProductId());
            assertEquals("1B2", record.getProductLevel());
        }
        assertEquals("uncorrected SCID", record.getUncorrectedSceneId());
        assertEquals(1.2345678, record.getSceneCenterLat_L1A_L1B1(), 1e-8);
        assertEquals(2.3456789, record.getSceneCenterLon_L1A_L1B1(), 1e-8);
        assertEquals(3.4567891, record.getSceneCenterLineNum_L1A_L1B1(), 1e-8);
        assertEquals(4.5678912, record.getSceneCenterPixelNum_L1A_L1B1(), 1e-8);
        assertEquals("?pkdfperioerngpeiun?werngpiufver", record.getSceneCenterTime());
        assertEquals(5165486514651865L, record.getTimeOffsetFromNominalRspCenter());
        assertEquals("dgponronvsdkl   ", record.getRspId());
        assertEquals(9845131842323841L, record.getOrbitsPerCycle());

        assertEquals("poerpov?md?refui", record.getSceneID_L1B2());
        assertEquals(5.6789123, record.getSceneCenterLat_L1B2(), 1e-8);
        assertEquals(6.7891234, record.getSceneCenterLon_L1B2(), 1e-8);
        assertEquals(7.8912345, record.getSceneCenterLineNum_L1B2(), 1e-8);
        assertEquals(8.9123456, record.getSceneCenterPixelNum_L1B2(), 1e-8);
        assertEquals("           NNN.N", record.getOrientationAngle());
        assertEquals("SNN.N           ", record.getIncidentAngle());
        assertEquals("ALOS            ", record.getMissionId());
        assertEquals("PRISM           ", record.getSensorId());
        assertEquals(1231586482315865L, record.getCalcOrbitNo());
        assertEquals("A               ", record.getOrbitDirection());

        assertFields30To31(record);
        assertEquals(2006, record.getDateImageWasTaken().get(Calendar.YEAR));
        assertEquals(5, record.getDateImageWasTaken().get(Calendar.MONTH)); // Month is zero-based
        assertEquals(12, record.getDateImageWasTaken().get(Calendar.DAY_OF_MONTH));
        assertEquals("C LDD-MM/WDDD-MM ", record.getSceneCenterLonLat());
        assertEquals("XXX BBBB  ", record.getSensorTypeAndSpectrumBandIdentification());
        assertEquals("SUN ELGGG AHHH", record.getSceneCenterSunAngle());

        assertEquals("GGP-R-XXX   ", record.getProcessingCode());
        assertEquals("JAXAALOS    ", record.getCompetentAgentAndProjectIdentification());
        assertEquals("AABBBCDDDDDEEEE ", record.getSceneId());

        assertEquals(5674, record.getNumEffectiveBands());
        assertEquals(32000, record.getNumPixelsPerLineInImage());
        assertEquals(14000, record.getNumLinesInImage());
        assertEquals(574, record.getRadiometricResolution());
        assertEquals("RGD             ", record.getLevel1B2Option());
        assertEquals("NNYNN           ", record.getResamplingMethod());
        assertEquals("NNNNY           ", record.getMapProjectionMethod());
        assertEquals("CORRECTIONLEVEL ", record.getCorrectionLevel());
        assertEquals(147, record.getNumMapProjAncillaryRecords());
        assertEquals(65, record.getNumRadiometricAncillaryRecords());

        // effektiveBands // 64I1
        final int[] effektiveBands = record.getEffektiveBands();
        assertEquals(64, effektiveBands.length);
        for (int i = 0; i < effektiveBands.length; i++) {
            final int expected = (i + 1) % 10;
            assertEquals(expected, effektiveBands[i]);
        }

        assertEquals("BSQ             ", record.getImageFormat());
        assertEquals(16.1111111, record.getSceneCornerUpperLeftLat(), 1e-8);
        assertEquals(17.2222222, record.getSceneCornerUpperLeftLon(), 1e-8);
        assertEquals(18.3333333, record.getSceneCornerUpperRightLat(), 1e-8);
        assertEquals(19.4444444, record.getSceneCornerUpperRightLon(), 1e-8);
        assertEquals(20.5555555, record.getSceneCornerLowerLeftLat(), 1e-8);
        assertEquals(21.6666666, record.getSceneCornerLowerLeftLon(), 1e-8);
        assertEquals(22.7777777, record.getSceneCornerLowerRightLat(), 1e-8);
        assertEquals(23.8888888, record.getSceneCornerLowerRightLon(), 1e-8);
        assertEquals("12", record.getStatusTimeSystem());
        assertEquals("34", record.getStatusAbsoluteNavigation());
        assertEquals("56", record.getFlagAttitudeDetermination());
        assertEquals("78", record.getAccuracyUsedOrbitData());
        assertEquals("90", record.getAccuracyUsedAttitudeData());
        assertFields73ToEnd(record);
    }


    private void writeRecordData(final ImageOutputStream ios, final int level) throws IOException {
        BaseRecordTest.writeRecordData(ios);
        ios.writeBytes("   4"); // headerRecordNumber // I4
        CeosTestHelper.writeBlanks(ios, 4); // 4 x blank
        if (level == _level1A) {
            ios.writeBytes("O1A___UB        "); // productID // A16
        } else if (level == _level1B1) {
            ios.writeBytes("O1B1__UB        "); // productID // A16
        } else if (level == _level1B2) {
            ios.writeBytes("O1B2R_UB        "); // productID // A16
        }
        ios.writeBytes("uncorrected SCID"); // uncorrectedSceneID // A16
        ios.writeBytes("       1.2345678"); // sceneCenterLat_L1A_L1B1 // F16.7
        ios.writeBytes("       2.3456789"); // sceneCenterLon_L1A_L1B1 // F16.7
        ios.writeBytes("       3.4567891"); // sceneCenterLineNum_L1A_L1B1 // F16.7
        ios.writeBytes("       4.5678912"); // sceneCenterPixelNum_L1A_L1B1 // F16.7
        ios.writeBytes("?pkdfperioerngpeiun?werngpiufver"); // sceneCenterTime // A32
        ios.writeBytes("5165486514651865"); // timeOffsetFromNominalRspCenter // I16
        ios.writeBytes("dgponronvsdkl   "); // rspId // A16
        ios.writeBytes("9845131842323841"); // orbitsPerCycle // I16

        ios.writeBytes("poerpov?md?refui"); // sceneID_L1B2 // A16
        ios.writeBytes("       5.6789123"); // sceneCenterLat_L1B2 // F16.7
        ios.writeBytes("       6.7891234"); // sceneCenterLon_L1B2 // F16.7
        ios.writeBytes("       7.8912345"); // sceneCenterLineNum_L1B2 // F16.7
        ios.writeBytes("       8.9123456"); // sceneCenterPixelNum_L1B2 // F16.7
        ios.writeBytes("           NNN.N"); // orientationAngle // A16
        ios.writeBytes("SNN.N           "); // incidentAngle // A16
        ios.writeBytes("ALOS            "); // missionId // A16
        ios.writeBytes("PRISM           "); // sensorId // A16
        ios.writeBytes("1231586482315865"); // calcOrbitNo // I16
        ios.writeBytes("A               "); // orbitDirection // A16

        writeFields30To31(ios);

        CeosTestHelper.writeBlanks(ios, 11); // 11 x blank
        ios.writeBytes("12Jun06 "); // dateImageWasTaken // A8
        ios.writeBytes("C LDD-MM/WDDD-MM "); // sceneCenterLonLat // A17
        CeosTestHelper.writeBlanks(ios, 17); // 17 x blank
        ios.writeBytes("XXX BBBB  "); // sensorTypeAndSpectrumBandIdentification // A10
        ios.writeBytes("SUN ELGGG AHHH"); // sceneCenterSunAngle // A14

        ios.writeBytes("GGP-R-XXX   "); // processingCode // A12
        ios.writeBytes("JAXAALOS    "); // competentAgentAndProjectIdentification // A12
        ios.writeBytes("AABBBCDDDDDEEEE "); // sceneId // A16
        CeosTestHelper.writeBlanks(ios, 906); // 906 x blank

        ios.writeBytes("            5674"); // numEffectiveBands // I16
        ios.writeBytes("           32000"); // numPixelsPerLineInImage // I16
        ios.writeBytes("           14000"); // numLinesInImage // I16
        CeosTestHelper.writeBlanks(ios, 16); // 16 x blank
        CeosTestHelper.writeBlanks(ios, 16); // 16 x blank
        ios.writeBytes("             574"); // readiometricResolution // I16
        CeosTestHelper.writeBlanks(ios, 16); // 16 x blank
        ios.writeBytes("RGD             "); // level1B2Option // A16
        ios.writeBytes("NNYNN           "); // resamplingMethod // A16
        ios.writeBytes("NNNNY           "); // mapProjectionMethod // A16
        ios.writeBytes("CORRECTIONLEVEL "); // correctionLevel // A16
        ios.writeBytes("             147"); // numMapProjAncillaryRecords // I16
        ios.writeBytes("              65"); // numRadiometricAncillaryRecords // I16
        CeosTestHelper.writeBlanks(ios, 32); // 32 x blank
        // effektiveBands // 64I1
        ios.writeBytes("1234567890123456789012345678901234567890123456789012345678901234");

        ios.writeBytes("BSQ             "); // imageFormat // A16
        ios.writeBytes("      16.1111111"); // sceneCornerUpperLeftLat // F16.7
        ios.writeBytes("      17.2222222"); // sceneCornerUpperLeftLon // F16.7
        ios.writeBytes("      18.3333333"); // sceneCornerUpperRightLat // F16.7
        ios.writeBytes("      19.4444444"); // sceneCornerUpperRightLon // F16.7
        ios.writeBytes("      20.5555555"); // sceneCornerLowerLeftLat // F16.7
        ios.writeBytes("      21.6666666"); // sceneCornerLowerLeftLon // F16.7
        ios.writeBytes("      22.7777777"); // sceneCornerLowerRightLat // F16.7
        ios.writeBytes("      23.8888888"); // sceneCornerLowerRightLon // F16.7
        ios.writeBytes("12"); // statusTimeSystem // A2
        ios.writeBytes("34"); // statusAbsoluteNavigation // A2
        ios.writeBytes("56"); // flagAttitudeDetermination // A2
        ios.writeBytes("78"); // accuracyUsedOrbitData // A2
        ios.writeBytes("90"); // accuracyUsedAttitudeData // A2
        writeFields73ToEnd(ios);
    }


    protected abstract void writeFields30To31(final ImageOutputStream ios) throws IOException;

    protected abstract void writeFields73ToEnd(final ImageOutputStream ios) throws IOException;

    protected abstract void assertFields30To31(final BaseSceneHeaderRecord record);

    protected abstract void assertFields73ToEnd(final BaseSceneHeaderRecord record);

}