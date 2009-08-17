package org.esa.beam.gpf.common.reproject.ui;

import org.esa.beam.framework.datamodel.Product;
import org.opengis.referencing.crs.ProjectedCRS;

/**
 * User: Marco
 * Date: 16.08.2009
 */
public class ReprojectionFormModel {
    private Product sourceProduct;
    private String interpolationName;
    private ProjectedCRS targetCrs;

    public Product getSourceProduct() {
        return sourceProduct;
    }

    public String getInterpolationName() {
        return interpolationName;
    }

    public ProjectedCRS getTargetCrs() {
        return targetCrs;
    }
}
