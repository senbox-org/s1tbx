package org.esa.beam.framework.ui.product.spectrum;

import com.jidesoft.utils.Lm;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ProductData;

public class SpectrumChooserMainForManualTesting {
    /*
     * Used for testing UI
     */
    public static void main(String[] args) {
        Lm.verifyLicense("Brockmann Consult", "BEAM", "lCzfhklpZ9ryjomwWxfdupxIcuIoCxg2");

        final DisplayableSpectrum[] spectra = new DisplayableSpectrum[3];

        spectra[0] = createSpectrum(0);
        spectra[1] = createSpectrum(1);
        spectra[2] = new DisplayableSpectrum(DisplayableSpectrum.ALTERNATIVE_DEFAULT_SPECTRUM_NAME, 3);
        spectra[2].addBand(createBand(11));

        SpectrumChooser chooser = new SpectrumChooser(null, spectra, "");
        chooser.show();
        System.exit(0);
    }

    private static DisplayableSpectrum createSpectrum(int offset) {
        int numBands = 5;
        String name = "Radiances";
        final DisplayableSpectrum spectrum = new DisplayableSpectrum(name + " " + (offset + 1), offset + 1);
        final boolean selected = offset % 2 == 1;
        spectrum.setSelected(selected);
        final int bandOffset = numBands * offset;
        for (int i = 0; i < numBands; i++) {
            spectrum.addBand(createBand(i + bandOffset));
        }
        return spectrum;
    }

    static private SpectrumBand createBand(int index) {
        final Band band = new Band("Radiance_" + (index + 1), ProductData.TYPE_INT16, 100, 100);
        band.setDescription("Radiance for band " + (index + 1));
        band.setSpectralWavelength((float) Math.random());
        band.setSpectralBandwidth((float) Math.random());
        band.setUnit("sr^-1");
        if (index == 7) {
            band.setUnit("dl");
        }
        return new SpectrumBand(band, true);
    }

}
