package org.jlinda.core.delaunay;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import java.util.*;

import static org.jlinda.core.delaunay.MathUtils.ccw;

/**
 * Fast Delaunay Triangulator.
 */

public class FastDelaunayTriangulator extends AbstractInMemoryTriangulator {

    /**
     * A phantom Triangle located out of the convex hull and having HORIZON as
     * its C point.
     */
    private Triangle currentExternalTriangle;


    /**
     * Fictive Coordinate representing the Horizon, or an infinite point.
     * It closes triangles around the convex hull of the triangulation
     */
    private static final Coordinate HORIZON = new Coordinate(Double.NaN, Double.NaN);

    /**
     * Triangulate geometries.
     * @param geometryIterator to be triangulated
     */
    public void triangulate(Iterator<Geometry> geometryIterator) throws TriangulationException {
        Collection<Coordinate> vertices = extractUniqueVertices(geometryIterator);
        vertices = sortVertices(vertices);
        Iterator<Coordinate> vertexIterator = vertices.iterator();
        if (vertices.size() > 2) {
            try {
                initTriangulation(vertexIterator.next(), vertexIterator.next());
                while (vertexIterator.hasNext()) {
                    addExternalVertex(vertexIterator.next());
                }
            } catch(TriangulationException te) {throw te;}
        }
        else {
            throw new TriangulationException("A minimum of three distinct " +
            "points is necessary to triangulate a set of geometries");
        }
    }


    private Collection<Coordinate> extractUniqueVertices(final Iterator<Geometry> geometryIterator) {
        Set<Coordinate> vertices = new HashSet<Coordinate>();
        for (Iterator<Geometry> it = geometryIterator ; it.hasNext() ; ) {
            Collections.addAll(vertices, it.next().getCoordinates());
        }
        return vertices;
    }


    private List<Coordinate> sortVertices(Collection<Coordinate> vertices) {
        List list = new ArrayList(vertices);
        Collections.sort(list, new Coordinate.DimensionalComparator());
        return list;
    }


    private void initTriangulation(Coordinate c0, Coordinate c1) {
        Triangle t0 = new Triangle(c0, c1, HORIZON);
        Triangle t1 = new Triangle(c1, c0, HORIZON);
        t0.setNeighbours(t1, t1, t1);
        t1.setNeighbours(t0, t0, t0);
        //if (debugLevel!=0) debug(0,"Initialisation : t0 = " + t0);
        //if (debugLevel!=0) debug(0,"Initialisation : t1 = " + t1);
        currentExternalTriangle = t1;
    }


    protected void addExternalVertex(Coordinate vertex) throws TriangulationException {
        List<Triangle> newTriangles = buildTrianglesBetweenNewVertexAndConvexHull(vertex);
        for (Triangle t : newTriangles) {
            //if (debugLevel>=NORMAL) debug(2,"new triangle before delaunay " + t);
            if (t.getC() != HORIZON) delaunay(t, 0);
        }
        //if (debugLevel>=SHORT) {
        //    for (Triangle t : newTriangles) debug(1,"add triangle " + t);
        //}
        triangles.addAll(newTriangles);
    }


