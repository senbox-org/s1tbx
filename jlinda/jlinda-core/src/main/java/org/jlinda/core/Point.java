package org.jlinda.core;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * User: pmar@ppolabs.com
 * Date: 2/18/11
 * Time: 3:34 PM
 */
public class Point extends Coordinate {

    public Point() {
        this(0, 0, 0);
    }

    public Point(double x, double y) {
        super(x, y);
    }

    public Point(double x, double y, double z) {
        super(x, y, z);
    }

    public Point(double[] xyz) {
        super(xyz[0], xyz[1], xyz[2]);
    }

    public Point(Point c) {
        super(c);
    }

    public Point(Coordinate c) {
        super(c);
    }

    public Point min(Point p) {
        double dx = x - p.x;
        double dy = y - p.y;
        double dz = z - p.z;
        return new Point(dx, dy, dz);
    }

    public Point plus(Point p) {
        double dx = x + p.x;
        double dy = y + p.y;
        double dz = z + p.z;
        return new Point(dx, dy, dz);
    }

    public Point mult(Point p) {
        double dx = x * p.x;
        double dy = y * p.y;
        double dz = z * p.z;
        return new Point(dx, dy, dz);
    }

    public Point div(Point p) {
        double dx = x / p.x;
        double dy = y / p.y;
        double dz = z / p.z;
        return new Point(dx, dy, dz);
    }

    // inner product
    public double in(Point p) {
        double dx = x * p.x;
        double dy = y * p.y;
        double dz = z * p.z;
        return (dx + dy + dz);
    }

    // cross product
    public Point out(Point p) {
        return new Point(y * p.z - z * p.y, z * p.x - x * p.z, x * p.y - y * p.x);
    }

    public Point multByScalar(double scalar) {
        double dx = x * scalar;
        double dy = y * scalar;
        double dz = z * scalar;
        return new Point(dx, dy, dz);
    }

    public Point divByScalar(double scalar) {
        double dx = x / scalar;
        double dy = y / scalar;
        double dz = z / scalar;
        return new Point(dx,dy,dz);
    }

    public Point negative() {
        return multByScalar(-1);
    }

    public double norm2() {
        return x*x + y*y + z*z;
    }

    public double norm() {
        return Math.sqrt(x*x + y*y + z*z);
    }

    public Point normalize() {
        double norm = this.norm();
        double dx = x / norm;
        double dy = y / norm;
        double dz = z / norm;
        return new Point(dx, dy, dz);
    }

    // Radians [0:pi]
    public double angle(Point p) {
        return Math.acos(in(p) / (norm() * p.norm()));
    }

    public Point scale(double scalar) {
        return multByScalar(scalar);
    }

    @Override
    public double distance(Coordinate p) {

        double dx = x - p.x;
        double dy = y - p.y;
        double dz = z - p.z;

        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public void toScreen() {
        System.out.println("Point{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                '}');
    }

    // TODO: UNIT test for only if x,y defined
    public double[] toArray() {
        double array[] = new double[3];
        array[0] = this.x;
        array[1] = this.y;
        array[2] = this.z;
        return array;
    }
}