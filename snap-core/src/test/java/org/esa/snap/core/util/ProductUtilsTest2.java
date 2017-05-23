package org.esa.snap.core.util;

import org.esa.snap.core.datamodel.GeoPos;
import org.junit.Before;
import org.junit.Test;

import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.*;


/**
 * @author Marco Peters
 */
public class ProductUtilsTest2 {

    private GeoPos[] geoPoints;

    @Before
    public void setUp() throws Exception {
        URL resource = ProductUtilsTest2.class.getResource("geoPositions.txt");
        Stream<String> stringStream = Files.lines(Paths.get(resource.toURI()));
        ArrayList<GeoPos> posList = new ArrayList<>();
        stringStream.forEachOrdered(s -> {
            if (s.startsWith("#")) {
                return;
            }
            String[] split = s.split(";");
            if (split != null && split.length == 2) {
                posList.add(new GeoPos(Double.parseDouble(split[0].trim()), Double.parseDouble(split[1].trim())));
            }
        });
        geoPoints = posList.toArray(new GeoPos[0]);
    }

    @Test
    public void testAssemblePathList() {
        ArrayList<GeneralPath> generalPaths = ProductUtils.assemblePathList(geoPoints);

        assertEquals(3, generalPaths.size());
        for (int i = 0; i < generalPaths.size(); i++) {
            int segCloseCount = getSegTypeCount(generalPaths.get(i), PathIterator.SEG_CLOSE);
            assertEquals(String.format("General path [%d] contains to many close segments", i), 1, segCloseCount);
        }
    }

    @Test
    public void testFillPath() {
        GeneralPath path = new GeneralPath(Path2D.WIND_NON_ZERO);
        ProductUtils.fillPath(geoPoints, path);

        int segCloseCount = getSegTypeCount(path, PathIterator.SEG_CLOSE);
        assertEquals("path contains to many close segments", 1, segCloseCount);
        int segMoveToCount = getSegTypeCount(path, PathIterator.SEG_MOVETO);
        assertEquals("path contains to many move-to segments", 1, segMoveToCount);
        int segLineToCount = getSegTypeCount(path, PathIterator.SEG_LINETO);
        assertEquals("path contains to many line-to segments", geoPoints.length - segCloseCount, segLineToCount);
    }

    @Test
    public void testAreaToPath() {
        GeneralPath path = new GeneralPath(Path2D.WIND_NON_ZERO);
        ProductUtils.fillPath(geoPoints, path);

        Area area = new Area(path);
        assertFalse(area.isSingular());
        GeneralPath areaPath = ProductUtils.areaToPath(area, 0);
        int segCloseCount = getSegTypeCount(areaPath, PathIterator.SEG_CLOSE);
        assertEquals("area path contains to many close segments", 2, segCloseCount);
    }

    @Test
    public void testAreaToSubPaths() throws Exception {
        GeneralPath path = new GeneralPath(Path2D.WIND_NON_ZERO);
        ProductUtils.fillPath(geoPoints, path);

        Area area = new Area(path);
        assertFalse(area.isSingular());
        List<GeneralPath> areaPaths = ProductUtils.areaToSubPaths(area, 0);
        assertEquals(2, areaPaths.size());
        for (int i = 0; i < areaPaths.size(); i++) {
            GeneralPath areaPath = areaPaths.get(i);
            int segCloseCount = getSegTypeCount(areaPath, PathIterator.SEG_CLOSE);
            assertEquals(String.format("area path [%d] contains to many close segments", i), 1, segCloseCount);

        }
    }

    private int getSegTypeCount(GeneralPath path, int segType) {
        PathIterator pathIterator = path.getPathIterator(new AffineTransform());
        int segTypeCnt = 0;
        while (!pathIterator.isDone()) {
            pathIterator.next();
            float[] coords = new float[6];
            int currentSegType = pathIterator.currentSegment(coords);
            if (currentSegType == segType) {
                segTypeCnt++;
            }
        }
        return segTypeCnt;
    }

}