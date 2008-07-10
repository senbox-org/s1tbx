package org.esa.beam.visat.toolviews.imageinfo;

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.datamodel.Scaling;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.ImageInfo;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import java.awt.Color;


abstract class ImageInfoEditorModel {

    private final ImageInfo imageInfo;
    protected final EventListenerList listenerList;
    private Scaling scaling;
    private String unit;
    private double minSample;
    private double maxSample;
    private int[] histogramBins;
    private Double histogramViewGain;
    private Double minHistogramViewSample;
    private Double maxHistogramViewSample;

    protected ImageInfoEditorModel(ImageInfo imageInfo) {
        this.imageInfo = imageInfo;
        this.listenerList = new EventListenerList();
    }

    public ImageInfo getImageInfo() {
        return imageInfo;
    }

    public void setDisplayProperties(RasterDataNode raster) {
        setUnit(raster.getUnit());
        setScaling(raster);
        if (raster.getStx() != null) {
            setMinSample(raster.scale(raster.getStx().getMinSample()));
            setMaxSample(raster.scale(raster.getStx().getMaxSample()));
            setHistogramBins(raster.getStx().getSampleFrequencies());
        } else {
            setMinSample(imageInfo.getColorPaletteDef().getMinDisplaySample());
            setMaxSample(imageInfo.getColorPaletteDef().getMaxDisplaySample());
            setHistogramBins(null);
        }
        fireStateChanged();
    }

    public abstract boolean isColorEditable();

    public abstract int getSliderCount();

    public abstract double getSliderSample(int index);

    public abstract void setSliderSample(int index, double sample);

    public abstract Color getSliderColor(int index);

    public abstract void setSliderColor(int index, Color color);

    public abstract void createSliderAfter(int index);

    public abstract void removeSlider(int removeIndex);

    public abstract Color[] createColorPalette();

    public abstract boolean isGammaUsed();

    public abstract double getGamma();

    public abstract void setGamma(double gamma);

    public abstract byte[] getGammaCurve();

    public String getUnit() {
        return unit;
    }

    private void setUnit(String unit) {
        this.unit = unit;
    }

    public Scaling getScaling() {
        return scaling;
    }

    private void setScaling(Scaling scaling) {
        Assert.notNull(scaling, "scaling");
        this.scaling = scaling;
    }

    public double getMinSample() {
        return minSample;
    }

    private void setMinSample(double minSample) {
        this.minSample = minSample;
    }

    public double getMaxSample() {
        return maxSample;
    }

    private void setMaxSample(double maxSample) {
        this.maxSample = maxSample;
    }

    public boolean isHistogramAvailable() {
        return histogramBins != null && histogramBins.length > 0;
    }

    public int[] getHistogramBins() {
        return histogramBins;
    }

    private void setHistogramBins(int[] histogramBins) {
        this.histogramBins = histogramBins;
    }

    public double getMinHistogramViewSample() {
        if (minHistogramViewSample != null) {
            return minHistogramViewSample;
        }
        return getMinSample();
    }

    public void setMinHistogramViewSample(double minViewSample) {
        minHistogramViewSample = minViewSample;
    }

    public double getMaxHistogramViewSample() {
        if (maxHistogramViewSample != null) {
            return maxHistogramViewSample;
        }
        return getMaxSample();
    }

    public void setMaxHistogramViewSample(double maxViewSample) {
        maxHistogramViewSample = maxViewSample;
    }

    public double getHistogramViewGain() {
        if (histogramViewGain != null) {
            return histogramViewGain;
        }
        return 1.0;
    }

    public void setHistogramViewGain(double gain) {
        histogramViewGain = gain;
    }

    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }

    public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }

    public void fireStateChanged() {
        final ChangeEvent event = new ChangeEvent(this);
        ChangeListener[] changeListeners = listenerList.getListeners(ChangeListener.class);
        for (ChangeListener changeListener : changeListeners) {
            changeListener.stateChanged(event);
        }
    }
}
