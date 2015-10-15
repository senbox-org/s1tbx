package org.esa.snap.binning.operator;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductFilter;

abstract class BinningProductFilter implements ProductFilter {

    private String reason;
    private BinningProductFilter parent;

    String getReason() {
        if (parent != null && parent.getReason() != null) {
            return parent.getReason();
        } else {
            return reason;
        }
    }

    @Override
    public boolean accept(Product product) {
        setReason(null);
        if (parent != null && !parent.accept(product)) {
            return false;
        }
        return acceptForBinning(product);
    }

    protected abstract boolean acceptForBinning(Product product);

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setParent(BinningProductFilter parent) {
        this.parent = parent;
    }

    public BinningProductFilter getParent() {
        return parent;
    }
}
