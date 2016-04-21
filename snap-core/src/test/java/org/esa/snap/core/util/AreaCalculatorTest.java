package org.esa.snap.core.util;

import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Before;
import org.junit.Test;

import java.awt.geom.Rectangle2D;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 */
public class AreaCalculatorTest {

    private static final double SIZE_FIRST_LAST_LINE = 1.0813948123130877E8;
    private static final double SIZE_CENTER_LINE = 1.2391557179979538E10;

    private AreaCalculator areaCalculator;

    @Before
    public void setUp() throws Exception {
        CrsGeoCoding gc = new CrsGeoCoding(DefaultGeographicCRS.WGS84, 360, 180, -179.5, 89.5, 1.0, 1.0);
        areaCalculator = new AreaCalculator(gc);
    }

    @Test
    public void testCalculateRectangleSize() throws Exception {
        Rectangle2D.Double currentRect = new Rectangle2D.Double();

        currentRect.setFrameFromDiagonal(-180, 90, -179, 89);
        assertEquals(SIZE_FIRST_LAST_LINE, areaCalculator.calculateRectangleSize(currentRect), 1.0e-6);
        currentRect.setFrameFromDiagonal(0, 90, 1, 89);
        assertEquals(SIZE_FIRST_LAST_LINE, areaCalculator.calculateRectangleSize(currentRect), 1.0e-6);
        currentRect.setFrameFromDiagonal(179, 90, 180, 89);
        assertEquals(SIZE_FIRST_LAST_LINE, areaCalculator.calculateRectangleSize(currentRect), 1.0e-6);

        currentRect.setFrameFromDiagonal(-180, -1, -179, 0);
        assertEquals(SIZE_CENTER_LINE, areaCalculator.calculateRectangleSize(currentRect), 1.0e-6);
        currentRect.setFrameFromDiagonal(0, -1, 1, 0);
        assertEquals(SIZE_CENTER_LINE, areaCalculator.calculateRectangleSize(currentRect), 1.0e-6);
        currentRect.setFrameFromDiagonal(179, -1, 180, 0);
        assertEquals(SIZE_CENTER_LINE, areaCalculator.calculateRectangleSize(currentRect), 1.0e-6);

        currentRect.setFrameFromDiagonal(-180, -89, -179, -90);
        assertEquals(SIZE_FIRST_LAST_LINE, areaCalculator.calculateRectangleSize(currentRect), 1.0e-6);
        currentRect.setFrameFromDiagonal(0, -89, 1, -90);
        assertEquals(SIZE_FIRST_LAST_LINE, areaCalculator.calculateRectangleSize(currentRect), 1.0e-6);
        currentRect.setFrameFromDiagonal(179, -89, 180, -90);
        assertEquals(SIZE_FIRST_LAST_LINE, areaCalculator.calculateRectangleSize(currentRect), 1.0e-6);
    }

    @Test
    public void testCalculatePixelSize() throws Exception {
        Rectangle2D.Double currentRect = new Rectangle2D.Double();

        currentRect.setFrameFromDiagonal(-180, 90, -179, 89);
        assertEquals(SIZE_FIRST_LAST_LINE, areaCalculator.calculatePixelSize(0, 0), 1.0e-6);
        currentRect.setFrameFromDiagonal(0, 90, 1, 89);
        assertEquals(SIZE_FIRST_LAST_LINE, areaCalculator.calculatePixelSize(180, 0), 1.0e-6);
        currentRect.setFrameFromDiagonal(179, 90, 180, 89);
        assertEquals(SIZE_FIRST_LAST_LINE, areaCalculator.calculatePixelSize(359, 0), 1.0e-6);

        currentRect.setFrameFromDiagonal(-180, -1, -179, 0);
        assertEquals(SIZE_CENTER_LINE, areaCalculator.calculatePixelSize(0, 90), 1.0e-6);
        currentRect.setFrameFromDiagonal(0, -1, 1, 0);
        assertEquals(SIZE_CENTER_LINE, areaCalculator.calculatePixelSize(180, 90), 1.0e-6);
        currentRect.setFrameFromDiagonal(179, -1, 180, 0);
        assertEquals(SIZE_CENTER_LINE, areaCalculator.calculatePixelSize(359, 90), 1.0e-6);

        currentRect.setFrameFromDiagonal(-180, -89, -179, -90);
        assertEquals(SIZE_FIRST_LAST_LINE, areaCalculator.calculatePixelSize(0, 179), 1.0e-6);
        currentRect.setFrameFromDiagonal(0, -89, 1, -90);
        assertEquals(SIZE_FIRST_LAST_LINE, areaCalculator.calculatePixelSize(180, 179), 1.0e-6);
        currentRect.setFrameFromDiagonal(179, -89, 180, -90);
        assertEquals(SIZE_FIRST_LAST_LINE, areaCalculator.calculatePixelSize(359, 179), 1.0e-6);
    }

    @Test
    public void testCreateGeoRectangleForPixel() throws Exception {
        Rectangle2D.Double currentRect = new Rectangle2D.Double();

        currentRect.setFrameFromDiagonal(-180, 90, -179, 89);
        assertEquals(currentRect, areaCalculator.createGeoRectangleForPixel(0, 0));
        currentRect.setFrameFromDiagonal(0, 90, 1, 89);
        assertEquals(currentRect, areaCalculator.createGeoRectangleForPixel(180, 0));
        currentRect.setFrameFromDiagonal(179, 90, 180, 89);
        assertEquals(currentRect, areaCalculator.createGeoRectangleForPixel(359, 0));

        currentRect.setFrameFromDiagonal(-180, -1, -179, 0);
        assertEquals(currentRect, areaCalculator.createGeoRectangleForPixel(0, 90));
        currentRect.setFrameFromDiagonal(0, -1, 1, 0);
        assertEquals(currentRect, areaCalculator.createGeoRectangleForPixel(180, 90));
        currentRect.setFrameFromDiagonal(179, -1, 180, 0);
        assertEquals(currentRect, areaCalculator.createGeoRectangleForPixel(359, 90));

        currentRect.setFrameFromDiagonal(-180, -89, -179, -90);
        assertEquals(currentRect, areaCalculator.createGeoRectangleForPixel(0, 179));
        currentRect.setFrameFromDiagonal(0, -89, 1, -90);
        assertEquals(currentRect, areaCalculator.createGeoRectangleForPixel(180, 179));
        currentRect.setFrameFromDiagonal(179, -89, 180, -90);
        assertEquals(currentRect, areaCalculator.createGeoRectangleForPixel(359, 179));
    }

}