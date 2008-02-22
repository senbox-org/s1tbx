/*
 * $id$
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.framework.datamodel;

import org.esa.beam.util.Debug;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.ObjectUtils;
import org.esa.beam.util.math.Histogram;
import org.esa.beam.util.math.MathUtils;

import java.awt.Color;
import java.awt.image.IndexColorModel;

/**
 * This class contains information about how a product's raster data node is displayed as an image.
 *
 * @author Norman Fomferra
 * @version $Revision: 1.1.1.1 $ $Date: 2006/09/11 08:16:45 $
 */
public class ImageInfo implements Cloneable {

    public static final String HISTOGRAM_MATCHING_OFF = "off";
    public static final String HISTOGRAM_MATCHING_EQUALIZE = "equalize";
    public static final String HISTOGRAM_MATCHING_NORMALIZE = "normalize";

    private float gamma;
    private ColorPaletteDef colorPaletteDef;
    private Color[] colorPalette;
    private float minSample;
    private float maxSample;
    private int[] histogramBins;
    private Scaling scaling;

    // Color palette view properties.
    // Used by ContrastStretchPane, properties currently not saved in DIMAP.
    private Float _histogramViewGain;
    private Float _minHistogramViewSample;
    private Float _maxHistogramViewSample;

    /**
     * Constructs a new basic display information instance.
     *
     * @param minSample     the statistical minimum sample value
     * @param maxSample     the statistical maximum sample value
     * @param histogramBins the histogram pixel counts, can be <code>null</code>
     */
    public ImageInfo(float minSample,
                     float maxSample,
                     int[] histogramBins) {
        this(minSample,
             maxSample,
             histogramBins,
             new ColorPaletteDef(256, minSample, maxSample));
    }

    /**
     * Constructs a new basic display information instance.
     *
     * @param minSample          the statistical minimum sample value
     * @param maxSample          the statistical maximum sample value
     * @param histogramBins      the histogram pixel counts, can be <code>null</code>
     * @param numColors          the number of colors for the color palette
     * @param colorPalettePoints the points of the gradation curve
     */
    public ImageInfo(float minSample,
                     float maxSample,
                     int[] histogramBins,
                     int numColors,
                     ColorPaletteDef.Point[] colorPalettePoints) {
        this(minSample,
             maxSample,
             histogramBins,
             new ColorPaletteDef(colorPalettePoints, numColors));
    }

    /**
     * Constructs a new basic display information instance.
     *
     * @param minSample       the statistical minimum sample value
     * @param maxSample       the statistical maximum sample value
     * @param histogramBins   the histogram pixel counts, can be <code>null</code>
     * @param numColors       the number of colors for the color palette
     * @param colorPaletteDef the color palette definition
     * @deprecated since 4.2, use {@link #ImageInfo(float, float, int[], ColorPaletteDef)} instead
     */
    public ImageInfo(float minSample,
                     float maxSample,
                     int[] histogramBins,
                     int numColors,
                     ColorPaletteDef colorPaletteDef) {
        this(minSample, maxSample, histogramBins, colorPaletteDef);
        this.colorPaletteDef.setNumColors(numColors);
    }

    /**
     * Constructs a new basic display information instance.
     *
     * @param minSample       the statistical minimum sample value
     * @param maxSample       the statistical maximum sample value
     * @param histogramBins   the histogram pixel counts, can be <code>null</code>
     * @param colorPaletteDef the color palette definition
     */
    public ImageInfo(float minSample,
                     float maxSample,
                     int[] histogramBins,
                     ColorPaletteDef colorPaletteDef) {
        Guardian.assertNotNull("colorPaletteDef", colorPaletteDef);
        this.minSample = minSample;
        this.maxSample = maxSample;
        this.histogramBins = histogramBins;
        this.colorPaletteDef = colorPaletteDef;
        scaling = Scaling.IDENTITY;
        colorPalette = null;
        gamma = 1.0f;
    }

    /**
     * Gets the histogram.
     *
     * @return the histogram, or <code>null</code> if a histogram is not available.
     */
    public Histogram getHistogram() {
        return isHistogramAvailable() ? new Histogram(histogramBins, minSample, maxSample) : null;
    }

