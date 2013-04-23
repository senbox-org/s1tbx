package org.esa.beam.framework.ui.product.spectrum;

import org.esa.beam.framework.datamodel.Band;

public interface Spectrum {

    String getName();

    Band[] getSpectralBands();

    boolean hasBands();

}
