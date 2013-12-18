package org.esa.beam.framework.ui.product.spectrum;

import com.jidesoft.utils.Lm;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ProductData;

import javax.swing.JButton;
import javax.swing.JFrame;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SpectrumChooserMainForManualTesting {
    /*
     * Used for testing UI
     */
    public static void main(String[] args) {
        Lm.verifyLicense("Brockmann Consult", "BEAM", "lCzfhklpZ9ryjomwWxfdupxIcuIoCxg2");

        final DisplayableSpectrum[] spectra = new DisplayableSpectrum[3];

        spectra[0] = createSpectrum(0);
        spectra[1] = createSpectrum(1);
        spectra[2] = new DisplayableSpectrum(DisplayableSpectrum.ALTERNATIVE_DEFAULT_SPECTRUM_NAME);
        spectra[2].addBand(createBand(11), spectra[2].isSelected());

        final JFrame frame = new JFrame();
        frame.setSize(new Dimension(100, 100));
        JButton button = new JButton("Choose Spectrum");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SpectrumChooser chooser = new SpectrumChooser(frame, spectra, "");
                chooser.show();
            }
        });
        frame.add(button);
        frame.setVisible(true);
    }

    private static DisplayableSpectrum createSpectrum(int offset) {
        int numBands = 5;
        String name = "Radiances";
        Band[] bands = new Band[numBands];
        final int bandOffset = numBands * offset;
        for (int i = 0; i < numBands; i++) {
            bands[i] = createBand(i + bandOffset);
        }
        return new DisplayableSpectrum(name + " " + (offset + 1), bands);
    }

    /*
     * Used for testing UI
     */
    static private Band createBand(int index) {
        final Band band = new Band("Radiance_" + (index + 1), ProductData.TYPE_INT16, 100, 100);
        band.setDescription("Radiance for band " + (index + 1));
        band.setSpectralWavelength((float) Math.random());
        band.setSpectralBandwidth((float) Math.random());
        return band;
    }

}
