package org.jlinda.nest.utils;

import org.esa.beam.framework.datamodel.Band;
import org.esa.nest.datamodel.Unit;

public class BandUtilsDoris {

    public static boolean isBandReal(final Band band) {
        return band.getUnit().contains(Unit.REAL);
    }

    public static boolean isBandImag(final Band band) {
        return band.getUnit().contains(Unit.IMAGINARY);
    }

}