package org.esa.pfa.fe.op.out;

import com.bc.ceres.binding.PropertySet;
import org.esa.beam.framework.datamodel.Product;
import org.esa.pfa.fe.op.FeatureType;

import java.io.Closeable;
import java.io.IOException;

/**
* @author Norman Fomferra
*/
public interface PatchWriter extends PatchOutput, Closeable {
    void initialize(PropertySet configuration, Product sourceProduct, FeatureType... featureTypes) throws IOException;
}