    private List<Triangle> buildTrianglesBetweenNewVertexAndConvexHull(Coordinate c)
                                                throws TriangulationException {

        //Triangle currentT = beforeMaxT;
        Triangle currentT = currentExternalTriangle;
        Triangle nextExternalTriangle = currentExternalTriangle.getNeighbour(2);
        //int lastCCW = abcOrientation(currentT, c);
        int lastCCW = ccw(currentT.getA().x, currentT.getA().y,
                          currentT.getB().x, currentT.getB().y, c.x, c.y);
        int currentCCW = lastCCW;
        Triangle beforeFirstVisibleT = currentExternalTriangle;
        Triangle firstVisibleT = null;
        Triangle lastVisibleT = null;
        Triangle afterLastVisibleT = nextExternalTriangle;
        List<Triangle> newT = new ArrayList<Triangle>();
        boolean oneCycleCompleted = false;
        //if (debug) System.out.println("   searching visible sides of convex hull");
        while (true) {
            currentT = currentT.getACO();
            //currentCCW = abcOrientation(currentT, c);
            currentCCW = ccw(currentT.getA().x, currentT.getA().y,
                             currentT.getB().x, currentT.getB().y, c.x, c.y);
            if (currentCCW > 0) {
                if (lastCCW <= 0) {
                    firstVisibleT = currentT;
                    beforeFirstVisibleT = currentT.getCBO();
                }
                if (firstVisibleT!=null) {
                    //if (debugLevel>=VERBOSE) debug(2,"visible side : " + currentT.getA()+"-"+currentT.getB());
                    currentT.setC(c);
                    newT.add(currentT);
                }
                else {
                    //if (debugLevel>=VERBOSE) debug(2,"before first visible side : " + currentT.getA()+"-"+currentT.getB());
                }
            }
            else {
                //if (debugLevel>=VERBOSE) debug(2,"invisible side : " + currentT);
                if (firstVisibleT!=null && lastCCW>0) {
                    lastVisibleT = currentT.getCBO();
                    afterLastVisibleT = currentT;
                }
            }
            lastCCW = currentCCW;
            if (firstVisibleT!=null && lastVisibleT!=null) break;
            if (oneCycleCompleted && firstVisibleT==null && lastVisibleT==null) break;
            if (currentT==currentExternalTriangle) oneCycleCompleted = true;
        }

        currentExternalTriangle = new Triangle(c, beforeFirstVisibleT.getA(), HORIZON);
        nextExternalTriangle = new Triangle(afterLastVisibleT.getB(), c, HORIZON);
        linkExteriorTriangles(beforeFirstVisibleT, currentExternalTriangle);
        if (firstVisibleT!=null || lastVisibleT!=null) {
            link(currentExternalTriangle, 0, firstVisibleT, 1);
            link(nextExternalTriangle, 0, lastVisibleT, 2);
        }
        else link(currentExternalTriangle, 0, nextExternalTriangle, 0);
        linkExteriorTriangles(nextExternalTriangle, afterLastVisibleT);
        
        linkExteriorTriangles(currentExternalTriangle, nextExternalTriangle);

        //if (debugLevel>=VERBOSE) debug(2,"new exterior triangle : " + currentExternalTriangle);
        //if (debugLevel>=VERBOSE) debug(2,"new exterior triangle : " + nextExternalTriangle);
        
        return newT;
    }


    /**
     * Link t1 and t2 where t1 and t2 are both infinite exterior triangles,
     * t2 following t1 if one iterates around the triangulation in ccw.
     */
    private void linkExteriorTriangles(Triangle t1, Triangle t2) {
        assert(t1.getC()==HORIZON && t2.getC()==HORIZON && t1.getA()==t2.getB());
        t1.setACO(t2);
        t2.setCBO(t1);
    }


   /** 
    * Check the delaunay property of this triangle. If the circumcircle contains
    * one of the opposite vertex, the two triangles forming the quadrilatera are
    * flipped. The method is iterative.
    * While triangulating an ordered set of coordinates about
    * <ul>
    * <li>40% of time is spent in flip() method,</li>
    * <li>15% of time is spent in fastInCircle() method and</li>
    * <li>10% of time is spent in getOpposite() method</li>
    * </ul>
    * @param t triangle to check and to modify (if needed)
    */
    private static void delaunay (final Triangle t, final int side) {

        if (t.getEdgeType(side)==Triangle.EdgeType.HARDBREAK) return;

        final Triangle opp = t.getNeighbour(side);
        if (opp.getC()==HORIZON) return;
        final int i = t.getOpposite(side);

        if (t.inCircle(opp.getVertex(i)) > 0) {
            // Flip triangles without creating new Triangle objects
            flip(t, side, opp, (i+1)%3);
            delaunay(t,1);
            delaunay(t,2);
            delaunay(opp,0);
            delaunay(opp,1);
        }
    }


    /**
     * If t0 and t1 are two triangles sharing a common edge AB,
     * the method replaces ABC and BAD triangles by DCA and DBC, respectively.
     *          B                      B       
     *          *                      *       
     *        / | \                  /   \     
     *      /   |   \              /       \   
     *     /    |    \            /         \  
     *  C *     |     * D      C *-----------* D
     *     \    |    /            \         /  
     *      \   |   /              \       /   
     *       \  |  /                \     /    
     *        \ | /                  \   /     
     *          *                      *       
     *          A                      A       
     * To be fast, this method supposed that input triangles share a common
     * edge and that this common edge is known.
     * A check may be performed to ensure these conditions are verified.
     * TODO : change the breaklines edges
     */
    public static void flip(Triangle t0, int side0, Triangle t1, int side1) {
        int side0_1 = (side0+1)%3;
        int side0_2 = (side0+2)%3;
        int side1_1 = (side1+1)%3;
        int side1_2 = (side1+2)%3;

        Coordinate t0A = t1.getVertex(side1_2);
        Coordinate t0B = t0.getVertex(side0_2);
        Coordinate t1B = t0.getVertex(side0_1);

        t0.setABC(t0A, t0B, t0.getVertex(side0));
        t0.setBAO(t1);
        link(t0,1,t0.getNeighbour(side0_2));
        link(t0,2,t1.getNeighbour(side1_1));
        t1.setABC(t0A, t1B, t0B);
        link(t1,0,t1.getNeighbour(side1_2));
        link(t1,1,t0.getNeighbour(side0_1));
        t1.setACO(t0);
    }


}
