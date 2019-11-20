package org.esa.s1tbx.io.gamma.pyrate.pyrateheader;

import org.esa.s1tbx.io.gamma.pyrate.PyRateGammaProductWriter;
import org.esa.snap.core.datamodel.Product;

import java.io.File;

public class PyRateHeaderDiffWriter extends PyRateHeaderWriter {
    public PyRateHeaderDiffWriter(PyRateGammaProductWriter writer, Product srcProduct, File userOutputFile) {
        super(writer, srcProduct, userOutputFile);
    }
}
