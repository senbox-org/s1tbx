package org.esa.beam.dataio.shapefile;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.EOFException;

public class ShapefileTest extends TestCase {


    public void testShapefile() throws IOException {
        try {
            Shapefile.getShapefile("./src/test/resources/shapefile-samples/sedgrabs/sedgrabs.grunt");
            fail("IOException '.shp extension expected' expected");
        } catch (IOException e) {
            // ok
        }

        Shapefile shapefile = Shapefile.getShapefile("shapefile-samples/sedgrabs/sedgrabs.shp");
        assertNotNull(shapefile);
        assertEquals("sedgrabs.shp", shapefile.getMainFile().getName());
        assertEquals("sedgrabs.shx", shapefile.getIndexFile().getName());
        assertEquals("sedgrabs.dbf", shapefile.getDbaseFile().getName());
    }

    public void testReadPointShape() throws IOException {
        Shapefile shapefile = Shapefile.getShapefile(FileFactory.getFile("/shapefile-samples/sedgrabs"));
        try {
            Shapefile.Header header = shapefile.readHeader();
            assertNotNull(header);
            assertEquals(9994, header.fileCode);
            assertEquals(946, header.fileLength);
            assertEquals(1000, header.version);
            assertEquals(Shapefile.Geometry.GT_POINT, header.shapeType);
            assertEquals(331597.01984472736, header.xmin, 1e-10);
            assertEquals(4681705.796398605, header.ymin, 1e-10);
            assertEquals(349475.39837162, header.xmax, 1e-10);
            assertEquals(4699645.082864969, header.ymax, 1e-10);
            assertEquals(0.0, header.zmin, 1e-10);
            assertEquals(0.0, header.zmax, 1e-10);
            assertEquals(0.0, header.mmin, 1e-10);
            assertEquals(0.0, header.mmax, 1e-10);

            Shapefile.Record firstRecord = shapefile.readRecord();
            assertNotNull(firstRecord);
            assertEquals(1, firstRecord.recordNumber);
            assertEquals(10, firstRecord.contentLength);
            assertTrue(firstRecord.geometry instanceof Shapefile.Point);
            final Shapefile.Point firstPoint = (Shapefile.Point) firstRecord.geometry;
            assertEquals(348373.33237328037, firstPoint.x, 1e-10);
            assertEquals(4681705.796398605, firstPoint.y, 1e-10);

            Shapefile.Record lastRecord = getLastRecord(shapefile, 63);
            assertNotNull(lastRecord);
            assertEquals(64, lastRecord.recordNumber);
            assertEquals(10, lastRecord.contentLength);
            assertTrue(lastRecord.geometry instanceof Shapefile.Point);
            final Shapefile.Point lastPoint = (Shapefile.Point) lastRecord.geometry;
            assertEquals(334893.99099277693, lastPoint.x, 1e-10);
            assertEquals(4694033.969894456, lastPoint.y, 1e-10);

        } finally {
            shapefile.close();
        }
    }

    public void testReadPolygonShape() throws IOException {
        Shapefile shapefile = Shapefile.getShapefile(FileFactory.getFile("/shapefile-samples/bottomtype"));
        try {
            Shapefile.Header header = shapefile.readHeader();
            assertNotNull(header);
            assertEquals(9994, header.fileCode);
            assertEquals(453320, header.fileLength);
            assertEquals(1000, header.version);
            assertEquals(Shapefile.Geometry.GT_POLYGON, header.shapeType);
            assertEquals(331048.5100066487, header.xmin, 1e-10);
            assertEquals(4680270.509783089, header.ymin, 1e-10);
            assertEquals(350143.73828043253, header.xmax, 1e-10);
            assertEquals(4700000.489995284, header.ymax, 1e-10);
            assertEquals(0.0, header.zmin, 1e-10);
            assertEquals(0.0, header.zmax, 1e-10);
            assertEquals(0.0, header.mmin, 1e-10);
            assertEquals(0.0, header.mmax, 1e-10);

            Shapefile.Record firstRecord = shapefile.readRecord();
            assertNotNull(firstRecord);
            assertEquals(1, firstRecord.recordNumber);
            assertEquals(32416, firstRecord.contentLength);
            assertTrue(firstRecord.geometry instanceof Shapefile.Polygon);
            final Shapefile.Polygon firstPolygon = (Shapefile.Polygon) firstRecord.geometry;
            assertEquals(4043, firstPolygon.numPoints);
            assertEquals(331330.9999909118, firstPolygon.points[0].x, 1e-10);
            assertEquals(4692193.000000373, firstPolygon.points[0].y, 1e-10);
            assertEquals(338850.8329259152, firstPolygon.points[4042].x, 1e-10);
            assertEquals(4683685.63096845, firstPolygon.points[4042].y, 1e-10);
            assertEquals(25, firstPolygon.numParts);
            assertEquals(0, firstPolygon.parts[0]);
            assertEquals(2706, firstPolygon.parts[1]);
            assertEquals(2725, firstPolygon.parts[2]);
            assertEquals(3711, firstPolygon.parts[21]);
            assertEquals(3840, firstPolygon.parts[23]);
            assertEquals(4034, firstPolygon.parts[24]);

            Shapefile.Record lastRecord = getLastRecord(shapefile, 5);
            assertNotNull(lastRecord);
            assertEquals(6, lastRecord.recordNumber);
            assertEquals(68644, lastRecord.contentLength);
            assertTrue(lastRecord.geometry instanceof Shapefile.Polygon);
            final Shapefile.Polygon lastPolygon = (Shapefile.Polygon) lastRecord.geometry;
            assertEquals(8550, lastPolygon.numPoints);
            assertEquals(334905.65772224095, lastPolygon.points[0].x, 1e-10);
            assertEquals(4690244.781248379, lastPolygon.points[0].y, 1e-10);
            assertEquals(334465.9999938315, lastPolygon.points[7851].x, 1e-10);
            assertEquals(4690318.000000447, lastPolygon.points[7851].y, 1e-10);
            assertEquals(111, lastPolygon.numParts);
            assertEquals(0, lastPolygon.parts[0]);
            assertEquals(8546, lastPolygon.parts[110]);

        } finally {
            shapefile.close();
        }
    }