    /**
     * Gets a suitable round factor for the given number of digits.
     *
     * @param numDigits the number of digits.
     * @return a suitable round factor
     * @see org.esa.beam.util.math.MathUtils#computeRoundFactor(double, double, int)
     */
    public double getRoundFactor(int numDigits) {
        return MathUtils.computeRoundFactor(getMinSample(), getMaxSample(), numDigits);
    }

    public Scaling getScaling() {
        return scaling;
    }

    public void setScaling(Scaling scaling) {
        Guardian.assertNotNull("scaling", scaling);
        this.scaling = scaling;
    }

    /**
     * @return the minimum sample value.
     */
    public float getMinSample() {
        return minSample;
    }

    /**
     * Sets the minimum sample value.
     *
     * @param minSample the minimum sample value.
     */
    public void setMinSample(float minSample) {
        this.minSample = minSample;
    }

    /**
     * @return the maximum sample value.
     */
    public float getMaxSample() {
        return maxSample;
    }


    /**
     * Sets the maximum sample value.
     *
     * @param maxSample the maximum sample value.
     */
    public void setMaxSample(float maxSample) {
        this.maxSample = maxSample;
    }

    /**
     * Gets the minimum display sample value for a linear contrast stretch operation.
     *
     * @return the minimum display sample
     */
    public double getMinDisplaySample() {
        Debug.assertNotNull(getColorPaletteDef());
        Debug.assertTrue(getColorPaletteDef().getNumPoints() >= 2);
        return getColorPaletteDef().getFirstPoint().getSample();
    }

    /**
     * Gets the maximum display sample value for a linear contrast stretch operation.
     *
     * @return the maximum display sample
     */
    public double getMaxDisplaySample() {
        Debug.assertNotNull(getColorPaletteDef());
        Debug.assertTrue(getColorPaletteDef().getNumPoints() >= 2);
        return getColorPaletteDef().getLastPoint().getSample();
    }

    /**
     * Gets the minimum sample value used for a histogram view.
     *
     * @return the minimum histogram view sample
     */
    public float getMinHistogramViewSample() {
        if (_minHistogramViewSample != null) {
            return _minHistogramViewSample;
        }
        return getMinSample();
    }

    /**
     * Sets the minimum sample value used for a histogram view.
     *
     * @param minViewSample the minimum histogram view sample
     */
    public void setMinHistogramViewSample(float minViewSample) {
        _minHistogramViewSample = minViewSample;
    }

    /**
     * Gets the maximum sample value used for a histogram view.
     *
     * @return the maximum histogram view sample
     */
    public float getMaxHistogramViewSample() {
        if (_maxHistogramViewSample != null) {
            return _maxHistogramViewSample;
        }
        return getMaxSample();
    }

    /**
     * Sets the maximum sample value used for a histogram view.
     *
     * @param maxViewSample the maximum histogram view sample
     */
    public void setMaxHistogramViewSample(float maxViewSample) {
        _maxHistogramViewSample = maxViewSample;
    }

    /**
     * Gets the gain (Y-axis scale factor) used for a histogram view.
     *
     * @return the histogram view gain
     */
    public float getHistogramViewGain() {
        if (_histogramViewGain != null) {
            return _histogramViewGain;
        }
        return 1.0f;
    }

    /**
     * Sets the maximum sample value used for a histogram view.
     *
     * @param gain the histogram view gain
     */
    public void setHistogramViewGain(float gain) {
        _histogramViewGain = gain;
    }

    public boolean isGammaActive() {
        return gamma >= 0 && gamma != 1.0;
    }

    public float getGamma() {
        return gamma;
    }

    public void setGamma(float gamma) {
        this.gamma = gamma;
    }

    /**
     * Gets the histogram pixel counts.
     *
     * @return the histogram pixel counts, can be <code>null</code> if not available
     */
    public int[] getHistogramBins() {
        return histogramBins;
    }

    /**
     * Gets whether or not a histogram is available.
     *
     * @return <code>true</code> if so
     */
    public boolean isHistogramAvailable() {
        return histogramBins != null && histogramBins.length > 0;
    }

