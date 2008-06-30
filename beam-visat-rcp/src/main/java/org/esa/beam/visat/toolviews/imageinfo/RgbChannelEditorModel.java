package org.esa.beam.visat.toolviews.imageinfo;

import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.util.math.Range;
import org.esa.beam.util.math.MathUtils;

import java.awt.Color;

/**
 * todo - add API doc
*
* @author Norman Fomferra
* @version $Revision$ $Date$
* @since BEAM 4.2
*/
class RgbChannelEditorModel extends ImageInfoEditorModel {
    private final static Color[] RGB_COLORS = new Color[]{Color.RED, Color.GREEN, Color.BLUE};

    private final ImageInfo imageInfo;
    private final int channel;
    private byte[] gammaCurve;

    RgbChannelEditorModel(ImageInfo imageInfo, int channel) {
        this.imageInfo = imageInfo;
        this.channel = channel;
    }

    @Override
    public int getSliderCount() {
        return 2;
    }

    @Override
    public double getSliderSample(int index) {
        final Range range = imageInfo.getRgbProfile().getSampleDisplayRange(channel);
        if (index == 0) {
            return range.getMin();
        } else {
            return range.getMax();
        }
    }

    @Override
    public void setSliderSample(int index, double sample) {
        final Range range = imageInfo.getRgbProfile().getSampleDisplayRange(channel);
        if (index == 0) {
             range.setMin(sample);
        } else {
             range.setMax(sample);
        }
        fireStateChanged();
    }

    @Override
    public boolean isColorEditable() {
        return false;
    }

    @Override
    public Color getSliderColor(int index) {
        if (index == 0) {
            return Color.BLACK;
        } else {
            return RGB_COLORS[channel];
        }
    }

    @Override
    public void setSliderColor(int index, Color color) {
        throw new IllegalStateException("not implemented for RGB");
    }

    @Override
    public void createSliderAfter(int index) {
        throw new IllegalStateException("not implemented for RGB");
    }

    @Override
    public void removeSlider(int removeIndex) {
        throw new IllegalStateException("not implemented for RGB");
    }

    @Override
    public Color[] createColorPalette() {
        Color color = RGB_COLORS[channel];
        Color[] palette = new Color[256];
        final int redMult = color.getRed() / 255;
        final int greenMult = color.getGreen() / 255;
        final int blueMult = color.getBlue() / 255;
        for (int i = 0; i < palette.length; i++) {
            int j = i;
            if (gammaCurve != null) {
                j = gammaCurve[i] & 0xff;
            }
            final int r = j * redMult;
            final int g = j * greenMult;
            final int b = j * blueMult;
            palette[i] = new Color(r, g, b);
        }
        return palette;
    }

    @Override
    public boolean isGammaActive() {
        return imageInfo.getRgbProfile().isSampleDisplayGammaActive(channel);
    }

    @Override
    public double getGamma() {
        return imageInfo.getRgbProfile().getSampleDisplayGamma(channel);
    }

    @Override
    public void setGamma(double gamma) {
        imageInfo.getRgbProfile().setSampleDisplayGamma(channel, gamma);
        gammaCurve = null;
        fireStateChanged();
    }

    @Override
    public byte[] getGammaCurve() {
        updateGammaCurve();
        return gammaCurve;
    }

    protected void updateGammaCurve() {
        if (isGammaActive() && gammaCurve == null) {
            gammaCurve = MathUtils.createGammaCurve(getGamma(), gammaCurve);
        } else {
            gammaCurve = null;
        }
    }
}
