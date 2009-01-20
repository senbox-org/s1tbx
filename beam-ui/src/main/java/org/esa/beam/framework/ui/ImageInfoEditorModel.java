package org.esa.beam.framework.ui;

import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.Stx;
import org.esa.beam.framework.datamodel.Scaling;

import javax.swing.event.ChangeListener;
import java.awt.Color;

/**
 * todo - add API doc
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public interface ImageInfoEditorModel {
    ImageInfo getImageInfo();

    void setDisplayProperties(String unit, Stx stx, Scaling scaling);

    boolean isColorEditable();

    int getSliderCount();

    double getSliderSample(int index);

    void setSliderSample(int index, double sample);

    Color getSliderColor(int index);

    void setSliderColor(int index, Color color);

    void createSliderAfter(int index);

    void removeSlider(int removeIndex);

    Color[] createColorPalette();

    boolean isGammaUsed();

    double getGamma();

    void setGamma(double gamma);

    byte[] getGammaCurve();

    String getUnit();

    Scaling getScaling();

    double getMinSample();

    double getMaxSample();

    boolean isHistogramAvailable();

    int[] getHistogramBins();

    double getMinHistogramViewSample();

    void setMinHistogramViewSample(double minViewSample);

    double getMaxHistogramViewSample();

    void setMaxHistogramViewSample(double maxViewSample);

    double getHistogramViewGain();

    void setHistogramViewGain(double gain);

    boolean isHistogramAccurate();

    void addChangeListener(ChangeListener l);

    void removeChangeListener(ChangeListener l);

    void fireStateChanged();
}
