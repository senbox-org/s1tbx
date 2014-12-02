package org.jlinda.core.delaunay;


import java.util.Iterator;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Triangulator is the interface implemented by all the tools performing
 * a triangulation from a set of point, of constraint lines or of polygons.
 */

public interface Triangulator {


    /**
     * Triangulate geometries obtained from a Geometry iterator
     * @param geometries to be triangulated
     */
    public void triangulate(Iterator<Geometry> geometryIterator) throws TriangulationException;


    /**
     * Get a {@link Triangle} iterator iterating through all the triangles
     * issued from the triangulation process.
     * Returning an iterator is preferred, because it can iterate through an
     * in-memory list of Triangles or from a stream (e.g. a file), depending on
     * the implementation.
     * @return an Iterator
     */
    public Iterator<Triangle> getTriangleIterator();

}
