package org.esa.beam.visat.actions.masktools;

import org.esa.beam.framework.datamodel.Band;

import java.util.Comparator;

/**
* Created by Norman on 07.03.14.
*/
class SpectralBandComparator implements Comparator<Band> {
    @Override
    public int compare(Band b1, Band b2) {
        float deltaWl = b1.getSpectralWavelength() - b2.getSpectralWavelength();
        if (Math.abs(deltaWl) > 1e-05) {
            return deltaWl < 0 ? -1 : 1;
        }
        int deltaSi = b1.getSpectralBandIndex() - b2.getSpectralBandIndex();
        if (deltaSi != 0) {
            return deltaSi < 0 ? -1 : 1;
        }
        return b1.getName().compareTo(b2.getName());
    }
}
