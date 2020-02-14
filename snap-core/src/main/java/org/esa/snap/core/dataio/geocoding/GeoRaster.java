package org.esa.snap.core.dataio.geocoding;

public class GeoRaster {

    private final double[] longitudes;
    private final double[] latitudes;
    private String lonVariableName;
    private String latVariableName;
    private final int rasterWidth;
    private final int rasterHeight;
    private final int sceneWidth;
    private final int sceneHeight;
    private final double rasterResolutionInKm;
    private final double offsetX;
    private final double offsetY;
    private final double subsamplingX;
    private final double subsamplingY;

    /**
     * Constructs a geoRaster; convenience constructor for pixel geolocation raster with the center pixel as reference location.
     * Sets subsampling to 1.0 and offset to 0.5 for x- and y-axis. It also sets sceneWidth and height to rasterWidth and height.
     *
     * @param longitudes           the longitude data
     * @param latitudes            the latitude data
     * @param lonVariableName      variable name of data origin for longitude data
     * @param latVariableName      variable name of data origin for latitude data
     * @param rasterWidth          width of the data raster
     * @param rasterHeight         height of the data raster
     * @param rasterResolutionInKm ground resolution of pixels
     */
    public GeoRaster(double[] longitudes, double[] latitudes, String lonVariableName, String latVariableName,
                     int rasterWidth, int rasterHeight, double rasterResolutionInKm) {
        this(longitudes, latitudes, lonVariableName, latVariableName, rasterWidth, rasterHeight, rasterWidth, rasterHeight, rasterResolutionInKm,
                0.5, 0.5, 1.0, 1.0);
    }

    /**
     * Constructs a geoRaster
     *
     * @param longitudes           the longitude data
     * @param latitudes            the latitude data
     * @param lonVariableName      variable name of data origin for longitude data
     * @param latVariableName      variable name of data origin for latitude data
     * @param rasterWidth          width of the data raster
     * @param rasterHeight         height of the data raster
     * @param sceneWidth           width of the scene raster
     * @param sceneHeight          height of the scene raster
     * @param rasterResolutionInKm ground resolution of pixels
     * @param offsetX              offset along x-axis
     * @param offsetY              offset along y-axis
     * @param subsamplingX         subsampling along x-axis
     * @param subsamplingY         subsampling along y-axis
     */
    public GeoRaster(double[] longitudes, double[] latitudes, String lonVariableName, String latVariableName,
                     int rasterWidth, int rasterHeight, int sceneWidth, int sceneHeight,
                     double rasterResolutionInKm, double offsetX, double offsetY, double subsamplingX, double subsamplingY) {
        this.longitudes = longitudes;
        this.latitudes = latitudes;
        this.lonVariableName = lonVariableName;
        this.latVariableName = latVariableName;
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

    public String getLonVariableName() {
        return lonVariableName;
    }

    public String getLatVariableName() {
        return latVariableName;
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
