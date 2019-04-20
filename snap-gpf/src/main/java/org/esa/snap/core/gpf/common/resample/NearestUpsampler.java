package org.esa.snap.core.gpf.common.resample;

import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.image.ImageManager;

/**
 * Created by obarrile on 11/04/2019.
 */
public class NearestUpsampler implements Upsampling {
    @Override
    public String getName() {
        return "Nearest";
    }

    @Override
    public boolean isCompatible(RasterDataNode rasterDataNode, int dataBufferType) {
        return true;
    }

    @Override
    public Interpolator createUpsampler(RasterDataNode rasterDataNode, int dataBufferType) {
        return InterpolatorFactory.createInterpolator(InterpolationType.Nearest,dataBufferType);
    }

    public static class Spi extends UpsamplerSpi {

        public Spi() {
            super(NearestUpsampler.class,"Nearest");
        }
    }
}
