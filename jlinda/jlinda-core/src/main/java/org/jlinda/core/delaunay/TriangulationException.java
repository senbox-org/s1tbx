package org.jlinda.core.delaunay;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Triangulator is the interface implemented by all the tools performing
 * a triangulation from a set of points, of constraint lines or of polygons.
 */

public class TriangulationException extends Exception {

    public TriangulationException(String message) {
        super(message);
    };

    public TriangulationException(String message, Coordinate coord) {
        super(message + "[" + coord + "]");
    }

    public TriangulationException(String message, Triangle t) {
        super(message + "[" + t + "]");
    }

}
