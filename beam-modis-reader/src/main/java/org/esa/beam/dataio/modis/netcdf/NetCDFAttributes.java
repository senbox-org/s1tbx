package org.esa.beam.dataio.modis.netcdf;

import ucar.nc2.Variable;

import java.util.List;

public class NetCDFAttributes {

    final java.util.List<Variable> variables;

    public NetCDFAttributes(List<Variable> variables) {
        this.variables = variables;
    }
}
