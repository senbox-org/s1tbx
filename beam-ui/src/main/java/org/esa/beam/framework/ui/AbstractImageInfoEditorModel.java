package org.esa.beam.framework.ui;

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.Scaling;
import org.esa.beam.framework.datamodel.Stx;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;


public abstract class AbstractImageInfoEditorModel implements ImageInfoEditorModel {

    private final ImageInfo imageInfo;
    private final EventListenerList listenerList;
    private Scaling scaling;
    private Stx stx;
    private String unit;
    private Double histogramViewGain;
    private Double minHistogramViewSample;
    private Double maxHistogramViewSample;

    protected AbstractImageInfoEditorModel(ImageInfo imageInfo) {
        this.imageInfo = imageInfo;
        this.listenerList = new EventListenerList();
    }

    public ImageInfo getImageInfo() {
        return imageInfo;
    }

    public void setDisplayProperties(String unit, Stx stx, Scaling scaling) {
        setUnit(unit);
        setScaling(scaling);
        setStx(stx);
        fireStateChanged();
    }

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

    private void setStx(Stx stx) {
        Assert.notNull(stx, "stx");
        this.stx = stx;
    }

    public double getMinSample() {
        return scaling == null ? 0 : scaling.scale(stx.getMin());
    }

    public double getMaxSample() {
        return scaling == null ? 0 : scaling.scale(stx.getMax());
    }

    public boolean isHistogramAvailable() {
        return getHistogramBins() != null && getHistogramBins().length > 0;
    }

    public int[] getHistogramBins() {
        return stx == null ? null : stx.getHistogramBins();
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

    public boolean isHistogramAccurate() {
        return stx.getResolutionLevel() == 0;
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
