package org.jlinda.core.utils;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.engine_utilities.datamodel.Unit;

public class BandUtilsDoris {

    public static boolean isBandReal(final Band band) {
        return band.getUnit().contains(Unit.REAL);
    }

    public static boolean isBandImag(final Band band) {
        return band.getUnit().contains(Unit.IMAGINARY);
    }

}
