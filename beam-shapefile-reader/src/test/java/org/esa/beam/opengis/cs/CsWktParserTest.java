package org.esa.beam.opengis.cs;

import junit.framework.TestCase;

import java.text.ParseException;
import java.io.InputStreamReader;
import java.io.IOException;

import org.esa.beam.opengis.ct.ParamMathTransform;
import org.esa.beam.opengis.ct.MathTransform;
import org.esa.beam.opengis.ct.InverseMathTransform;
import org.esa.beam.opengis.ct.ConcatMathTransform;

public class CsWktParserTest extends TestCase {
    public void testMt() throws ParseException {
        String wkt = "PARAM_MT[\"sin\",PARAMETER[\"x\",0.5]]";
        MathTransform mt = CsWktParser.parseMt(wkt);
        assertTrue(mt instanceof ParamMathTransform);
        ParamMathTransform paramMt = (ParamMathTransform) mt;
        assertEquals("sin", paramMt.name);
        assertEquals(1, paramMt.parameters.length);
        assertEquals("x", paramMt.parameters[0].name);
        assertEquals(0.5, paramMt.parameters[0].value, 1e-10);
    }

    public void testBigMt() throws ParseException, IOException {
        InputStreamReader reader = new InputStreamReader(CsWktParserTest.class.getResourceAsStream("/mt-test.wkt"));
        MathTransform mt = CsWktParser.parseMt(reader);

        assertTrue(mt instanceof ConcatMathTransform);
        ConcatMathTransform cmt = (ConcatMathTransform) mt;
        assertEquals(8, cmt.mts.length);
        assertTrue(cmt.mts[0] instanceof ParamMathTransform);
        assertTrue(cmt.mts[1] instanceof InverseMathTransform);
        assertTrue(cmt.mts[2] instanceof ParamMathTransform);
        assertTrue(cmt.mts[3] instanceof ParamMathTransform);
        assertTrue(cmt.mts[4] instanceof ParamMathTransform);
        assertTrue(cmt.mts[5] instanceof ParamMathTransform);
        assertTrue(cmt.mts[6] instanceof ParamMathTransform);
        assertTrue(cmt.mts[7] instanceof ParamMathTransform);
    }

    public void testBigCs() throws ParseException, IOException {
        InputStreamReader reader = new InputStreamReader(CsWktParserTest.class.getResourceAsStream("/cs-test.wkt"));
        CoordinateSystem cs = CsWktParser.parseCs(reader);

        assertTrue(cs instanceof CompoundCoordinateSystem);
        CompoundCoordinateSystem ccs = (CompoundCoordinateSystem) cs;

        assertTrue(ccs.headCs instanceof ProjectedCoordinateSystem);
        ProjectedCoordinateSystem pcs = (ProjectedCoordinateSystem) ccs.headCs;
        assertEquals("OSGB 1936 / British National Grid", pcs.name);
        assertNotNull(pcs.axes);
        assertEquals(2, pcs.axes.length);
        assertEquals("E", pcs.axes[0].name);
        assertEquals(AxisInfo.Orientation.EAST, pcs.axes[0].orientation);
        assertEquals("N", pcs.axes[1].name);
        assertEquals(AxisInfo.Orientation.NORTH, pcs.axes[1].orientation);
        assertNotNull(pcs.projection);
        assertEquals("Transverse_Mercator", pcs.projection.name);
        assertNotNull(pcs.projection.parameters);
        assertEquals(5, pcs.projection.parameters.length);
        assertEquals("latitude_of_origin", pcs.projection.parameters[0].name);
        assertEquals(49.0, pcs.projection.parameters[0].value, 1e-8);
        assertEquals("scale_factor", pcs.projection.parameters[2].name);
        assertEquals(0.999601272, pcs.projection.parameters[2].value, 1e-8);

        assertTrue(ccs.tailCs instanceof VerticalCoordinateSystem);
        VerticalCoordinateSystem vcs = (VerticalCoordinateSystem) ccs.tailCs;
        assertEquals("Newlyn", vcs.name);
        assertEquals("Up", vcs.axisInfo.name);
        assertEquals(AxisInfo.Orientation.UP, vcs.axisInfo.orientation);
        assertEquals("Ordnance Datum Newlyn", vcs.verticalDatum.name);
        assertEquals(2005, vcs.verticalDatum.datumType);
    }
}

