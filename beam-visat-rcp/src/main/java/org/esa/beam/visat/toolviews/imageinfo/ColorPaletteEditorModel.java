package org.esa.beam.visat.toolviews.imageinfo;

import org.esa.beam.framework.datamodel.ImageInfo;

import java.awt.Color;

/**
 * todo - add API doc
*
* @author Norman Fomferra
* @version $Revision$ $Date$
* @since BEAM 4.2
*/
class ColorPaletteEditorModel extends ImageInfoEditorModel {
    private final ImageInfo imageInfo;

    ColorPaletteEditorModel(ImageInfo imageInfo) {
        this.imageInfo = imageInfo;
    }

    @Override
    public boolean isColorEditable() {
        return true;
    }

    @Override
    public int getSliderCount() {
        return imageInfo.getColorPaletteDef().getNumPoints();
    }

    @Override
    public double getSliderSample(int index) {
        return imageInfo.getColorPaletteDef().getPointAt(index).getSample();
    }

    @Override
    public void setSliderSample(int index, double sample) {
        imageInfo.getColorPaletteDef().getPointAt(index).setSample(sample);
        fireStateChanged();
    }

    @Override
    public Color getSliderColor(int index) {
        return imageInfo.getColorPaletteDef().getPointAt(index).getColor();
    }

    @Override
    public void setSliderColor(int index, Color color) {
        imageInfo.getColorPaletteDef().getPointAt(index).setColor(color);
        fireStateChanged();
    }

    @Override
    public void createSliderAfter(int index) {
        final boolean b = imageInfo.getColorPaletteDef().createPointAfter(index, getScaling());
        if (b) {
            fireStateChanged();
        }
    }

    @Override
    public void removeSlider(int index) {
        imageInfo.getColorPaletteDef().removePointAt(index);
        fireStateChanged();
    }

    @Override
    public Color[] createColorPalette() {
        return imageInfo.getColorPaletteDef().createColorPalette(getScaling());
    }

    @Override
    public boolean isGammaActive() {
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
