package org.esa.beam.dataio.shapefile;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;

public class Shapefile {
    public static final double MEASURE_MIN = -1.0E38;

    private File mainFile;
    private File indexFile;
    private File dbaseFile;
    private FileImageInputStream stream;
    private Header header;

    // todo - parse also the *.prj file. Uses OGC WKT, e.g.
    // PROJCS["WGS_1984_UTM_Zone_19N",
    //        GEOGCS["GCS_WGS_1984",
    //               DATUM["D_WGS_1984",
    //                     SPHEROID["WGS_1984",
    //                              6378137.0,298.257223563]
    //               ],
    //               PRIMEM["Greenwich",0.0],
    //               UNIT["Degree",0.0174532925199433]
    //        ],
    //        PROJECTION["Transverse_Mercator"],
    //        PARAMETER["False_Easting",500000.0],
    //        PARAMETER["False_Northing",0.0],
    //        PARAMETER["Central_Meridian",-69.0],
    //        PARAMETER["Scale_Factor",0.9996],
    //        PARAMETER["Latitude_Of_Origin",0.0],
    //        UNIT["Meter",1.0]
    // ]

    public static Shapefile getShapefile(String filePath) throws IOException {
        return getShapefile(new File(filePath));
    }

    public static Shapefile getShapefile(File file) throws IOException {
        if (file.isDirectory()) {
           String nameBase = file.getName();
            File mainFile = new File(file, nameBase + ".shp");
            File indexFile = new File(file, nameBase + ".shx");
            File dbaseFile = new File(file, nameBase + ".dbf");
            return new Shapefile(mainFile, indexFile, dbaseFile);
        } else {
            String name = file.getName().toLowerCase();
            if (name.endsWith(".shp")) {
                String nameBase = name.substring(0, name.length() - ".shp".length());
                File indexFile = new File(nameBase + ".shx");
                File dbaseFile = new File(nameBase + ".dbf");
                return new Shapefile(file, indexFile, dbaseFile);
            } else {
                throw new IOException(".shp extension expected");
            }
        }
    }

    public Shapefile(File mainFile, File indexFile, File dbaseFile) {
        this.mainFile = mainFile;
        this.indexFile = indexFile;
        this.dbaseFile = dbaseFile;
    }

    public File getMainFile() {
        return mainFile;
    }

    public File getIndexFile() {
        return indexFile;
    }

    public File getDbaseFile() {
        return dbaseFile;
    }


    public Header readHeader() throws IOException {
        openRead();
        if (header == null) {
            header = new Header();
            header.readFrom(stream);
        }
        return header;
    }

    public void close() throws IOException {
        if (stream != null) {
            stream.close();
            stream = null;
        }
    }

    public Record readRecord() throws IOException {
        readHeader();
        Record record = new Record();
        record.readFrom(stream);
        return record;
    }

    private void openRead() throws IOException {
        if (stream == null) {
            stream = new FileImageInputStream(mainFile);
        }
    }

    public static class Header {

        public int fileCode, fileLength, version, shapeType;
        public double xmin, ymin, xmax, ymax, zmin, zmax, mmin, mmax;

        public void readFrom(ImageInputStream stream) throws IOException {

            stream.setByteOrder(ByteOrder.BIG_ENDIAN);
            // Position Field Value Type Order
            fileCode = stream.readInt(); // Byte 0 File Code 9994 Integer Big
            stream.readInt(); // Byte 4 Unused 0 Integer Big
            stream.readInt(); // Byte 8 Unused 0 Integer Big
            stream.readInt(); // Byte 12 Unused 0 Integer Big
            stream.readInt(); // Byte 16 Unused 0 Integer Big
            stream.readInt(); // Byte 20 Unused 0 Integer Big
            fileLength = stream.readInt(); // Byte 24 File Length File Length Integer Big

            stream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            // Position Field Value Type Order
            version = stream.readInt(); // Byte 28 Version 1000 Integer Little
            shapeType = stream.readInt(); // Byte 32 Shape Type Shape Type Integer Little
            xmin = stream.readDouble(); // Byte 36 Bounding Box Xmin Double Little
            ymin = stream.readDouble(); // Byte 44 Bounding Box Ymin Double Little
            xmax = stream.readDouble(); // Byte 52 Bounding Box Xmax Double Little
            ymax = stream.readDouble(); // Byte 60 Bounding Box Ymax Double Little
            zmin = stream.readDouble(); // Byte 68* Bounding Box Zmin Double Little
            zmax = stream.readDouble(); // Byte 76* Bounding Box Zmax Double Little
            mmin = stream.readDouble(); // Byte 84* Bounding Box Mmin Double Little
            mmax = stream.readDouble(); // Byte 92* Bounding Box Mmax Double Little
        }
    }

    public static class Record {
        public int recordNumber, contentLength, shapeType;
        public Geometry geometry;

        public void readFrom(ImageInputStream stream) throws IOException {
            readRecordHeader(stream);
            readRecordContent(stream);
        }

