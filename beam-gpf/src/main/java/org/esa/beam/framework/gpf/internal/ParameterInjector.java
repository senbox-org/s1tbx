package org.esa.beam.framework.gpf.internal;

import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;


public interface ParameterInjector {
    void injectParameters(Operator operator) throws OperatorException;
}
