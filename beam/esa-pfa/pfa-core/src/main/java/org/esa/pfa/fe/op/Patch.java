package org.esa.pfa.fe.op;

import org.esa.beam.framework.datamodel.Product;

import java.awt.*;

/**
 * A Patch contains the basic information for each segmented area
 * @author Norman Fomferra
 */
public final class Patch {

    private final int patchX;
    private final int patchY;
    private Product patchProduct = null;
    private final String patchName;

    public Patch(final int patchX, final int patchY, final Product patchProduct) {
        this(patchX, patchY);
        this.patchProduct = patchProduct;
    }

    public Patch(final int patchX, final int patchY) {
        this.patchX = patchX;
        this.patchY = patchY;
        this.patchName = String.format("x%02dy%02d", patchX, patchY);
    }

    public String getPatchName() {
        return patchName;
    }

    public int getPatchX() {
        return patchX;
    }

    public int getPatchY() {
        return patchY;
    }

    public Product getPatchProduct() {
        return patchProduct;
    }
}
