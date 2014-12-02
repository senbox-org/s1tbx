package org.jlinda.core;

import java.awt.*;
import java.io.Serializable;

public class Window implements Comparable, Cloneable, Serializable {

    /**
     * min line coordinate
     */
    public long linelo;

    /**
     * max line coordinate
     */
    public long linehi;

    /**
     * min pix coordinate
     */
    public long pixlo;

    /**
     * max pix coordinate
     */
    public long pixhi;

    /**
     * Constructs a <code>Window</code> at (linelo,linehi,pixlo,pixhi).
     *
     * @param linelo the min line coordinate
     * @param linehi the max line coordinate
     * @param pixlo  the min pix coordinate
     * @param pixhi  the max pix coordinate
     */
    public Window(long linelo, long linehi, long pixlo, long pixhi) {
        this.linelo = linelo;
        this.linehi = linehi;
        this.pixlo = pixlo;
        this.pixhi = pixhi;
    }

    /**
     * Constructs a <code>Window</code> at (0,0,0,0).
     */
    public Window() {
        this(0, 0, 0, 0);
    }

    /**
     * Constructs a <code>Window</code> having the same (linelo,linehi,pixlo,pixhi) values as
     * <code>other</code>.
     *
     * @param w the <code>Window</code> to copy.
     */
    public Window(final Window w) {
        this(w.linelo, w.linehi, w.pixlo, w.pixhi);
    }

    /**
     * Constructs a <code>Window</code> having the (x,y,width,height) values as
     * <code>other</code> Rectangle.
     *
     * @param r the <code>Rectangle</code> to copy.
     */
    public Window(final Rectangle r) {
        this.linelo = r.y;
        this.linehi = r.y + r.height;
        this.pixlo = r.x;
        this.pixhi = r.x + r.width;
    }


    /**
     * Sets this <code>Window</code>s (linelo,linehi,pixlo,pixhi) values to that
     * of <code>other</code>.
     *
     * @param other the <code>Window</code> to copy
     */
    public void setWindow(final Window other) {
        linelo = other.linelo;
        linehi = other.linehi;
        pixlo = other.pixlo;
        pixhi = other.pixhi;
    }

    /**
     * Set this window <code>Window</code> at (linelo,linehi,pixlo,pixhi).
     *
     * @param linelo the min line coordinate
     * @param linehi the max line coordinate
     * @param pixlo  the min pix coordinate
     * @param pixhi  the max pix coordinate
     */
    public void setWindow(final long linelo, final long linehi, final long pixlo, final long pixhi) {
        this.linelo = linelo;
        this.linehi = linehi;
        this.pixlo = pixlo;
        this.pixhi = pixhi;
    }


    /**
     * Returns a <code>String</code> of the form
     * <I>Class{linelo,linehi,pixlo,pixhi}</I> .
     *
     * @return a <code>String</code> of the form <I>Class{linelo,linehi,pixlo,pixhi}</I>
     */
    @Override
    public String toString() {
        return "Window{" +
                "linelo=" + linelo +
                ", linehi=" + linehi +
                ", pixlo=" + pixlo +
                ", pixhi=" + pixhi +
                '}';
    }

    /**
     * Prints a <code>String</code> of the form
     * <I>Class{linelo,linehi,pixlo,pixhi}</I> .
     */
    public void toScreen() {
        System.out.println("Window{" +
                "linelo=" + linelo +
                ", linehi=" + linehi +
                ", pixlo=" + pixlo +
                ", pixhi=" + pixhi +
                '}');
    }

    // TODO: for java doc
//        *    <LI> -1 : this.x < other.x || ((this.x == other.x) && (this.y < other.y))
//        *    <LI>  0 : this.linelo == other.linelo && this.linehi = other.linehi && this.pixlo == other.pixlo && this.pixhi = other.pixhi
//        *    <LI>  1 : this.x > other.x || ((this.x == other.x) && (this.y > other.y))

