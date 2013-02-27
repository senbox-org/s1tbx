package org.esa.beam.framework.ui.product.spectrum;

import org.esa.beam.framework.datamodel.Band;

public interface Spectrum {


    public String getName();

    public void setName(String name);

    public Band[] getSpectralBands();

    public void addBand(Band spectralBand);

    public boolean hasBands();

}
