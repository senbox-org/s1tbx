package org.jlinda.core.delaunay;


import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import java.io.*;
import java.util.*;

/**
 * Utility class to test the Triangulation methods.
 */
public class TriangulationTest {
    
    static Coordinate[] pts1 = null;
    
    private static BufferedReader reader;

    static {
//        pts1 = new Coordinate[]{
//            new Coordinate(2,8),
//            new Coordinate(2,2),
//            new Coordinate(8,2),
//            new Coordinate(8,8),
//            new Coordinate(7,6),
//            new Coordinate(3,6),
//            new Coordinate(3,4),
//            new Coordinate(7,4)
//        };
//        pts1 = new Coordinate[]{
//            new Coordinate(7,6),
//            new Coordinate(2,8),
//            new Coordinate(2,2),
//            new Coordinate(8,8),
//            new Coordinate(3,6),
//            new Coordinate(3,4),
//            new Coordinate(8,2),
//            new Coordinate(7,4)
//        };


//        try {
//            reader = new BufferedReader(new FileReader("/home/pmar/downloads/ddc/temp_crop.dat"));
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//        } finally {
//            if (reader != null){
//                try{ reader.close(); }
//                catch (IOException ignore) {}
//        }

//        pts1 = new Coordinate[717326];
//        int counter = 0;

//        String inputFile = "/d2/test.processing/temp.717326.dat";
//        try {
//
//            FileInputStream in = new FileInputStream(inputFile);
//            BufferedReader inStream = new BufferedReader(new InputStreamReader(in));
//
//            for (String line; (line = inStream.readLine()) != null;) {
//                 pts1[counter++] = new Coordinate(Double.valueOf(line.split("\\t")[1]), Double.valueOf(line.split("\\t")[2]));
//            }
//
//        } catch (IOException e) {
//            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//        }

        pts1 = new Coordinate[1000000];
        for (int i = 0; i < 1000000; i++)
            pts1[i] = new Coordinate(Math.random() * 1000000, Math.random() * 1000000);


    }

    public static void main(String[] args) {
        TriangulationTest test = new TriangulationTest();
        test.testFastDelaunayTriangulator(pts1);
        test.testFastDelaunayTriangulator(pts1);
    }
    
    private void testFastDelaunayTriangulator(Coordinate[] tp) {
        System.out.println("testFastDelaunayTriangulator with " + tp.length + " points");
        long t0 = System.currentTimeMillis();
        List<Geometry> list = new ArrayList<Geometry>();
        GeometryFactory gf = new GeometryFactory();
        for (Coordinate c : tp) list.add(gf.createPoint(c));
        long t1 = System.currentTimeMillis();
        System.out.printf("   set created in %10.3f sec\n", (0.001 * (t1 - t0)));

        long t2 = System.currentTimeMillis();
        FastDelaunayTriangulator FDT = new FastDelaunayTriangulator();
        try {
            FDT.triangulate(list.iterator());
        } catch (TriangulationException te) {
            te.printStackTrace();
        }
        long t3 = System.currentTimeMillis();
        System.out.printf("   triangulated in %10.3f sec\n", (0.001 * (t3 - t2)));

/*
        String outputFile = "/d2/test.processing/triangles.dat";
        BufferedWriter bufferedWriter = null;

        try {
            bufferedWriter = new BufferedWriter(new FileWriter(outputFile));

            for (Triangle triangleObject : FDT.triangles) {

                bufferedWriter.write(
                        String.valueOf(triangleObject.getA().x) + "\t" +
                                String.valueOf(triangleObject.getA().y) + "\t" +
                                String.valueOf(triangleObject.getB().x) + "\t" +
                                String.valueOf(triangleObject.getB().y) + "\t" +
                                String.valueOf(triangleObject.getC().x) + "\t" +
                                String.valueOf(triangleObject.getC().y) + "\t"
                );
                bufferedWriter.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
*/

    }


}
