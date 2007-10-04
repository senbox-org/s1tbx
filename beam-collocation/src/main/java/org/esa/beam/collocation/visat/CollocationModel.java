package org.esa.beam.collocation.visat;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.dataop.resamp.Resampling;

/**
 * Data model for collocation dialog.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class CollocationModel {

    private Product masterProduct;
    private Product slaveProduct;

    private Resampling resampling;

    public Product getMasterProduct() {
        return masterProduct;
    }

    public void setMasterProduct(Product masterProduct) {
        this.masterProduct = masterProduct;
    }

    public Product getSlaveProduct() {
        return slaveProduct;
    }

    public void setSlaveProduct(Product slaveProduct) {
        this.slaveProduct = slaveProduct;
    }

    public Resampling getResampling() {
        return resampling;
    }

    public void setResampling(Resampling resampling) {
        this.resampling = resampling;
    }
}
