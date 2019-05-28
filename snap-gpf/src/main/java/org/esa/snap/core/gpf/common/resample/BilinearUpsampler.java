package org.esa.snap.core.gpf.common.resample;

import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.image.ImageManager;

/**
 * Created by obarrile on 19/04/2019.
 */
public class BilinearUpsampler implements Upsampling {
    @Override
    public String getName() {
        return "Bilinear";
    }

    @Override
    public boolean isCompatible(RasterDataNode rasterDataNode, int dataBufferType) {
        return true;
    }

    @Override
    public Interpolator createUpsampler(RasterDataNode rasterDataNode, int dataBufferType) {
        return InterpolatorFactory.createInterpolator(InterpolationType.Bilinear,dataBufferType);
    }

    public static class Spi extends UpsamplerSpi {

        public Spi() {
            super(BilinearUpsampler.class,"Bilinear");
        }
    }
}
