package com.bc.layer;

import java.awt.AlphaComposite;

public enum AlphaCompositeMode {
    CLEAR(AlphaComposite.CLEAR),
    SRC(AlphaComposite.SRC),
    DST(AlphaComposite.DST),
    SRC_OVER(AlphaComposite.SRC_OVER),
    DST_OVER(AlphaComposite.DST_OVER),
    SRC_IN(AlphaComposite.SRC_IN),
    DST_IN(AlphaComposite.DST_IN),
    SRC_OUT(AlphaComposite.SRC_OUT),
    DST_OUT(AlphaComposite.DST_OUT),
    SRC_ATOP(AlphaComposite.SRC_ATOP),
    DST_ATOP(AlphaComposite.DST_ATOP),
    XOR(AlphaComposite.XOR);

    final int value;

    private AlphaCompositeMode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

}
