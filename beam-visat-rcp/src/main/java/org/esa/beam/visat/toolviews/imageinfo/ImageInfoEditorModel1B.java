package org.esa.beam.visat.toolviews.imageinfo;

import org.esa.beam.framework.datamodel.ImageInfo;

import java.awt.Color;

import com.bc.ceres.core.Assert;

/**
 * todo - add API doc
*
* @author Norman Fomferra
* @version $Revision$ $Date$
* @since BEAM 4.2
*/
class ImageInfoEditorModel1B extends ImageInfoEditorModel {

    ImageInfoEditorModel1B(ImageInfo imageInfo) {
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
        final boolean b = getImageInfo().getColorPaletteDef().createPointAfter(index, getScaling());
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
        return getImageInfo().getColorPaletteDef().createColorPalette(getScaling());
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