    /**
     * Gets the number of bins which are visible in the histogram view.
     *
     * @return the number of bins which are visible, <code>-1</code> if a histogram is not available.
     */
    public float getHistogramViewBinCount() {
        if (!isHistogramAvailable()) {
            return -1;
        }
        if (getMinSample() != getMinHistogramViewSample() || getMaxSample() != getMaxHistogramViewSample()) {
            return (float)
                    (getHistogramBins().length
                            / (scaleInverse(getMaxSample()) - scaleInverse(getMinSample()))
                            * (scaleInverse(getMaxHistogramViewSample()) - scaleInverse(getMinHistogramViewSample()))
                    );
        }
        return getHistogramBins().length;
    }

    /**
     * Returns the float offset in the bins array for the first bin which are visible in the histogram view.
     *
     * @return the float offset in the bins array, <code>-1</code> if a histogram is not available.
     */
    public float getFirstHistogramViewBinIndex() {
        if (!isHistogramAvailable()) {
            return -1;
        }
        if (getMinSample() != getMinHistogramViewSample()) {
            return (float) ((getHistogramBins().length - 1)
                    / (scaleInverse(getMaxSample()) - scaleInverse(getMinSample()))
                    * (scaleInverse(getMinHistogramViewSample()) - scaleInverse(getMinSample())));
        }
        return 0;
    }

    /**
     * Gets the color palette definition used to compute the linear contrast streching limits and the color palette
     * gradients.
     *
     * @return the gradation curve, never <code>null</code>
     */
    public ColorPaletteDef getColorPaletteDef() {
        return colorPaletteDef;
    }

    /**
     * Sets the color palette definition used to compute the linear contrast streching limits and the color palette
     * gradients.
     *
     * @param colorPaletteDef the color palette definition, must not be <code>null</code>
     */
    public void setColorPaletteDef(ColorPaletteDef colorPaletteDef) {
        Guardian.assertNotNull("colorPaletteDef", colorPaletteDef);
        this.colorPaletteDef = colorPaletteDef;
    }

    /**
     * @return the number of colors used to compute the color palette.
     */
    public int getNumColors() {
        return colorPaletteDef.getNumColors();
    }

    /**
     * @return the color palette. If no such exists a new one is computed from the gradation curve.
     */
    public Color[] getColorPalette() {
        if (isColorPaletteOutOfDate()) {
            computeColorPalette();
        }
        return colorPalette;
    }

    /**
     * (Re-)Computes the color palette for this basic display information instance.
     */
    public void computeColorPalette() {
        this.colorPalette = colorPaletteDef.createColourPalette(this.scaling);
    }

    public IndexColorModel createColorModel() {
        Color[] palette = getColorPalette();
        final int numColors = palette.length;
        final byte[] red = new byte[numColors];
        final byte[] green = new byte[numColors];
        final byte[] blue = new byte[numColors];
        for (int i = 0; i < palette.length; i++) {
            Color color = palette[i];
            red[i] = (byte) color.getRed();
            green[i] = (byte) color.getGreen();
            blue[i] = (byte) color.getBlue();
        }
        return new IndexColorModel(numColors <= 256 ? 8 : 16, numColors, red, green, blue);
    }

