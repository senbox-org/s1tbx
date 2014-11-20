package org.jlinda.core.delaunay;


import com.vividsolutions.jts.geom.Coordinate;
import java.util.Collections;
import java.util.List;

import static org.jlinda.core.delaunay.MathUtils.*;

/**
 * A triangle in a triangulation defined by its 3 vertices A, B, C.
 * Every triangle should be described in counterclockwise.
 * A special kind of Triangle is defined to cover the space outside the
 * triangulation convex hull : for those triangles, C point = HORIZON
 * Every Triangle has also references to its 3 neighbours :<ul>
 * <li>BAO is the neighbour along AB side</li>
 * <li>CBO is the neighbour along BC side</li>
 * <li>ACO is the neighbour along CA side</li>
 * </ul>
 * Finally, every triangle has three edges which may be :
 * <ul>
 * <li>EdgeType.VIRTUAL</li>
 * <li>EdgeType.SOFTBREAK</li>
 * <li>EdgeType.HARDBREAK</li>
 * </ul>
 */

public class Triangle {


    /**
     * HORIZON is a virtual point used as the C point of all the Triangles
     * triangles lacated out of the triangulation convex hull.
     */
    public final static Coordinate HORIZON = new Coordinate(Double.NaN, Double.NaN);


    /**
     * A Triangle edge may be :
     * <ul>
     * <li>VIRTUAL : no constraint, default value</li>
     * <li>SOFTBREAK : constrained edge representing a smooth limit between two triangles</li>
     * <li>HARDBREAK : constrained edge representing a break in the terrain profile</li>
     * </ul>
     */
    public static enum EdgeType {
        VIRTUAL,
        SOFTBREAK,
        HARDBREAK
    };


    private Coordinate A, B, C;

    private Triangle BAO, CBO, ACO;

    private EdgeType AB = EdgeType.VIRTUAL;
    private EdgeType BC = EdgeType.VIRTUAL;
    private EdgeType CA = EdgeType.VIRTUAL;

    private static final Coordinate.DimensionalComparator COORD2DCOMP = new Coordinate.DimensionalComparator();

    /**
     * Basic constructor which does not check Triangle validity.
     * This is fast and useful if the set of points is known as valid.
     * Otherwise, you can check validity at construction time with
     * Triangle(Coordinate A, Coordinate B, Coordinate C, boolean ccw)
     * @param A first point of the Triangle
     * @param B second point of the Triangle
     * @param C third point of the Triangle
     */
    public Triangle(Coordinate A, Coordinate B, Coordinate C) {
        this.A = A;
        this.B = B;
        this.C = C;
    }


    /**
     * Triangle constructor checking validity of input vertices, and ensuring
     * the order of vertices is counterclockwise.
     * @param A
     * @param B
     * @param C
     * @param check check the validity of parameters and build the triangle
     * in a ccw order
     */
    public Triangle(Coordinate A, Coordinate B, Coordinate C, boolean ccw) {
        assert (A != null) : "A must not be null";
        assert (B != null) : "B must not be null";
        assert (C != null) : "C must not be null";
        assert (A.equals(B)) : "A must not be equal to B";
        assert (A.equals(C)) : "A must not be equal to C";
        assert (B.equals(C)) : "B must not be equal to C";
        this.A = A;
        if (C==HORIZON || !ccw || ccw(A.x, A.y, B.x, B.y, C.x, C.y) < 0) {
            this.B = B;
            this.C = C;
        }
        else {
            this.C = B;
            this.B = C;
        }
    }


    /**
     * A valid triangle must have 3 non null vertices
     * It must not be flat and vertices must be in a ccw order
     * Last point (C) can eventually be the HORIZON (in which case flat and ccw
     * condition are not checked).
     * @return if this Triangle is valid or not
     */
    public boolean isValid() {
        if (A == null || B == null || C == null) {
            return false;
        }
        else if (A == HORIZON || B == HORIZON) {
            return false;
        }
        else if (C != HORIZON && ccw(A.x, A.y, B.x, B.y, C.x, C.y) <= 0) {
            return false;
        }
        else if (A.equals(B) || B.equals(C) || C.equals(A)) {
            return false;
        }
        else if (BAO == null || CBO==null || ACO == null) {
            return false;
        }
        else {
            return true;
        }
    }

    public Coordinate getA() {
        return A;
    }

    public Coordinate getB() {
        return B;
    }

    public Coordinate getC() {
        return C;
    }

    /**
     * Get vertex of Triangle object for a 'local' index i.
     * Vertex A is indexed as 0, B as 1, and C as 2.
     * @param i local index of vertex of Triangle Object.
     * @return Coordinate for the given vertex
     */
    public Coordinate getVertex(int i) {
        return i==0?A:i==1?B:C;
        //TODO: what if i larger then 2? Now method always returns value for i=2.
    }

    /**
     * Get 'local' index (eg. 0,1,2) for an input vertex of Triangle object.
     * @param p Coordinate of vertex p.
     * @return If point p vertex of Triangle, returns integer (0,1, or 2) for
     *  points (A,B, or C) respectively. If p not vertex of Triangle, returns -1.
     */
    public int getIndex(Coordinate p) {
        if (p.equals(A)) return 0;
        else if (p.equals(B)) return 1;
        else if (p.equals(C)) return 2;
        else return -1;
    }

