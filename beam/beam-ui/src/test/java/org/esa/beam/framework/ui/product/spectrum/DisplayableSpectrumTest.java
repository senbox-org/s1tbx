package org.esa.beam.framework.ui.product.spectrum;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ProductData;

/**
 * Created by E1001827 on 21.2.2014.
 */
public class DisplayableSpectrumTest extends TestCase {

    public void testNewDisplayableSpectrumIsSetupCorrectly() {
        String spectrumName = "name";
        DisplayableSpectrum displayableSpectrum = new DisplayableSpectrum(spectrumName, 1);

        assertEquals(spectrumName, displayableSpectrum.getName());
        assertEquals(DisplayableSpectrum.NO_UNIT, displayableSpectrum.getUnit());
        assertEquals(null, displayableSpectrum.getLineStyle());
        assertEquals(SpectrumShapeProvider.DEFAULT_SCALE_GRADE, displayableSpectrum.getSymbolSize());
        assertEquals(SpectrumShapeProvider.getScaledShape(1, SpectrumShapeProvider.DEFAULT_SCALE_GRADE),
                displayableSpectrum.getScaledShape());
        assertEquals(1, displayableSpectrum.getSymbolIndex());
        assertEquals(true, displayableSpectrum.isSelected());
        assertEquals(false, displayableSpectrum.isRemainingBandsSpectrum());
        assertEquals(false, displayableSpectrum.hasBands());
        assertEquals(0, displayableSpectrum.getSpectralBands().length);
        assertEquals(0, displayableSpectrum.getSelectedBands().length);
    }

    public void testNewDisplayableSpectrumIsSetUpCorrectlyWithBands() {
        String spectrumName = "name";
        SpectrumBand[] spectralBands = new SpectrumBand[2];
        for (int i = 0; i < spectralBands.length; i++) {
            Band band = createBand(i);
            band.setUnit("unit");
            spectralBands[i] = new SpectrumBand(band, true);
        }
        DisplayableSpectrum displayableSpectrum = new DisplayableSpectrum(spectrumName, spectralBands, 1);

        assertEquals(spectrumName, displayableSpectrum.getName());
        assertEquals("unit", displayableSpectrum.getUnit());
        assertEquals(true, displayableSpectrum.hasBands());
        assertEquals(2, displayableSpectrum.getSpectralBands().length);
        assertEquals(2, displayableSpectrum.getSelectedBands().length);
        assertEquals(true, displayableSpectrum.isBandSelected(0));
        assertEquals(true, displayableSpectrum.isBandSelected(1));
    }

    public void testBandsAreAddedCorrectlyToDisplayableSpectrum() {
        String spectrumName = "name";
        DisplayableSpectrum displayableSpectrum = new DisplayableSpectrum(spectrumName, 1);
        SpectrumBand[] bands = new SpectrumBand[3];
        for (int i = 0; i < bands.length; i++) {
            Band band = createBand(i);
            band.setUnit("unit" + i);
            bands[i] = new SpectrumBand(band, i%2 == 0);
            displayableSpectrum.addBand(bands[i]);
        }

        assertEquals(spectrumName, displayableSpectrum.getName());
        assertEquals(DisplayableSpectrum.MIXED_UNITS, displayableSpectrum.getUnit());
        assertEquals(true, displayableSpectrum.hasBands());
        assertEquals(3, displayableSpectrum.getSpectralBands().length);
        assertEquals(bands[0].getOriginalBand(), displayableSpectrum.getSpectralBands()[0]);
        assertEquals(bands[1].getOriginalBand(), displayableSpectrum.getSpectralBands()[1]);
        assertEquals(bands[2].getOriginalBand(), displayableSpectrum.getSpectralBands()[2]);
        assertEquals(2, displayableSpectrum.getSelectedBands().length);
        assertEquals(bands[0].getOriginalBand(), displayableSpectrum.getSpectralBands()[0]);
        assertEquals(bands[2].getOriginalBand(), displayableSpectrum.getSpectralBands()[2]);
        assertEquals(true, displayableSpectrum.isBandSelected(0));
        assertEquals(false, displayableSpectrum.isBandSelected(1));
        assertEquals(true, displayableSpectrum.isBandSelected(2));
    }

    private Band createBand(int number) {
        return new Band("name" + number, ProductData.TYPE_INT8, 1, 1);
    }

}
