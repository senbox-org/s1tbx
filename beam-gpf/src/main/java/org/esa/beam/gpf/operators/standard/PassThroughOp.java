package org.esa.beam.gpf.operators.standard;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;

@OperatorMetadata(alias = "PassThrough",
                  description = "Sets target product to source product.",
                  internal = true)
public class PassThroughOp extends Operator {
    @SourceProduct
    private Product sourceProduct;

    @TargetProduct
    private Product targetProduct;

    public PassThroughOp() {
    }

    public PassThroughOp(Product sourceProduct) {
        this.sourceProduct = sourceProduct;
        this.targetProduct = sourceProduct;
    }

    @Override
    public void initialize() throws OperatorException {
        targetProduct = sourceProduct;
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(PassThroughOp.class, "PassThrough");
        }
    }
}
