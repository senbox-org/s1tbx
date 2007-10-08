package org.esa.beam.framework.gpf.operators.common;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.AbstractOperator;
import org.esa.beam.framework.gpf.AbstractOperatorSpi;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;

/**
 * Simply passes the source product through the operator.
 */
public class NoOp extends AbstractOperator {
    @SourceProduct
    private Product sourceProduct;

    @TargetProduct
    private Product targetProduct;


    @Override
    protected Product initialize() throws OperatorException {
        targetProduct = sourceProduct;
        return targetProduct;
    }

    @Override
    public void computeTile(Band band, Tile targetTile) throws OperatorException {
        return;
    }

    public static class Spi extends AbstractOperatorSpi {
        public Spi() {
            super(NoOp.class, "NoOp");
        }
    }
}
