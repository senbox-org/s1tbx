package org.esa.beam.dataio.netcdf;

import ucar.nc2.Variable;

/**
 * Represents an extract of all variables that could be converted to bands.
 */
public class NcRasterDigest {

    private final NcRasterDim _rasterDim;
    private final Variable[] _variables;

    public NcRasterDigest(NcRasterDim rasterDim, Variable[] variables) {
        _rasterDim = rasterDim;
        _variables = variables;
    }

    public NcRasterDim getRasterDim() {
        return _rasterDim;
    }

    public Variable[] getRasterVariables() {
        return _variables;
    }
}
