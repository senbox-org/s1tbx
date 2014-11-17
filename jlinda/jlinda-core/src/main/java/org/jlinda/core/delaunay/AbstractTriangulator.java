package org.jlinda.core.delaunay;

import java.util.Iterator;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Base class for Delaunay Triangulation implementations.
 * In mathematics, and computational geometry, a Delaunay triangulation for
 * a set P of points in the plane is a triangulation DT(P) such that no point
 * in P is inside the circumcircle of any triangle in DT(P). Delaunay
 * triangulations maximize the minimum angle of all the angles of the triangles
 * in the triangulation; they tend to avoid skinny triangles.
 */

public abstract class AbstractTriangulator implements Triangulator {


    /**
     * A phantom Triangle located out of the convex hull and having HORIZON as
     * its C point.
     */
    protected Triangle currentExternalTriangle;


    /**
     * Fictive Coordinate representing the Horizon, or an infinite point.
     * It closes triangles around the convex hull of the triangulation
     */
    protected static final Coordinate HORIZON = new Coordinate(Double.NaN, Double.NaN);

    /**
     * Triangulate geometries.
     * @param geometries to be triangulated
     */
    abstract public void triangulate(Iterator<Geometry> geometryIterator) throws TriangulationException;

    /**
     * Get Triangles issued from the triangulation process.
     * @return a list of Triangles
     */
    abstract public Iterator<Triangle> getTriangleIterator();


    protected void link (Triangle t1, int side1, Triangle t2, int side2) {
        t1.setNeighbour(side1, t2);
        t2.setNeighbour(side2, t1);
    }

    protected void link (Triangle t1, int side1, Triangle t2) {
        if (t1.getVertex(side1) == t2.getVertex(side1)) {
            t1.setNeighbour(side1, t2);
            t2.setNeighbour((side1+2)%3, t1);
        }
        else if (t1.getVertex(side1) == t2.getVertex((side1+1)%3)) {
            t1.setNeighbour(side1, t2);
            t2.setNeighbour(side1, t1);
        }
        else if (t1.getVertex(side1) == t2.getVertex((side1+2)%3)) {
            t1.setNeighbour(side1, t2);
            t2.setNeighbour((side1+1)%3, t1);
        }
    }
}
