package org.jlinda.core.filtering;

import org.jblas.ComplexDoubleMatrix;
import org.jlinda.core.SLCImage;

/**
 * User: pmar@ppolabs.com
 * Date: 6/3/11
 * Time: 12:36 PM
 */
public class ProductDataFilter extends SlcDataFilter {

    ComplexDoubleMatrix data1;
    SLCImage metadata1;

    public SLCImage getMetadata1() {
        return metadata1;
    }

    public void setMetadata1(SLCImage metadata1) {
        this.metadata1 = metadata1;
    }

    public ComplexDoubleMatrix getData1() {
        return data1;
    }

    public void setData1(ComplexDoubleMatrix data1) {
        this.data1 = data1;
    }

}
