package org.esa.snap.core.gpf.common.resample;

import org.esa.snap.core.datamodel.RasterDataNode;

/**
 * Created by obarrile on 11/04/2019.
 */
public interface Upsampling extends ResamplingMethod {
    Interpolator createUpsampler(RasterDataNode rasterDataNode, int dataBufferType);
}
