package org.esa.snap.core.gpf.common.resample;

import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.gpf.Operator;

/**
 * Created by obarrile on 12/04/2019.
 */
public abstract class DownsamplerSpi {

    private final String downsamplerAlias;
    private final Class<? extends Downsampling> downsamplingClass;

    protected DownsamplerSpi(Class<? extends Downsampling> downsamplingClass, String alias) {

        this.downsamplerAlias = alias;
        this.downsamplingClass = downsamplingClass;
    }

    public String getAlias() {
        return downsamplerAlias;
    }

    public Downsampling createDownsampling() {
        try {
            return downsamplingClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }
}
