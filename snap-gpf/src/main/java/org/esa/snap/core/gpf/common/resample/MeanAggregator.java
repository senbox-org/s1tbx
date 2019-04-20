package org.esa.snap.core.gpf.common.resample;

import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.image.ImageManager;

/**
 * Created by obarrile on 12/04/2019.
 */
public class MeanAggregator implements Downsampling {

    @Override
    public String getName() {
        return "Mean";
    }

    @Override
    public boolean isCompatible(RasterDataNode rasterDataNode, int dataBufferType) {
        return true;
    }

    @Override
    public Aggregator createDownsampler(RasterDataNode rasterDataNode, int dataBufferType) {
        return AggregatorFactory.createAggregator(AggregationType.Mean,dataBufferType);
    }

    public static class Spi extends DownsamplerSpi {
        public Spi() {
            super(MeanAggregator.class,"Mean");
        }
    }
}
