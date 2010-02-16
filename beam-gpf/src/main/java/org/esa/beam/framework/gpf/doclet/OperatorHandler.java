package org.esa.beam.framework.gpf.doclet;

import com.sun.javadoc.RootDoc;

public interface OperatorHandler {
    void start(RootDoc root) throws Exception;
    void stop(RootDoc root) throws Exception;
    void processOperator(OperatorDesc operatorDesc) throws Exception;
}
