package org.esa.snap.core.gpf.common.resample;

/**
 * Created by obarrile on 11/04/2019.
 */
public abstract class UpsamplerSpi {

    private final String upsamplerAlias;
    private final Class<? extends Upsampling> upsamplingClass;

    protected UpsamplerSpi(Class<? extends Upsampling> upsamplingClass, String alias) {

        this.upsamplerAlias = alias;
        this.upsamplingClass = upsamplingClass;
    }

    public String getAlias() {
        return upsamplerAlias;
    }

    public Upsampling createUpsampling() {
        try {
            return upsamplingClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }
}
