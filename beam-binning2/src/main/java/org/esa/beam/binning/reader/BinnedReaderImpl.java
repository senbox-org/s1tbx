package org.esa.beam.binning.reader;

import org.esa.beam.framework.datamodel.Band;
import ucar.ma2.Array;
import ucar.nc2.Variable;

import java.io.IOException;

abstract class BinnedReaderImpl {

    abstract void dispose();

    abstract Array getLineValues(Band destBand, Variable binVariable, int lineIndex) throws IOException;

    abstract int getBinIndexInGrid(int binIndex, int lineIndex);

    abstract int getStartBinIndex();

    abstract int getEndBinIndex(int lineIndex);
}
