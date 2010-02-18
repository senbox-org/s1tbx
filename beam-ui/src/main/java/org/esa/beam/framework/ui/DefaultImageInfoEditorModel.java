package org.esa.beam.framework.ui;

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.datamodel.ImageInfo;

import java.awt.Color;

/**
 * Unstable interface. Do not use.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.5.1
 */
public class DefaultImageInfoEditorModel extends AbstractImageInfoEditorModel {

    public DefaultImageInfoEditorModel(ImageInfo imageInfo) {
        super(imageInfo);
        Assert.argument(imageInfo.getColorPaletteDef() != null, "imageInfo");
    }

    @Override
    public boolean isColorEditable() {
        return true;
    }

    @Override
    public int getSliderCount() {
        return getImageInfo().getColorPaletteDef().getNumPoints();
    }

    @Override
    public double getSliderSample(int index) {
        return getImageInfo().getColorPaletteDef().getPointAt(index).getSample();
    }

    @Override
    public void setSliderSample(int index, double sample) {
        getImageInfo().getColorPaletteDef().getPointAt(index).setSample(sample);
        fireStateChanged();
    }

    @Override
    public Color getSliderColor(int index) {
        return getImageInfo().getColorPaletteDef().getPointAt(index).getColor();
    }

    @Override
    public void setSliderColor(int index, Color color) {
        getImageInfo().getColorPaletteDef().getPointAt(index).setColor(color);
        fireStateChanged();
    }

    @Override
    public void createSliderAfter(int index) {
        final boolean b = getImageInfo().getColorPaletteDef().createPointAfter(index, getSampleScaling());
        if (b) {
            fireStateChanged();
        }
    }

    @Override
    public void removeSlider(int index) {
        getImageInfo().getColorPaletteDef().removePointAt(index);
        fireStateChanged();
    }

    @Override
    public Color[] createColorPalette() {
        return getImageInfo().getColorPaletteDef().createColorPalette(getSampleScaling());
    }

    @Override
    public boolean isGammaUsed() {
        return false;
    }

    @Override
    public double getGamma() {
        return 1;
    }

    @Override
    public void setGamma(double gamma) {
        // no support
    }

    @Override
    public byte[] getGammaCurve() {
        return null;
    }
}
