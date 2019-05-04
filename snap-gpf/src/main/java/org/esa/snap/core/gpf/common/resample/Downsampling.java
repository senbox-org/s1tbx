package org.esa.snap.core.gpf.common.resample;

import org.esa.snap.core.datamodel.RasterDataNode;

/**
 * Created by obarrile on 12/04/2019.
 */
public interface Downsampling extends ResamplingMethod {
    Aggregator createDownsampler(RasterDataNode rasterDataNode, int dataBufferType);
}
