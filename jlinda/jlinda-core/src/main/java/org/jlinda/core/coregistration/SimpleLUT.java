package org.jlinda.core.coregistration;

import ch.qos.logback.classic.Logger;
import org.jblas.DoubleMatrix;
import org.slf4j.LoggerFactory;

public class SimpleLUT extends LUT {

    private static final Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(SimpleLUT.class);

    public SimpleLUT(String method) {
        super(method);
    }

    public SimpleLUT(String method, int kernelLength) {
        super(method, kernelLength);
    }

    @Override
    public void constructLUT() {

        double[] kernelAxis = defineAxis(kernelLength);

        // temp matrices
        DoubleMatrix kernelTmp;

        // initialization
        axis = new DoubleMatrix(kernelAxis); // static for simple LUT
        kernel = new DoubleMatrix(nInterval, kernelLength);

        for (int i = 0; i < nInterval; i++) {
            if (method.equals(RECT)) {
                kernelTmp = new DoubleMatrix(rect(kernelAxis));
                kernel.putRow(i, kernelTmp);
            } else if (method.equals(TRI)) {
                kernelTmp = new DoubleMatrix(tri(kernelAxis));
                kernel.putRow(i, kernelTmp);
            } else if (method.equals(TS6P)) {
                kernelTmp = new DoubleMatrix(ts6(kernelAxis));
                kernel.putRow(i, kernelTmp);
            } else if (method.equals(TS8P)) {
                kernelTmp = new DoubleMatrix(ts8(kernelAxis));
                kernel.putRow(i, kernelTmp);
            } else if (method.equals(TS16P)) {
                kernelTmp = new DoubleMatrix(ts16(kernelAxis));
                kernel.putRow(i, kernelTmp);
            } else if (method.equals(CC4P)) {
                kernelTmp = new DoubleMatrix(cc4(kernelAxis));
                kernel.putRow(i, kernelTmp);
            } else if (method.equals(CC6P)) {
                kernelTmp = new DoubleMatrix(cc6(kernelAxis));
                kernel.putRow(i, kernelTmp);
            }

            kernelAxis = new DoubleMatrix(kernelAxis).sub(dx).toArray();

        }

        // normalization and back to array
        kernel.divColumnVector(kernel.rowSums());

    }

    @Override
    public void overviewLUT() {

        logger.debug("Overview of Simplified LUT for interpolation");
        logger.debug("--------------------------------------------");

        for (int i = 0; i < nInterval; ++i) {

            // logger
            logger.debug("Kernel row [{}]: {} :", i, kernel.getRow(i).toString());

        }

    }

}
