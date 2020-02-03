package org.esa.snap.core.util;

import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.test.LongTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.awt.geom.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@RunWith(LongTestRunner.class)
public class GeoUtilsIOTest {

    private GeoPos[] geoPoints;

    @Before
    public void setUp() throws Exception {
        final URL resource = GeoUtilsIOTest.class.getResource("geoPositions.txt");
        final Stream<String> stringStream = Files.lines(Paths.get(resource.toURI()));

        final ArrayList<GeoPos> posList = new ArrayList<>();

        stringStream.forEachOrdered(s -> {
            if (s.startsWith("#")) {
                return;
            }
            String[] split = s.split(";");
            if (split.length == 2) {
                posList.add(new GeoPos(Double.parseDouble(split[0].trim()), Double.parseDouble(split[1].trim())));
            }
        });

        geoPoints = posList.toArray(new GeoPos[0]);
    }

    @Test
    public void testAreaToSubPaths() {
        final GeneralPath path = new GeneralPath(Path2D.WIND_NON_ZERO);
        GeoUtils.fillPath(geoPoints, path);

        final Area area = new Area(path);
        assertFalse(area.isSingular());

        final List<GeneralPath> areaPaths = GeoUtils.areaToSubPaths(area, 0);
        assertEquals(2, areaPaths.size());

        for (int i = 0; i < areaPaths.size(); i++) {
            final GeneralPath areaPath = areaPaths.get(i);
            final int segCloseCount = getSegTypeCount(areaPath, PathIterator.SEG_CLOSE);
            assertEquals(String.format("area path [%d] contains to many close segments", i), 1, segCloseCount);
        }
    }

    @Test
    public void testFillPath() {
        final GeneralPath path = new GeneralPath(Path2D.WIND_NON_ZERO);

        GeoUtils.fillPath(geoPoints, path);

        final int segCloseCount = getSegTypeCount(path, PathIterator.SEG_CLOSE);
        assertEquals("path contains too many close segments", 1, segCloseCount);

        final int segMoveToCount = getSegTypeCount(path, PathIterator.SEG_MOVETO);
        assertEquals("path contains too many move-to segments", 1, segMoveToCount);

        final int segLineToCount = getSegTypeCount(path, PathIterator.SEG_LINETO);
        assertEquals("path contains too many line-to segments", geoPoints.length - segCloseCount, segLineToCount);
    }

    private int getSegTypeCount(GeneralPath path, int seqType) {
        final float[] coords = new float[6];
        final PathIterator pathIterator = path.getPathIterator(new AffineTransform());

        int segTypeCnt = 0;
        while (!pathIterator.isDone()) {
            pathIterator.next();
            final int currentSegType = pathIterator.currentSegment(coords);
            if (currentSegType == seqType) {
                segTypeCnt++;
            }
        }
        return segTypeCnt;
    }
}
