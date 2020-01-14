package org.esa.snap.core.dataio.geocoding.forward;

import org.esa.snap.core.dataio.geocoding.GeoRaster;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.fail;

public class TiePointForwardTest {

    private TiePointForward forward;

    @Before
    public void setUp() {
        forward = new DummyForward();
    }

    @Test
    public void testCheckGrids_pass() {
        final TiePointGrid lonGrid = new TiePointGrid("lon", 100, 500, 0.5, 0.5, 1.0, 1.0);
        final TiePointGrid latGrid = new TiePointGrid("lat", 100, 500, 0.5, 0.5, 1.0, 1.0);

        forward.checkGrids(lonGrid, latGrid);
    }

    @Test
    public void testCheckGrids_fail_gridWidth() {
        final TiePointGrid lonGrid = new TiePointGrid("lon", 100, 500, 0.5, 0.5, 1.0, 1.0);
        final TiePointGrid latGrid = new TiePointGrid("lat", 101, 500, 0.5, 0.5, 1.0, 1.0);

        try {
            forward.checkGrids(lonGrid, latGrid);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testCheckGrids_fail_gridHeight() {
        final TiePointGrid lonGrid = new TiePointGrid("lon", 100, 500, 0.5, 0.5, 1.0, 1.0);
        final TiePointGrid latGrid = new TiePointGrid("lat", 100, 499, 0.5, 0.5, 1.0, 1.0);

        try {
            forward.checkGrids(lonGrid, latGrid);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testCheckGrids_fail_offsetX() {
        final TiePointGrid lonGrid = new TiePointGrid("lon", 100, 500, 0.51, 0.5, 1.0, 1.0);
        final TiePointGrid latGrid = new TiePointGrid("lat", 100, 500, 0.5, 0.5, 1.0, 1.0);

        try {
            forward.checkGrids(lonGrid, latGrid);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testCheckGrids_fail_offsetY() {
        final TiePointGrid lonGrid = new TiePointGrid("lon", 100, 500, 0.5, 0.5, 1.0, 1.0);
        final TiePointGrid latGrid = new TiePointGrid("lat", 100, 500, 0.5, 0.4999, 1.0, 1.0);

        try {
            forward.checkGrids(lonGrid, latGrid);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testCheckGrids_fail_subsamplingX() {
        final TiePointGrid lonGrid = new TiePointGrid("lon", 100, 500, 0.5, 0.5, 1.01, 1.0);
        final TiePointGrid latGrid = new TiePointGrid("lat", 100, 500, 0.5, 0.5, 1.0, 1.0);

        try {
            forward.checkGrids(lonGrid, latGrid);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testCheckGrids_fail_subsamplingY() {
        final TiePointGrid lonGrid = new TiePointGrid("lon", 100, 500, 0.5, 0.5, 1.0, 1.0);
        final TiePointGrid latGrid = new TiePointGrid("lat", 100, 500, 0.5, 0.5, 1.0, 0.999);

        try {
            forward.checkGrids(lonGrid, latGrid);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testCheckGeoRaster_pass() {
        final GeoRaster geoRaster = new GeoRaster(new double[3], new double[3], 100, 500,
                100, 500, 12.8, 0.5, 0.5, 1.0, 1.0);

        forward.checkGeoRaster(geoRaster);
    }

    @Test
    public void testCheckGeoRaster_fail_gridSize() {
        final GeoRaster geoRaster = new GeoRaster(new double[3], new double[4], 100, 500,
                100, 500, 12.8, 0.5, 0.5, 1.0, 1.0);

        try {
            forward.checkGeoRaster(geoRaster);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    private static class DummyForward extends TiePointForward {
        @Override
        public GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos) {
            return null;
        }

        @Override
        public void initialize(GeoRaster geoRaster, boolean containsAntiMeridian, PixelPos[] poleLocations) {
            throw new RuntimeException("not implemented");
        }

        @Override
        public void dispose() {
        }
    }
}
