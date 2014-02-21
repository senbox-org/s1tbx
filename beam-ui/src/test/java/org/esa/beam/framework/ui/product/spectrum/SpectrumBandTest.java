package org.esa.beam.framework.ui.product.spectrum;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ProductData;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by E1001827 on 21.2.2014.
 */
public class SpectrumBandTest {

    private Band band;

    @Before
    public void setUp() {
        band = new Band("name", ProductData.TYPE_INT8, 1, 1);
        band.setUnit("unit");
    }

    @Test
    public void testSpectrumBandIsNotCreatedFromNullBand() {
        try {
            SpectrumBand spectrumBand = new SpectrumBand(null, true);
            Assert.fail("Exception expected");
        } catch (NullPointerException npe) {
            Assert.assertEquals(npe.getMessage(), "Assert.notNull(null) called");
        }
    }

    @Test
    public void testSpectrumBandIsCreatedCorrectlyWithFalseInitialState() {
        SpectrumBand spectrumBand = new SpectrumBand(band, true);

        Assert.assertNotNull(spectrumBand);
        Assert.assertEquals(true, spectrumBand.isSelected());

        spectrumBand.setSelected(false);

        Assert.assertEquals(false, spectrumBand.isSelected());
    }

    @Test
    public void testSpectrumBandIsCreatedCorrectlyWithTrueInitialState() {
        SpectrumBand spectrumBand = new SpectrumBand(band, false);

        Assert.assertNotNull(spectrumBand);
        Assert.assertEquals(false, spectrumBand.isSelected());

        spectrumBand.setSelected(true);

        Assert.assertEquals(true, spectrumBand.isSelected());
    }

    @Test
    public void testGetUnit() {
        SpectrumBand spectrumBand = new SpectrumBand(band, false);

        Assert.assertEquals("unit", spectrumBand.getUnit());
    }

    @Test
    public void testGetOriginalBand() {
        SpectrumBand spectrumBand = new SpectrumBand(band, false);

        Assert.assertEquals(band, spectrumBand.getOriginalBand());
    }

}