    /**
     * Compares this {@link Window} with the specified {@link Window} for order.
     * This method ignores the z value when making the comparison.
     * Returns:
     * <UL>
     * <p/>
     * </UL>
     * Note: This method assumes that window values
     * are valid numbers.  NaN values are not handled correctly.
     *
     * @param o the <code>Window</code> with which this <code>Window</code>
     *          is being compared
     * @return -1, zero, or 1 as this <code>Window</code>
     *         is less than, equal to, or greater than the specified <code>Window</code>
     */
    public int compareTo(final Object o) {

        Window other = (Window) o;

        if (linelo < other.linelo) return -1;
        if (linelo > other.linelo) return 1;
        if (linehi < other.linehi) return -1;
        if (linehi > other.linehi) return 1;

        if (pixlo < other.pixlo) return -1;
        if (pixlo > other.pixlo) return 1;
        if (pixhi < other.pixhi) return -1;
        if (pixhi > other.pixhi) return 1;

        return 0;
    }

    /**
     * Returns <code>true</code> if <code>other</code> has the same values for linelo,linehi,
     * and pixlo,pixhi.
     *
     * @param other <code>Window</code> with which to do the comparison.
     * @return <code>true</code> if <code>other</code> is a <code>Window</code>
     *         with the same values for linelo,linehi,and pixlo,pixhi.
     */
    public boolean equals(final Object other) {
        if (!(other instanceof Window)) {
            return false;
        }
        return equals((Window) other);
    }

    public boolean equals(final Window other) {
        return (linelo == other.linelo) && (linehi == other.linehi) &&
                (pixlo == other.pixlo) && (pixhi == other.pixhi);
    }

    public Object clone() throws CloneNotSupportedException {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            // this shouldn't happen, since we are Cloneable
            return null;
//        throw new InternalError();
        }
    }


    /**
     * Computes extend of window in AZIMUTH direction ~ height.
     * <p/>
     * NOTE: for SAR computations counting of lines starts at 0!
     *
     * @param w a window
     * @return the height of window
     */
    public static long lines(final Window w) {
        return w.linehi - w.linelo + 1;

    }

    /**
     * Computes extend of window in AZIMUTH direction ~ height.
     * <p/>
     * NOTE: for SAR computations counting of lines starts at 0!
     *
     * @return the height of window
     */
    public long lines() {
        return linehi - linelo + 1;

    }

    /**
     * Computes extend of window in RANGE direction ~ width.
     * <p/>
     * NOTE: for SAR computations counting of lines starts at 0!
     *
     * @param w a window
     * @return the height of window
     */
    public static long pixels(final Window w) {
        return w.pixhi - w.pixlo + 1;
    }

    /**
     * Computes extend of window in RANGE direction ~ width.
     * <p/>
     * NOTE: for SAR computations counting of lines starts at 0!
     *
     * @return the height of window
     */
    public long pixels() {
        return pixhi - pixlo + 1;
    }

    /**
     * Computes a hash code for a double value, using the algorithm from
     * Joshua Bloch's book <i>Effective Java"</i>
     *
     * @param x
     * @return a hashcode for the double value
     */
    private static int hashCode(final double x) {
        final long f = Double.doubleToLongBits(x);
        return (int) (f ^ (f >>> 32));
    }

    /**
     * Gets a hashcode for this window.
     *
     * @return a hashcode for this window
     */
    public int hashCode() {
        //Algorithm from Effective Java by Joshua Bloch
        int result = 17;
        result = 37 * result + hashCode(linelo);
        result = 37 * result + hashCode(linehi);
        result = 37 * result + hashCode(pixlo);
        result = 37 * result + hashCode(pixhi);
        return result;
    }

//    /**
//     * Compares two {@link Window}s along to the number of
//     * dimensions specified.
//     *
//     * @param o1 a {@link Window}
//     * @param o2 a {@link Window}
//     * @return -1, 0, or 1 depending on whether o1 is less than, equal to,
//     *         or greater than o2
//     */
//    public int compare(Object o1, Object o2) {
//        Window c1 = (Window) o1;
//        Window c2 = (Window) o2;
//
//        int compLineLo = compare(c1.linelo, c2.linelo);
//        if (compLineLo != 0) return compLineLo;
//
//        int compLineHi = compare(c1.linehi, c2.linehi);
//        if (compLineHi != 0) return compLineHi;
//
//        int compPixLo = compare(c1.pixlo, c2.pixlo);
//        if (compPixLo != 0) return compPixLo;
//
//        int compPixHi = compare(c1.linehi, c2.linehi);
//        if (compPixHi != 0) return compPixHi;
//
//        return 0;
//    }

}
