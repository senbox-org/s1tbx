package org.esa.beam.opengis.cs;

// <to wgs84s> = TOWGS84[<seven param>]
public class WGS84ConversionInfo {
    public double dx, dy, dz;
    public double ex, ey, ez;
    public double ppm;

    public WGS84ConversionInfo() {
    }

    public WGS84ConversionInfo(double dx, double dy, double dz, double ex, double ey, double ez, double ppm) {
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        this.ex = ex;
        this.ey = ey;
        this.ez = ez;
        this.ppm = ppm;
    }
}
