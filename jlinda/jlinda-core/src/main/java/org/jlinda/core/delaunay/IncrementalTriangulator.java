package org.jlinda.core.delaunay;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Triangulator able to add new vertices to a triangulation, repeatedly, one
 * vertex at a time.
 * With such a triangulator, there is no pre-condition on vertex order.
 *
 */

public interface IncrementalTriangulator extends Triangulator {

    /**
     * Add a new Geometry to be inserted in an existing triangulation.
     */
    public void add(Geometry geometry) throws TriangulationException;

    /**
     * Add a new Vertex to be inserted into an existing triangulation.
     */
    public void add(Coordinate coord) throws TriangulationException;

}
