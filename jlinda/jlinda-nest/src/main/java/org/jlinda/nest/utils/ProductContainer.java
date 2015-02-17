package org.jlinda.nest.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * User: pmar@ppolabs.com
 * Date: 6/20/11
 * Time: 11:16 PM
 */
public class ProductContainer {

    public String name;
    public CplxContainer sourceMaster;
    public CplxContainer sourceSlave;
    public String targetBandName_I;
    public String targetBandName_Q;
    private final Map<String,String> bandNameMap = new HashMap<>(3);

    public boolean subProductsFlag;
    public SubProduct masterSubProduct;
    public SubProduct slaveSubProduct;

    // TODO: put baselines in the structure
    // Baseline baseline = new Baseline();

    public ProductContainer(String name, CplxContainer sourceMaster, CplxContainer sourceSlave) {
        this.name = name;
        this.sourceMaster = sourceMaster;
        this.sourceSlave = sourceSlave;
        this.subProductsFlag = false;
    }

    public ProductContainer(String name, CplxContainer sourceMaster, CplxContainer sourceSlave, boolean subProductsFlag) {

        this.name = name;
        this.sourceMaster = sourceMaster;
        this.sourceSlave = sourceSlave;

        this.subProductsFlag = subProductsFlag;
        if (subProductsFlag) {
            this.masterSubProduct = new SubProduct();
            this.slaveSubProduct = new SubProduct();
        }
    }

    public void addBand(final String key, final String bandName) {
        bandNameMap.put(key, bandName);
    }

    public String getBandName(final String key) {
        return bandNameMap.get(key);
    }

    // helper sub-class
    public class SubProduct {
        public String name;
        public String targetBandName_I;
        public String targetBandName_Q;
    }

}
