package org.esa.beam.binning.reader;

import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Variable;

import java.io.IOException;

class VariableReader {

    private final Variable binVariable;
    private final int layer;
    private int maxExtent;

    VariableReader(Variable binVariable) {
        this.binVariable = binVariable;
        this.layer = -1;
    }

    VariableReader(Variable binVariable, int layer) {
        this.binVariable = binVariable;
        this.layer = layer;
        maxExtent = binVariable.getDimension(0).getLength();
    }

    Variable getBinVariable() {
        return binVariable;
    }

    Array readFully() throws IOException {
        if (layer < 0) {
            return binVariable.read();
        } else {
            try {
                final int[] origin = new int[]{0, layer};
                final int[] shape = new int[]{maxExtent, 1};
                return binVariable.read(origin, shape);
            } catch (InvalidRangeException e) {
                throw new IOException(e.getMessage());
            }
        }
    }

    Array read(int[] origin, int[] shape) throws IOException {
        try {
            if (layer < 0) {
                return binVariable.read(origin, shape);
            } else {
                final int[] originFull = new int[]{origin[0], layer};
                final int[] shapeFull = new int[]{shape[0], 1};
                return binVariable.read(originFull, shapeFull);
            }
        } catch (InvalidRangeException e) {
            throw new IOException(e.getMessage());
        }
    }
}
