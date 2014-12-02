package org.jlinda.core.delaunay;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Base class for Delaunay Triangulation implementations.
 * In mathematics, and computational geometry, a Delaunay triangulation for
 * a set P of points in the plane is a triangulation DT(P) such that no point
 * in P is inside the circumcircle of any triangle in DT(P). Delaunay
 * triangulations maximize the minimum angle of all the angles of the triangles
 * in the triangulation; they tend to avoid skinny triangles.
 */

public abstract class AbstractInMemoryTriangulator extends AbstractTriangulator {


    /**
     * Triangles of the triangulation.
     * Do not contain phantom triangles located out of the convex hull.
     */
    public List<Triangle> triangles = new ArrayList<Triangle>();


    /**
     * Triangulate geometries.
     * @param geometries to be triangulated
     */
    abstract public void triangulate(Iterator<Geometry> geometryIterator) throws TriangulationException;

    /**
     * Get Triangles issued from the triangulation process.
     * @return a list of Triangles
     */
     public Iterator<Triangle> getTriangleIterator() {
         return triangles.iterator();
     }

}
