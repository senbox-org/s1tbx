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

package org.esa.snap.dataio.envisat;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.Debug;

import java.util.Date;

public class HeaderParserTest extends TestCase {

    private boolean _oldDebugState;

    public HeaderParserTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(HeaderParserTest.class);
    }

    @Override
    protected void setUp() {
        //_oldDebugState = Debug.setEnabled(true);
        _oldDebugState = Debug.setEnabled(false);
    }

    @Override
    protected void tearDown() {
        Debug.setEnabled(_oldDebugState);
    }

    public void testThatSingleEntriesAreParsedCorrectly() {
        try {
            String source = "TOT_SIZE=+00000000000186478394<bytes>\n";
            Header header = HeaderParser.getInstance().parseHeader("TEST", source.getBytes());
            assertNotNull(header);
            assertNotNull(header.getParam("TOT_SIZE"));
            assertNotNull(header.getParam("tot_size"));
            assertSame(header.getParam("TOT_SIZE"), header.getParam("tot_size"));
            assertEquals(1, header.getNumParams());

            Field param = header.getParam("TOT_SIZE");
            assertNotNull(param);
            assertNotNull(param.getInfo());
            assertEquals("TOT_SIZE", param.getInfo().getName());
            assertEquals(ProductData.TYPE_UINT32, param.getInfo().getDataType());
            assertEquals(1, param.getInfo().getNumDataElems());
            assertEquals("bytes", param.getInfo().getPhysicalUnit());
            assertEquals(1, param.getNumElems());
            assertEquals(186478394, param.getElemInt(0));
            Debug.trace("header: " + header);
        } catch (HeaderParseException e) {
            Debug.trace(e);
            fail("unexpected HeaderParseException: " + e.getMessage());
        }

        try {
            String source = "Z_VELOCITY=+7377.421000<m/s>\n";
            Header header = HeaderParser.getInstance().parseHeader("TEST", source.getBytes());
            assertNotNull(header);
            assertEquals(1, header.getNumParams());

            Field param = header.getParam("Z_VELOCITY");
            assertNotNull(param);
            assertNotNull(param.getInfo());
            assertEquals("Z_VELOCITY", param.getInfo().getName());
            assertEquals(ProductData.TYPE_FLOAT64, param.getInfo().getDataType());
            assertEquals(1, param.getInfo().getNumDataElems());
            assertEquals("m/s", param.getInfo().getPhysicalUnit());
            assertEquals(1, param.getNumElems());
            assertEquals(7377.421, param.getElemDouble(0), 1.e-6);
            Debug.trace("header: " + header);
        } catch (HeaderParseException e) {
            Debug.trace(e);
            fail("unexpected HeaderParseException: " + e.getMessage());
        }


        try {
            String source = "COLUMN_SPACING=+2.60000000e+02<m>\n";
            Header header = HeaderParser.getInstance().parseHeader("TEST", source.getBytes());
            assertNotNull(header);
            assertEquals(1, header.getNumParams());

            Field param = header.getParam("COLUMN_SPACING");
            assertNotNull(param);
            assertNotNull(param.getInfo());
            assertEquals("COLUMN_SPACING", param.getInfo().getName());
            assertEquals(ProductData.TYPE_FLOAT64, param.getInfo().getDataType());
            assertEquals(1, param.getInfo().getNumDataElems());
            assertEquals("m", param.getInfo().getPhysicalUnit());
            assertEquals(1, param.getNumElems());
            assertEquals(2.6e2, param.getElemDouble(0), 1.e-6);
            Debug.trace("header: " + header);
        } catch (HeaderParseException e) {
            Debug.trace(e);
            fail("unexpected HeaderParseException: " + e.getMessage());
        }

        try {
            String source = "DS_NAME=\"Cloud measurement parameters\"\n";
            Header header = HeaderParser.getInstance().parseHeader("TEST", source.getBytes());
            assertNotNull(header);
            assertEquals(1, header.getNumParams());

            Field param = header.getParam("DS_NAME");
            assertNotNull(param);
            assertNotNull(param.getInfo());
            assertEquals("DS_NAME", param.getInfo().getName());
            assertEquals(ProductData.TYPE_ASCII, param.getInfo().getDataType());
            assertEquals(28, param.getInfo().getNumDataElems());
            assertEquals(null, param.getInfo().getPhysicalUnit());
            assertEquals(28, param.getNumElems());
            assertEquals("Cloud measurement parameters", param.getAsString());
            Debug.trace("header: " + header);
        } catch (HeaderParseException e) {
            Debug.trace(e);
            fail("unexpected HeaderParseException: " + e.getMessage());
        }

        try {
            String source = "BAND_WAVELEN=+0000412500+0000442500+0000490000+0000510000+0000560000+0000620000+0000665000+0000681250+0000705000+0000753750+0000760625+0000775000+0000865000+0000885000+0000900000<10-3nm>";
            Header header = HeaderParser.getInstance().parseHeader("TEST", source.getBytes());
            assertNotNull(header);
            assertEquals(1, header.getNumParams());

            Field param = header.getParam("BAND_WAVELEN");
            assertEquals(ProductData.TYPE_INT32, param.getInfo().getDataType());
            assertEquals(15, param.getInfo().getNumDataElems());
            assertEquals("10-3nm", param.getInfo().getPhysicalUnit());
            assertEquals(15, param.getNumElems());
            assertEquals(412500, param.getElemInt(0));
            assertEquals(442500, param.getElemInt(1));
            assertEquals(490000, param.getElemInt(2));
            assertEquals(510000, param.getElemInt(3));
            assertEquals(560000, param.getElemInt(4));
            assertEquals(620000, param.getElemInt(5));
            assertEquals(665000, param.getElemInt(6));
            assertEquals(681250, param.getElemInt(7));
            assertEquals(705000, param.getElemInt(8));
            assertEquals(753750, param.getElemInt(9));
            assertEquals(760625, param.getElemInt(10));
            assertEquals(775000, param.getElemInt(11));
            assertEquals(865000, param.getElemInt(12));
            assertEquals(885000, param.getElemInt(13));
            assertEquals(900000, param.getElemInt(14));
            Debug.trace("header: " + header);
        } catch (HeaderParseException e) {
            Debug.trace(e);
            fail("unexpected HeaderParseException: " + e.getMessage());
        }

        try {
            String source = "DS_TYPE=A\nNUM_DSR=+0000036       ";
            Header header = HeaderParser.getInstance().parseHeader("TEST", source.getBytes());
            assertNotNull(header);
            assertEquals(2, header.getNumParams());


            Field param1 = header.getParam("DS_TYPE");
            assertNotNull(param1);
            assertNotNull(param1.getInfo());
            assertEquals("DS_TYPE", param1.getInfo().getName());
            assertEquals(ProductData.TYPE_ASCII, param1.getInfo().getDataType());
            assertEquals(null, param1.getInfo().getPhysicalUnit());
            assertEquals(1, param1.getInfo().getNumDataElems());
            assertEquals("A", param1.getAsString());

            Field param2 = header.getParam("NUM_DSR");
            assertNotNull(header.getParamAt(1));
            assertNotNull(param2.getInfo());
            assertEquals("NUM_DSR", param2.getInfo().getName());
            assertEquals(ProductData.TYPE_INT32, param2.getInfo().getDataType());
            assertEquals(1, param2.getInfo().getNumDataElems());
            assertEquals(null, param2.getInfo().getPhysicalUnit());
            assertEquals(1, param2.getNumElems());
            assertEquals(36, param2.getElemInt(0));

            Debug.trace("header: " + header);
        } catch (HeaderParseException e) {
            Debug.trace(e);
            fail("unexpected HeaderParseException: " + e.getMessage());
        }


    }

    public void testThatInvalidEntriesAreRejected() {
        try {
            String source = "=\"Holla Senor\"";
            HeaderParser.getInstance().parseHeader("TEST", source.getBytes());
            fail("expected HeaderParseException since source is invalid");
        } catch (HeaderParseException e) {
        }

        try {
            String source = "BIBO WAS HERE";
            HeaderParser.getInstance().parseHeader("TEST", source.getBytes());
            fail("expected HeaderParseException since source is invalid");
        } catch (HeaderParseException e) {
        }

        try {
            String source = new String(new byte[]{8, 27, 94, 10, 9, 4, 23, 56, 63, 3, 0, 0, 8, 7, 32, 32, 67});
            HeaderParser.getInstance().parseHeader("TEST", source.getBytes());
            fail("expected HeaderParseException since source is invalid");
        } catch (HeaderParseException expected) {
            // Ok
        }
    }

    public void testThatInvalidNumbersAreHandledAsStringValues() throws HeaderParseException,
                                                                        HeaderEntryNotFoundException {
        Header header;
        header = HeaderParser.getInstance().parseHeader("TEST", "X=+000+".getBytes());
        assertEquals(ProductData.TYPE_ASCII, header.getParamDataType("X"));

        header = HeaderParser.getInstance().parseHeader("TEST", "X=+000+0a".getBytes());
        assertEquals(ProductData.TYPE_ASCII, header.getParamDataType("X"));

        header = HeaderParser.getInstance().parseHeader("TEST", "X=+ 001".getBytes());
    }

    public void testThatNumericDataTypesAreHandledCorrectly() {

        try {
            String source =
                    "AN_UINT32=+4294967295<bytes>\n" +
                    "AN_INT32=-2147483648<bytes>\n" +
                    "AN_UINT16=+65535<bytes>\n" +
                    "AN_INT16=-32768<bytes>\n" +
                    "AN_UINT8=+255<bytes>\n" +
                    "AN_INT8=-128<bytes>\n" +
                    "TRUE=1\n" +
                    "FALSE=0\n";

            Header header = HeaderParser.getInstance().parseHeader("TEST", source.getBytes());
            assertNotNull(header);
            assertEquals(8, header.getNumParams());

            Field param;

            param = header.getParam("AN_UINT32");
            assertNotNull(param);
            assertEquals(ProductData.TYPE_UINT32, param.getDataType());
            assertEquals(4294967295L, param.getElemLong(0));

            param = header.getParam("AN_INT32");
            assertNotNull(param);
            assertEquals(ProductData.TYPE_INT32, param.getDataType());
            assertEquals(-2147483648, param.getElemInt(0));
            assertEquals(-2147483648L, param.getElemLong(0));

            param = header.getParam("AN_UINT16");
            assertNotNull(param);
            assertEquals(ProductData.TYPE_INT32, param.getDataType());
            assertEquals(65535L, param.getElemLong(0));
            assertEquals(65535, param.getElemInt(0));

            param = header.getParam("AN_INT16");
            assertNotNull(param);
            assertEquals(ProductData.TYPE_INT32, param.getDataType());
            assertEquals(-32768L, param.getElemLong(0));
            assertEquals(-32768, param.getElemInt(0));

            param = header.getParam("AN_UINT8");
            assertNotNull(param);
            assertEquals(ProductData.TYPE_INT32, param.getDataType());
            assertEquals(255L, param.getElemLong(0));
            assertEquals(255, param.getElemInt(0));

            param = header.getParam("AN_INT8");
            assertNotNull(param);
            assertEquals(ProductData.TYPE_INT32, param.getDataType());
            assertEquals(-128L, param.getElemLong(0));
            assertEquals(-128, param.getElemInt(0));

            param = header.getParam("TRUE");
            assertNotNull(param);
            assertEquals(ProductData.TYPE_INT8, param.getDataType());
            assertEquals(1L, param.getElemLong(0));
            assertEquals(1, param.getElemInt(0));

            param = header.getParam("FALSE");
            assertNotNull(param);
            assertEquals(ProductData.TYPE_INT8, param.getDataType());
            assertEquals(0L, param.getElemLong(0));
            assertEquals(0, param.getElemInt(0));

        } catch (HeaderParseException e) {
            fail("unexpected HeaderParseException: " + e.getMessage());
        }
    }

    public void testARealLifeMPHExample() throws HeaderParseException {
        StringBuffer sb = new StringBuffer();
        sb.append("PRODUCT=\"MER_FR__2PTACR20000620_104323_00000099X000_00000_00000_0000.N1\"\n");
        sb.append("PROC_STAGE=T\n");
        sb.append("REF_DOC=\"PO-RS-MDA-GS-2009_3/B  \"\n");
        sb.append("\n");
        sb.append("ACQUISITION_STATION=\"ENVISAT SampleData#3\"\n");
        sb.append("PROC_CENTER=\"F-ACRI\"\n");
        sb.append("PROC_TIME=\"22-FEB-2000 19:41:46.000000\"\n");
        sb.append("SOFTWARE_VER=\"MEGS/4.3      \"\n");
        sb.append("\n");
        sb.append("SENSING_START=\"20-JUN-2000 10:43:23.851360\"\n");
        sb.append("SENSING_STOP=\"20-JUN-2000 10:45:02.411360\"\n");
        sb.append("\n");
        sb.append("PHASE=X\n");
        sb.append("CYCLE=+000\n");
        sb.append("REL_ORBIT=+00000\n");
        sb.append("ABS_ORBIT=+00000\n");
        sb.append("STATE_VECTOR_TIME=\"20-JUN-2000 10:06:52.269120\"\n");
        sb.append("DELTA_UT1=+.000000<s>\n");
        sb.append("X_POSITION=-7162215.231<m>\n");
        sb.append("Y_POSITION=+0208912.061<m>\n");
        sb.append("Z_POSITION=-0000004.200<m>\n");
        sb.append("X_VELOCITY=+0056.067000<m/s>\n");
        sb.append("Y_VELOCITY=+1629.960000<m/s>\n");
        sb.append("Z_VELOCITY=+7377.421000<m/s>\n");
        sb.append("VECTOR_SOURCE=\"00\"\n");
        sb.append("\n");
        sb.append("UTC_SBT_TIME=\"20-JUN-2000 06:29:50.343648\"\n");
        sb.append("SAT_BINARY_TIME=+0000000000\n");
        sb.append("CLOCK_STEP=+3906250000<ps>\n");
        sb.append("\n");
        sb.append("LEAP_UTC=\"                           \"\n");
        sb.append("LEAP_SIGN=+000\n");
        sb.append("LEAP_ERR=0\n");
        sb.append("\n");
        sb.append("PRODUCT_ERR=0\n");
        sb.append("TOT_SIZE=+00000000000186478394<bytes>\n");
        sb.append("SPH_SIZE=+0000011622<bytes>\n");
        sb.append("NUM_DSD=+0000000036\n");
        sb.append("DSD_SIZE=+0000000280<bytes>\n");
        sb.append("NUM_DATA_SETS=+0000000023\n");
        sb.append("\n");

        HeaderParser.getInstance().parseHeader("MPH", sb.toString().getBytes());
    }

    public void testARealLifeSPHExample() throws HeaderParseException {
        StringBuffer sb = new StringBuffer();
        sb.append("SPH_DESCRIPTOR=\"Level 2 Full Resolution     \"\n");
        sb.append("STRIPLINE_CONTINUITY_INDICATOR=+000\n");
        sb.append("SLICE_POSITION=+001\n");
        sb.append("NUM_SLICES=+001\n");
        sb.append("FIRST_LINE_TIME=\"20-JUN-2000 10:43:23.827346\"\n");
        sb.append("LAST_LINE_TIME=\"20-JUN-2000 10:45:02.387346\"\n");
        sb.append("FIRST_FIRST_LAT=+0048477538<10-6degN>\n");
        sb.append("FIRST_FIRST_LONG=-0000848029<10-6degE>\n");
        sb.append("FIRST_MID_LAT=+0049120852<10-6degN>\n");
        sb.append("FIRST_MID_LONG=-0004690841<10-6degE>\n");
        sb.append("FIRST_LAST_LAT=+0049633188<10-6degN>\n");
        sb.append("FIRST_LAST_LONG=-0008623846<10-6degE>\n");
        sb.append("LAST_FIRST_LAT=+0042727139<10-6degN>\n");
        sb.append("AST_FIRST_LONG=-0003066400<10-6degE>\n");
        sb.append("LAST_MID_LAT=+0043337479<10-6degN>\n");
        sb.append("LAST_MID_LONG=-0006541594<10-6degE>\n");
        sb.append("LAST_LAST_LAT=+0043840261<10-6degN>\n");
        sb.append("LAST_LAST_LONG=-0010080759<10-6degE>\n");
        sb.append("\n");
        sb.append("TRANS_ERR_FLAG=0\n");
        sb.append("FORMAT_ERR_FLAG=0\n");
        sb.append("DATABASE_FLAG=0\n");
        sb.append("COARSE_ERR_FLAG=0\n");
        sb.append("ECMWF_TYPE=1\n");
        sb.append("NUM_TRANS_ERR=+0000000000\n");
        sb.append("NUM_FORMAT_ERR=+0000000000\n");
        sb.append("TRANS_ERR_THRESH=+0.00000000e+00<%>\n");
        sb.append("FORMAT_ERR_THRESH=+0.00000000e+00<%>\n");
        sb.append("\n");
        sb.append("NUM_BANDS=+015\n");
        sb.append(
                "BAND_WAVELEN=+0000412500+0000442500+0000490000+0000510000+0000560000+0000620000+0000665000+0000681250+0000705000+0000753750+0000760625+0000775000+0000865000+0000885000+0000900000<10-3nm>\n");
        sb.append(
                "BANDWIDTH=+10000+10000+10000+10000+10000+10000+10000+07500+10000+07500+03750+15000+20000+10000+10000<10-3nm>\n");
        sb.append("INST_FOV=+0000019151<10-6deg>\n");
        sb.append("PROC_MODE=0\n");
        sb.append("OFFSET_COMP=1\n");
        sb.append("LINE_TIME_INTERVAL=+0000044000<10-6s>\n");
        sb.append("LINE_LENGTH=+02241<samples>\n");
        sb.append("LINES_PER_TIE_PT=+064\n");
        sb.append("SAMPLES_PER_TIE_PT=+064\n");
        sb.append("COLUMN_SPACING=+2.60000000e+02<m>\n");
        sb.append("\n");

        HeaderParser.getInstance().parseHeader("SPH", sb.toString().getBytes());
    }

    public void testARealLifeDSDExample() throws HeaderParseException {
        StringBuffer sb = new StringBuffer();
        sb.append("DS_NAME=\"Quality ADS                 \"\n");
        sb.append("DS_TYPE=A\n");
        sb.append("FILENAME=\"                                                              \"\n");
        sb.append("DS_OFFSET=+00000000000000012869<bytes>\n");
        sb.append("DS_SIZE=+00000000000000000160<bytes>\n");
        sb.append("NUM_DSR=+0000000005\n");
        sb.append("DSR_SIZE=+0000000032<bytes>\n");
        sb.append("\n");

        HeaderParser.getInstance().parseHeader("DSD(1)", sb.toString().getBytes());
    }


    public final void testGetAsDate() throws HeaderParseException,
                                             HeaderEntryNotFoundException {
        StringBuffer sb = new StringBuffer();
        sb.append("SENSING_START=\"20-JAN-2000 10:43:23.851360\"\n");
        Header header = HeaderParser.getInstance().parseHeader("MPH", sb.toString().getBytes());
        final Date paramDate = header.getParamDate("SENSING_START");
        final long mjd2kOffset = 946684800000L;
        assertEquals(mjd2kOffset + ((((20 - 1) * 24 + 10) * 60 + 43) * 60 + 23) * 1000 + 851, paramDate.getTime());
    }

}
