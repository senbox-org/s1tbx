package org.esa.snap.binning.support;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class RotatedLatLonGridTest_GetCenterLatLon_GetBinIndex {

    private RotatedLatLonGrid rotatedLatLonGrid;
    private PlateCarreeGrid plate_Carree_Grid;

    @Before
    public void setUp() throws Exception {
        rotatedLatLonGrid = new RotatedLatLonGrid(180, 10, 20);
        plate_Carree_Grid = new PlateCarreeGrid(180);
    }

    @Test
    public void testCorners() throws Exception {
        final long indexUL = 0;
        final long indexUR = indexUL + 359;
        final long indexLL = 360 * 179;
        final long indexLR = indexLL + 359;

        assertThat(plate_Carree_Grid.getCenterLatLon(indexUL), equalTo(new double[]{89.5, -179.5}));
        assertThat(plate_Carree_Grid.getCenterLatLon(indexUR), equalTo(new double[]{89.5, 179.5}));
        assertThat(plate_Carree_Grid.getCenterLatLon(indexLL), equalTo(new double[]{-89.5, -179.5}));
        assertThat(plate_Carree_Grid.getCenterLatLon(indexLR), equalTo(new double[]{-89.5, 179.5}));

        final double[] cllUL = rotatedLatLonGrid.getCenterLatLon(indexUL);
        final double[] cllUR = rotatedLatLonGrid.getCenterLatLon(indexUR);
        final double[] cllLL = rotatedLatLonGrid.getCenterLatLon(indexLL);
        final double[] cllLR = rotatedLatLonGrid.getCenterLatLon(indexLR);
        assertThat(rotatedLatLonGrid.getBinIndex(cllUL[0], cllUL[1]), equalTo(indexUL));
        assertThat(rotatedLatLonGrid.getBinIndex(cllUR[0], cllUR[1]), equalTo(indexUR));
        assertThat(rotatedLatLonGrid.getBinIndex(cllLL[0], cllLL[1]), equalTo(indexLL));
        assertThat(rotatedLatLonGrid.getBinIndex(cllLR[0], cllLR[1]), equalTo(indexLR));
    }

    @Test
    public void testAroundCenterPos() throws Exception {
        final long index1 = 360 * 89 + 179;
        final long index2 = index1 + 1;
        final long index3 = index2 + 359;
        final long index4 = index3 + 1;

        assertThat(plate_Carree_Grid.getCenterLatLon(index1), equalTo(new double[]{0.5, -0.5}));
        assertThat(plate_Carree_Grid.getCenterLatLon(index2), equalTo(new double[]{0.5, 0.5}));
        assertThat(plate_Carree_Grid.getCenterLatLon(index3), equalTo(new double[]{-0.5, -0.5}));
        assertThat(plate_Carree_Grid.getCenterLatLon(index4), equalTo(new double[]{-0.5, 0.5}));

        final double[] centerLatLon1 = rotatedLatLonGrid.getCenterLatLon(index1);
        final double[] centerLatLon2 = rotatedLatLonGrid.getCenterLatLon(index2);
        final double[] centerLatLon3 = rotatedLatLonGrid.getCenterLatLon(index3);
        final double[] centerLatLon4 = rotatedLatLonGrid.getCenterLatLon(index4);
        assertThat(rotatedLatLonGrid.getBinIndex(centerLatLon1[0], centerLatLon1[1]), equalTo(index1));
        assertThat(rotatedLatLonGrid.getBinIndex(centerLatLon2[0], centerLatLon2[1]), equalTo(index2));
        assertThat(rotatedLatLonGrid.getBinIndex(centerLatLon3[0], centerLatLon3[1]), equalTo(index3));
        assertThat(rotatedLatLonGrid.getBinIndex(centerLatLon4[0], centerLatLon4[1]), equalTo(index4));
        assertThat(rotatedLatLonGrid.getBinIndex(10.5, 19.5), equalTo(index1));
        assertThat(rotatedLatLonGrid.getBinIndex(10.5, 20.5), equalTo(index2));
        assertThat(rotatedLatLonGrid.getBinIndex(9.5, 19.5), equalTo(index3));
        assertThat(rotatedLatLonGrid.getBinIndex(9.5, 20.5), equalTo(index4));
    }

    @Test
    public void testIndexOutOfBounds() throws Exception {
        final int indexLL = 360 * 179;
        final int indexLR = indexLL + 359;
        final int indexOutOfBBounds = indexLR + 1;

        try {
            plate_Carree_Grid.getCenterLatLon(indexOutOfBBounds);
            fail("ArrayIndexOutOfBoundException expected");
        } catch (ArrayIndexOutOfBoundsException expected) {

        } catch (Exception e) {
            fail("ArrayIndexOutOfBoundException expected");
        }
        try {
            rotatedLatLonGrid.getCenterLatLon(indexOutOfBBounds);
            fail("ArrayIndexOutOfBoundException expected");
        } catch (ArrayIndexOutOfBoundsException expected) {

        } catch (Exception e) {
            fail("ArrayIndexOutOfBoundException expected");
        }
    }
}
