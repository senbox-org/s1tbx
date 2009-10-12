package org.esa.beam.framework.dataop.barithm;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.jexp.Term;
import static org.junit.Assert.*;
import org.junit.Ignore;
import org.junit.Test;

import java.awt.Point;
import java.io.IOException;
import java.util.ArrayList;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
public class RasterDataLoopTest {

    @Ignore
    @Test
    public void testForEachPixel() throws IOException {
        final ArrayList<Integer> pixelIndexList = new ArrayList<Integer>();
        final ArrayList<Integer> elemIndexList = new ArrayList<Integer>();
        final ArrayList<Point> pixelPosList = new ArrayList<Point>();
        RasterDataLoop loop = new RasterDataLoop(512, 600, 2, 2, new Term[]{}, ProgressMonitor.NULL);
        loop.forEachPixel(new RasterDataLoop.Body() {
            @Override
            public void eval(RasterDataEvalEnv env, int pixelIndex) {
                pixelIndexList.add(pixelIndex);
                elemIndexList.add(env.getElemIndex());
                pixelPosList.add(new Point(env.getPixelX(), env.getPixelY()));
            }
        });

        final Integer[] pixelIndexes = pixelIndexList.toArray(new Integer[pixelIndexList.size()]);
        assertArrayEquals(new Integer[]{0,1,2,3}, pixelIndexes);
        final Integer[] elemIndexes = elemIndexList.toArray(new Integer[elemIndexList.size()]);
        assertArrayEquals(new Integer[]{0,1,2,3}, elemIndexes);
        final Point[] pixelPositions = pixelPosList.toArray(new Point[pixelPosList.size()]);
        final Point[] expectedPixelPositions = {
                new Point(512, 600), new Point(513, 600),
                new Point(512, 601), new Point(513, 601)
        };
        assertArrayEquals(expectedPixelPositions, pixelPositions);
    }
}
