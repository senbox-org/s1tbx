package org.esa.snap.core.dataio.geocoding;

public class GeoRaster {

    private final double[] longitudes;
    private final double[] latitudes;
    private final int rasterWidth;
    private final int rasterHeight;
    private final int sceneWidth;
    private final int sceneHeight;
    private final double rasterResolutionInKm;
    private final double offsetX;
    private final double offsetY;
    private final double subsamplingX;
    private final double subsamplingY;

    public GeoRaster(double[] longitudes, double[] latitudes, int rasterWidth, int rasterHeight, int sceneWidth, int sceneHeight,
                     double rasterResolutionInKm) {
        this(longitudes, latitudes, rasterWidth, rasterHeight, sceneWidth, sceneHeight, rasterResolutionInKm,
                0.5, 0.5, 1.0, 1.0);
    }

    public GeoRaster(double[] longitudes, double[] latitudes, int rasterWidth, int rasterHeight, int sceneWidth, int sceneHeight,
                     double rasterResolutionInKm, double offsetX, double offsetY, double subsamplingX, double subsamplingY) {
        this.longitudes = longitudes;
        this.latitudes = latitudes;
        this.rasterWidth = rasterWidth;
        this.rasterHeight = rasterHeight;
        this.sceneWidth = sceneWidth;
        this.sceneHeight = sceneHeight;
        this.rasterResolutionInKm = rasterResolutionInKm;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.subsamplingX = subsamplingX;
        this.subsamplingY = subsamplingY;
    }

    public double[] getLongitudes() {
        return longitudes;
    }

    public double[] getLatitudes() {
        return latitudes;
    }

    public int getRasterWidth() {
        return rasterWidth;
    }

    public int getRasterHeight() {
        return rasterHeight;
    }

    public int getSceneWidth() {
        return sceneWidth;
    }

    public int getSceneHeight() {
        return sceneHeight;
    }

    public double getRasterResolutionInKm() {
        return rasterResolutionInKm;
    }

    public double getOffsetX() {
        return offsetX;
    }

    public double getOffsetY() {
        return offsetY;
    }

    public double getSubsamplingX() {
        return subsamplingX;
    }

    public double getSubsamplingY() {
        return subsamplingY;
    }
}
