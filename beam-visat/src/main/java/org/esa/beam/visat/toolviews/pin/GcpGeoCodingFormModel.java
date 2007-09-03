package org.esa.beam.visat.toolviews.pin;

import org.esa.beam.framework.datamodel.Product;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision:$ $Date:$
 */
public class GcpGeoCodingFormModel {

    public enum TransformationType {
        PolynomialFirstOrder {
            @Override
            public String toString() {
                return "1st Order Polynomial";
            }
        },
        PolynomialSecondOrder {
            @Override
            public String toString() {
                return "2nd Order Polynomial";
            }
        },
        PolynomialThirdOrder {
            @Override
            public String toString() {
                return "3rd Order Polynomial";
            }
        },
        RationalFunction {
            @Override
            public String toString() {
                return "Rational Function";
            }
        },
    }

    private Product product;
    private TransformationType transformationType;
    private double geoCodingRmse;

    public GcpGeoCodingFormModel() {
        geoCodingRmse = Double.NaN;
        transformationType = TransformationType.PolynomialFirstOrder;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public TransformationType getTransformationType() {
        return transformationType;
    }

    public void setTransformationType(TransformationType transformationType) {
        this.transformationType = transformationType;
    }

    public double getGeoCodingRmse() {
        return geoCodingRmse;
    }

    public void setGeoCodingRmse(double geoCodingRmse) {
        this.geoCodingRmse = geoCodingRmse;
    }

}