        private void readRecordHeader(ImageInputStream stream) throws IOException {
            stream.setByteOrder(ByteOrder.BIG_ENDIAN);
            // Position Field Value Type Order
            recordNumber = stream.readInt(); // Byte 0 Record Number Record Number Integer Big
            contentLength = stream.readInt(); // Byte 4 Content Length Content Length Integer Big
        }

        private void readRecordContent(ImageInputStream stream) throws IOException {
            stream.setByteOrder(ByteOrder.LITTLE_ENDIAN);

            // Position Field Value Type Order
            shapeType = stream.readInt(); // Byte 0 Record Number Record Number Integer Big

            Geometry geometry;
            switch (shapeType) {
                case Geometry.GT_NULL:
                    geometry = new NullShape();
                    break;
                case Geometry.GT_POINT:
                    geometry = readPoint(stream);
                    break;
                case Geometry.GT_POLYLINE:
                    geometry = readPolyLine(stream);
                    break;
                case Geometry.GT_POLYGON:
                    geometry = readPolygon(stream);
                    break;
                case Geometry.GT_MULTI_POINT:
                    geometry = readMultiPoint(stream);
                    break;
                default:
                    System.out.println("Unhandled shape type: " + shapeType);
                    geometry = new NullShape();
                    stream.skipBytes(contentLength * 2);
            }

            this.geometry = geometry;
        }

        private static Point readPoint(ImageInputStream stream) throws IOException {
            // Position Field Value Type Order
            double x = stream.readDouble(); // Byte 4 X X Double 1 Little
            double y = stream.readDouble(); // Byte 12 Y Y Double 1 Little
            return new Point(x, y);
        }

        private static MultiPoint readMultiPoint(ImageInputStream stream) throws IOException {
            // Position Field Value Type Order
            double[] box = readBox(stream); // Byte 4 Box Box Double 4 Little
            int numPoints = stream.readInt(); // Byte 36 NumPoints NumPoints Integer 1 Little
            Point[] points = readPoints(stream, numPoints); // Byte 40 Points Points Point NumPoints Little
            return new MultiPoint(box, points);
        }

        private static int[] readParts(ImageInputStream stream, int numParts) throws IOException {
            int[] parts = new int[numParts];
            for (int i = 0; i < parts.length; i++) {
                parts[i] = stream.readInt();
            }
            return parts;
        }

        private static Polyline readPolyLine(ImageInputStream stream) throws IOException {
            // Position Field Value Type Order
            double[] box = readBox(stream); // Byte 4 Box Box Double 4 Little
            int numParts = stream.readInt(); // Byte 36 NumParts NumParts Integer 1 Little
            int numPoints = stream.readInt(); // Byte 40 NumPoints NumPoints Integer 1 Little
            int[] parts = readParts(stream, numParts); // Byte 44 Parts Parts Integer NumParts Little
            Point[] points = readPoints(stream, numPoints); // Byte X Points Points Point NumPoints Little
            return new Polyline(box, parts, points);
        }

        private static Polygon readPolygon(ImageInputStream stream) throws IOException {
            Polyline polyline = readPolyLine(stream);
            return new Polygon(polyline.box, polyline.parts, polyline.points);
        }

        private static double[] readBox(ImageInputStream stream) throws IOException {
            double[] box = new double[4];
            stream.readFully(box, 0, 4);
            return box;
        }

        private static Point[] readPoints(ImageInputStream stream, int numPoints) throws IOException {
            Point[] points = new Point[numPoints];
            for (int i = 0; i < points.length; i++) {
                points[i] = readPoint(stream);
            }
            return points;
        }

    }


    public static interface Geometry {
        int GT_NULL = 0;
        int GT_POINT = 1;
        int GT_POLYLINE = 3;
        int GT_POLYGON = 5;
        int GT_MULTI_POINT = 8;
    }

    public static class NullShape implements Geometry {
    }

    public static class Point implements Geometry {
        public double x; // X coordinate
        public double y; // Y coordinate

        public Point(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    public static class MultiPoint implements Geometry {
        public double[] box; // Bounding Box
        public int numPoints; // Number of Points
        public Point[] points; // The Points in the Set

        public MultiPoint(double[] box, Point[] points) {
            this.box = box;
            this.numPoints = points.length;
            this.points = points;
        }
    }

    public static class Polyline implements Geometry {
        public double[] box; // Bounding Box
        public int numParts; // Number of Parts
        public int numPoints; // Total Number of Points
        public int[] parts; // Index to First Point in Part
        public Point[] points; // Points for All Parts

        public Polyline(double[] box, int[] parts, Point[] points) {
            this.box = box;
            this.numParts = parts.length;
            this.numPoints = points.length;
            this.parts = parts;
            this.points = points;
        }
    }

    public static class Polygon implements Geometry {
        public double[] box; // Bounding Box
        public int numParts; // Number of Parts
        public int numPoints; // Total Number of Points
        public int[] parts; // Index to First Point in Part
        public Point[] points; // Points for All Parts

        public Polygon(double[] box, int[] parts, Point[] points) {
            this.box = box;
            this.numParts = parts.length;
            this.numPoints = points.length;
            this.parts = parts;
            this.points = points;
        }
    }
}
