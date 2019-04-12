package org.esa.snap.core.gpf.common.resample;

/**
 * Created by obarrile on 11/04/2019.
 */
public abstract class UpsamplerSpi {

    private final String upsamplerAlias;

    protected UpsamplerSpi(String  upsamplerAlias) {
        this.upsamplerAlias = upsamplerAlias;
    }

    public String getAlias() {
        return upsamplerAlias;
    }
}
