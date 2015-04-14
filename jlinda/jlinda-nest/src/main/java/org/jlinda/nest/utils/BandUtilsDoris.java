package org.jlinda.nest.utils;

import org.esa.snap.datamodel.Unit;
import org.esa.snap.framework.datamodel.Band;

public class BandUtilsDoris {

    public static boolean isBandReal(final Band band) {
        return band.getUnit().contains(Unit.REAL);
    }

    public static boolean isBandImag(final Band band) {
        return band.getUnit().contains(Unit.IMAGINARY);
    }

}