    /**
     * Indicates whether some object object is "equal to" this basic display information instance.
     *
     * @param object the reference object with which to compare.
     * @return <code>true</code> if this object is the same as the obj argument; <code>false</code> otherwise
     */
    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        } else if (object instanceof ImageInfo) {
            ImageInfo other = (ImageInfo) object;
            if (other.getMinSample() != getMinSample()) {
                return false;
            }
            if (other.getMaxSample() != getMaxSample()) {
                return false;
            }
            if (other.getNumColors() != getNumColors()) {
                return false;
            }
            return ObjectUtils.equalObjects(other.getColorPaletteDef(), getColorPaletteDef());
        }
        return false;
    }

    /**
     * Creates and returns a copy of this object. The method simply returns the value of
     * <code>createDeepClone()</code>.
     *
     * @return a copy of this object
     */
    @Override
    public Object clone() {
        return createDeepCopy();
    }


    /**
     * Creates and returns a "deep" copy of this object.
     *
     * @return a copy of this object
     */
    public ImageInfo createDeepCopy() {
        ImageInfo imageInfo = new ImageInfo(getMinSample(),
                                            getMaxSample(),
                                            getHistogramBins(),
                                            getColorPaletteDef().createDeepCopy());
        imageInfo.setGamma(gamma);
        imageInfo.setMinHistogramViewSample(getMinHistogramViewSample());
        imageInfo.setMaxHistogramViewSample(getMaxHistogramViewSample());
        imageInfo.setHistogramViewGain(getHistogramViewGain());
        imageInfo.setScaling(getScaling());
        return imageInfo;
    }

    private boolean isColorPaletteOutOfDate() {
        return colorPalette == null || getColorPaletteDef().getNumColors() != colorPalette.length;
    }

    public double getNormalizedHistogramViewSampleValue(double sample) {
        final double minVisibleSample;
        final double maxVisibleSample;
        minVisibleSample = scaleInverse(getMinHistogramViewSample());
        maxVisibleSample = scaleInverse(getMaxHistogramViewSample());
        sample = scaleInverse(sample);
        double delta = maxVisibleSample - minVisibleSample;
        if (delta == 0 || Double.isNaN(delta)) {
            delta = 1;
        }
        return (sample - minVisibleSample) / delta;
    }

    public double getNormalizedDisplaySampleValue(double sample) {
        final double minDisplaySample;
        final double maxDisplaySample;
        minDisplaySample = scaleInverse(getMinDisplaySample());
        maxDisplaySample = scaleInverse(getMaxDisplaySample());
        sample = scaleInverse(sample);
        double delta = maxDisplaySample - minDisplaySample;
        if (delta == 0 || Double.isNaN(delta)) {
            delta = 1;
        }
        return (sample - minDisplaySample) / delta;
    }

    public void transferColorPaletteDef(final ImageInfo sourceImageInfo, boolean changeColorsOnly) {
        transferColorPaletteDef(sourceImageInfo.getColorPaletteDef(), changeColorsOnly);
    }

    public void transferColorPaletteDef(final ColorPaletteDef sourceCPD, boolean changeColorsOnly) {
        final ColorPaletteDef currentCPD = getColorPaletteDef();
        int deltaNumPoints = currentCPD.getNumPoints() - sourceCPD.getNumPoints();
        if (deltaNumPoints < 0) {
            for (; deltaNumPoints != 0; deltaNumPoints++) {
                currentCPD.insertPointAfter(0, new ColorPaletteDef.Point());
            }
        } else if (deltaNumPoints > 0) {
            for (; deltaNumPoints != 0; deltaNumPoints--) {
                currentCPD.removePointAt(1);
            }
        }
        if (changeColorsOnly) {
            for (int i = 0; i < sourceCPD.getNumPoints(); i++) {
                currentCPD.getPointAt(i).setColor(sourceCPD.getPointAt(i).getColor());
            }
        } else {
            double min1 = getMinSample();
            double max1 = getMaxSample();
            double min2 = sourceCPD.getFirstPoint().getSample();
            double max2 = sourceCPD.getLastPoint().getSample();
            double a, b;
            // Check if source range fits into this range
            if (min2 >= min1 && max2 <= max1) {
                // --> ok, no sample conversion
                a = 0.0;
                b = 1.0;
            } else {
                // --> sourcerange overlaps this range, sample conversion
                min1 = currentCPD.getFirstPoint().getSample();
                max1 = currentCPD.getLastPoint().getSample();
                double delta1 = (max1 > min1) ? max1 - min1 : 1;
                double delta2 = (max2 > min2) ? max2 - min2 : 1;
                a = min1 - min2 * delta1 / delta2;
                b = delta1 / delta2;
            }
            for (int i = 0; i < sourceCPD.getNumPoints(); i++) {
                currentCPD.getPointAt(i).setSample(a + b * sourceCPD.getPointAt(i).getSample());
                currentCPD.getPointAt(i).setColor(sourceCPD.getPointAt(i).getColor());
            }
        }
        computeColorPalette();
    }

    /**
     * Releases all of the resources used by this object instance and all of its owned children. Its primary use is to
     * allow the garbage collector to perform a vanilla job.
     * <p/>
     * <p>This method should be called only if it is for sure that this object instance will never be used again. The
     * results of referencing an instance of this class after a call to <code>dispose()</code> are undefined.
     * <p/>
     * <p>Overrides of this method should always call <code>super.dispose();</code> after disposing this instance.
     */
    public void dispose() {
        colorPalette = null;
        histogramBins = null;
        scaling = null;
        if (colorPaletteDef != null) {
            colorPaletteDef.dispose();
            colorPaletteDef = null;
        }
    }

    private double scaleInverse(double value) {
        return scaling.scaleInverse(value);
    }
}