    public void testReadPolyLineShape() throws IOException {
        Shapefile shapefile = Shapefile.getShapefile(FileFactory.getFile("/shapefile-samples/surveylines_sss"));
        try {
            Shapefile.Header header = shapefile.readHeader();
            assertNotNull(header);
            assertEquals(9994, header.fileCode);
            assertEquals(438170, header.fileLength);
            assertEquals(1000, header.version);
            assertEquals(Shapefile.Geometry.GT_POLYLINE, header.shapeType);
            assertEquals(330921.8799990894, header.xmin, 1e-10);
            assertEquals(4679933.510006693, header.ymin, 1e-10);
            assertEquals(350057.2300009106, header.xmax, 1e-10);
            assertEquals(4699917.389993305, header.ymax, 1e-10);
            assertEquals(0.0, header.zmin, 1e-10);
            assertEquals(0.0, header.zmax, 1e-10);
            assertEquals(0.0, header.mmin, 1e-10);
            assertEquals(0.0, header.mmax, 1e-10);

            Shapefile.Record firstRecord = shapefile.readRecord();
            assertNotNull(firstRecord);
            assertEquals(1, firstRecord.recordNumber);
            assertEquals(120, firstRecord.contentLength);
            assertTrue(firstRecord.geometry instanceof Shapefile.Polyline);
            final Shapefile.Polyline firstPolyline = (Shapefile.Polyline) firstRecord.geometry;
            assertEquals(12, firstPolyline.numPoints);
            assertEquals(331082.43999923894, firstPolyline.points[0].x, 1e-10);
            assertEquals(4692970.829986836, firstPolyline.points[0].y, 1e-10);
            assertEquals(330966.93001513137, firstPolyline.points[11].x, 1e-10);
            assertEquals(4692774.009986652, firstPolyline.points[11].y, 1e-10);
            assertEquals(1, firstPolyline.numParts);
            assertEquals(0, firstPolyline.parts[0]);

            Shapefile.Record lastRecord = getLastRecord(shapefile, 1677);
            assertNotNull(lastRecord);
            assertEquals(1678, lastRecord.recordNumber);
            assertEquals(200, lastRecord.contentLength);
            assertTrue(lastRecord.geometry instanceof Shapefile.Polyline);
            final Shapefile.Polyline lastPolyline = (Shapefile.Polyline) lastRecord.geometry;
            assertEquals(22, lastPolyline.numPoints);
            assertEquals(340920.100008401, lastPolyline.points[0].x, 1e-10);
            assertEquals(4694739.700004483, lastPolyline.points[0].y, 1e-10);
            assertEquals(340925.7000084062, lastPolyline.points[11].x, 1e-10);
            assertEquals(4695179.489988892, lastPolyline.points[11].y, 1e-10);
            assertEquals(1, lastPolyline.numParts);
            assertEquals(0, lastPolyline.parts[0]);

        } finally {
            shapefile.close();
        }
    }

    private Shapefile.Record getLastRecord(Shapefile shapefile, int expecetedNumRemaining) throws IOException {
        Shapefile.Record lastRecord = null;
        int numRemaining = 0;
        while (true) {
            try {
                lastRecord = shapefile.readRecord();
                numRemaining++;
            } catch (EOFException e) {
                break;
            }
        }
        assertEquals(expecetedNumRemaining, numRemaining);
        return lastRecord;
    }
}


