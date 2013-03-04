package org.esa.beam.framework.ui.product.spectrum;

import com.jidesoft.utils.Lm;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ProductData;

import javax.swing.JButton;
import javax.swing.JFrame;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class SpectrumChooserMainForManualTesting {
    /*
     * Used for testing UI
     */
    public static void main(String[] args) {
        Lm.verifyLicense("Brockmann Consult", "BEAM", "lCzfhklpZ9ryjomwWxfdupxIcuIoCxg2");

        String name = "Radiances";
        int numBands = 5;
        Band[] bands = new Band[numBands];
        for (int i = 0; i < bands.length; i++) {
            bands[i] = createBand(i);
        }
        DisplayableSpectrum spectrum = new DisplayableSpectrum(name, bands);
        final List<DisplayableSpectrum> spectra = new ArrayList<DisplayableSpectrum>();
        spectra.add(spectrum);
        final JFrame frame = new JFrame();
        frame.setSize(new Dimension(100, 100));
        JButton button = new JButton("Choose Spectrum");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SpectrumChooser chooser = new SpectrumChooser(frame, spectra, spectra, "");
                chooser.show();
            }
        });
        frame.add(button);
        frame.setVisible(true);
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
