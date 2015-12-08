/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.core.datamodel;

import javax.media.jai.KernelJAI;
import javax.media.jai.operator.ConvolveDescriptor;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;

/**
 * A band that obtains its input data from an underlying source band and filters
 * the raster data using a {@link Kernel}.
 * <p><i>Note that this class is not yet public API. Interface may chhange in future releases.</i>
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class ConvolutionFilterBand extends FilterBand {

    private final Kernel kernel;
    private final int iterationCount;

    public ConvolutionFilterBand(String name, RasterDataNode source, Kernel kernel, int iterationCount) {
        super(name,
              source.getGeophysicalDataType() == ProductData.TYPE_FLOAT64 ? ProductData.TYPE_FLOAT64 : ProductData.TYPE_FLOAT32,
              source.getRasterWidth(),
              source.getRasterHeight(),
              source);
        this.kernel = kernel;
        this.iterationCount = iterationCount;
    }

    public Kernel getKernel() {
        return kernel;
    }

    @Override
    protected RenderedImage createSourceLevelImage(RenderedImage sourceImage, int level, RenderingHints rh) {
        KernelJAI jaiKernel = createJaiKernel();
        RenderedImage targetImage = sourceImage;
        for (int i = 0; i < iterationCount; i++) {
            targetImage = ConvolveDescriptor.create(targetImage, jaiKernel, rh);
        }
        return targetImage;
    }

    private KernelJAI createJaiKernel() {
        final double[] data = this.kernel.getKernelData(null);
        final float[] scaledData = new float[data.length];
        final double factor = this.kernel.getFactor();
        for (int i = 0; i < data.length; i++) {
            scaledData[i] = (float) (data[i] * factor);
        }
        return new KernelJAI(this.kernel.getWidth(), this.kernel.getHeight(),
                             this.kernel.getXOrigin(), this.kernel.getYOrigin(),
                             scaledData);
    }
}