    public void setA(Coordinate A) {
        this.A = A;
    }

    public void setB(Coordinate B) {
        this.B = B;
    }

    public void setC(Coordinate C) {
        this.C = C;
    }

    public void setVertex(int i, Coordinate p) {
        if (i == 0) this.A = p;
        else if (i == 1) this.B = p;
        else this.C = p;
    }

    public void setABC(Coordinate A, Coordinate B, Coordinate C) {
        this.A = A;
        this.B = B;
        this.C = C;
    }

    public Triangle getBAO() {
        return BAO;
    }

    public Triangle getCBO() {
        return CBO;
    }

    public Triangle getACO() {
        return ACO;
    }

    public Triangle getNeighbour(int i) {
        return i==0?BAO:i==1?CBO:ACO;
    }

    public void setBAO(Triangle BAO) {
        this.BAO = BAO;
    }
    
    public void setCBO(Triangle CBO) {
        this.CBO = CBO;
    }
    
    public void setACO(Triangle ACO) {
        this.ACO = ACO;
    }
    
    public void setNeighbour(int side, Triangle t) {
        if (side == 0) this.BAO = t;
        else if (side == 1) this.CBO = t;
        else this.ACO = t;
    }

    public void setNeighbours(Triangle BAO, Triangle CBO, Triangle ACO) {
        this.BAO = BAO;
        this.CBO = CBO;
        this.ACO = ACO;
    }

    public EdgeType getAB() {return AB;}

    public EdgeType getBC() {return BC;}

    public EdgeType getCA() {return CA;}

    public EdgeType getEdgeType(int side) {
        if (side == 0) return AB;
        else if (side == 1) return BC;
        else return CA;
    }

    public void setAB(EdgeType type) {this.AB = type;}

    public void setBC(EdgeType type) {this.BC = type;}

    public void setCA(EdgeType type) {this.CA = type;}

    public void setEdgeType(int side, EdgeType type) {
        if (side == 0) this.AB = type;
        else if (side == 1) this.BC = type;
        else this.CA = type;
    }

    /**
     * Look for the position of the point in the neighbour of this Triangle
     * which is not adjacent to this Triangle
     * @param side number of the neighbour where opposite point is looked for.
     * @return the position of opposite point in the neighbour description
     */
    public int getOpposite(int side) {
        //switch(side) {
        if (side==0) {
            if (BAO.getA()==A) return 1;
            if (BAO.getB()==A) return 2;
            if (BAO.getC()==A) return 0;
        }
        if (side==1) {
            if (CBO.getA()==B) return 1;
            if (CBO.getB()==B) return 2;
            if (CBO.getC()==B) return 0;
        }
        if (side==2) {
            if (ACO.getA()==C) return 1;
            if (ACO.getB()==C) return 2;
            if (ACO.getC()==C) return 0;
        }
        return -1;
    }
    
    /**
     * Position of p relative to this triangle.
     * returns an integer where
     * hundreds represents the position of p relative to the segment opposite of A = BC
     * dizains represents the position of p relative to the segment opposite of B = CA
     * units represents the position of p relative to the segment opposite of C = AB
     * position are the result of ccw, or 2 if ccw = -1
     *
     *                       \  221  /
     *                        \     /
     *                         \001/
     *                          \ /
     *                           .
     *                          /C\
     *                         /   \
     *                        /     \
     *           121      101/  111  \011      211
     *                      /         \
     *                     /A         B\
     *       -------------. ----------- .---------------
     *                100/       110     \010
     *                  /                 \
     *        122      /         112       \      212
     * if r is the return integer
     * r/100 * (r/10)%10 * r%10 = 1 if p is inside t
     * r/100 * (r/10)%10 * r%10 = 0 if p is on the border of t
     * r/100 * (r/10)%10 * r%10 > 1 if p is out of t
     * @return an integer representing the position of p
     */
    public int locate(Coordinate p) {
        int cc0 = ccw(A.x, A.y, B.x, B.y, p.x, p.y);
        int cc1 = ccw(B.x, B.y, C.x, C.y, p.x, p.y);
        int cc2 = ccw(C.x, C.y, A.x, A.y, p.x, p.y);
        cc0 = cc0<0?2:cc0;
        cc1 = cc1<0?2:cc1;
        cc2 = cc2<0?2:cc2;
        return cc0*100 + cc1*10 + cc2;
    }

    /**
      * String representation of the Triangle as a set of Coordinates.
      */
    public String toString() {
        return "Triangle " + A + " - " + B + " - " +C;
    }

    /**
     * String representation of this Triangle as a set of indices in a
     * {@link Coordinate}s list.
     * @param the coordinates list
     */
    public String toString(List<Coordinate> pts) {
        StringBuffer sb = new StringBuffer("T:");
        if (A!=null) sb.append(Collections.binarySearch(pts, A, COORD2DCOMP) + "-");
        else sb.append("null-");
        if (B!=null) sb.append(Collections.binarySearch(pts, B, COORD2DCOMP) + "-");
        else sb.append("null-");
        if (C!=null) sb.append(Collections.binarySearch(pts, C, COORD2DCOMP));
        else sb.append("null");
        return sb.toString();
    }

}