package org.esa.snap.core.gpf.common.resample;

import org.esa.snap.core.datamodel.RasterDataNode;

/**
 * Created by obarrile on 11/04/2019.
 */
public class NearestUpsampler implements Upsampling {
    @Override
    public String getName() {
        return null;
    }

    @Override
    public boolean isCompatible(RasterDataNode rasterDataNode) {
        return false;
    }

    @Override
    public Interpolator createUpsampler(RasterDataNode rasterDataNode) {
        return null;
    }

    public static class Spi extends UpsamplerSpi {

        public Spi() {
            super("Nearest");
        }
    }
}
