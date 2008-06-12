package org.esa.beam.framework.datamodel;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;

import java.io.IOException;

/**
 * A band that obtains its input data from an underlying source band and filters
 * the raster data using a {@link Kernel}.
 * <p/>
 * <p><i>Note that this class is not yet public API. Interface may chhange in future releases.</i></p>
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class ConvolutionFilterBand extends FilterBand {

    private Kernel _kernel;

    public ConvolutionFilterBand(String name, RasterDataNode source, Kernel kernel) {
        super(name,
              ProductData.TYPE_FLOAT32,
              source.getSceneRasterWidth(),
              source.getSceneRasterHeight(),
              source);
        _kernel = kernel;
        setOwner(source.getProduct());
        setGeophysicalNoDataValue(-9999);
        setNoDataValueUsed(true);
    }

    public Kernel getKernel() {
        return _kernel;
    }

    // TODO - (nf) ConvolutionFilterBand-s are currently always read entirely. However, this method wont work anymore for
    // spatial subsets with width != sceneRasterWidth or height != sceneRasterHeight


    /**
     * Reads raster data from this dataset into the user-supplied raster data buffer.
     * <p/>
     * <p>This method always directly (re-)reads this band's data from its associated data source into the given data
     * buffer.
     *
     * @param offsetX    the X-offset in the raster co-ordinates where reading starts
     * @param offsetY    the Y-offset in the raster co-ordinates where reading starts
     * @param width      the width of the raster data buffer
     * @param height     the height of the raster data buffer
     * @param rasterData a raster data buffer receiving the pixels to be read
     * @param pm         a monitor to inform the user about progress
     *
     * @throws java.io.IOException      if an I/O error occurs
     * @throws IllegalArgumentException if the raster is null
     * @throws IllegalStateException    if this product raster was not added to a product so far, or if the product to
     *                                  which this product raster belongs to, has no associated product reader
     * @see org.esa.beam.framework.dataio.ProductReader#readBandRasterData(Band, int, int, int, int, ProductData, com.bc.ceres.core.ProgressMonitor) 
     */
    @Override
    public void readRasterData(int offsetX, int offsetY, int width, int height, ProductData rasterData,
                               ProgressMonitor pm) throws IOException {
        final RasterDataNode source = getSource();
        final ProductData sourceData = ProductData.createInstance(ProductData.TYPE_FLOAT64, width * height);
        pm.beginTask("Reading band data", 2);
        try {
            source.readPixels(offsetX, offsetY, width, height, (double[]) sourceData.getElems(),
                              SubProgressMonitor.create(pm, 1));

            final int kx0 = _kernel.getXOrigin();
            final int ky0 = _kernel.getYOrigin();
            final int kw = _kernel.getWidth();
            final int kh = _kernel.getHeight();
            final double factor = _kernel.getFactor();
            final double[] kernelData = _kernel.getKernelData(null);
            double sourceValue;
            double targetValue;
            int numSourceValues;

            ProgressMonitor subPm = SubProgressMonitor.create(pm, 1);
            subPm.beginTask("Applying filter...", height);
            try {
                for (int y = 0; y < height; y++) {
                    if (subPm.isCanceled()) {
                        return;
                    }
                    for (int x = 0; x < width; x++) {
                        targetValue = 0;
                        numSourceValues = 0;
                        for (int ky = 0; ky < kh; ky++) {
                            int sourceY = y + (ky - ky0);
                            if (sourceY >= 0 && sourceY < height) {
                                for (int kx = 0; kx < kw; kx++) {
                                    int sourceX = x + (kx - kx0);
                                    if (sourceX >= 0 && sourceX < width) {
                                        if (source.isPixelValid(sourceX, sourceY))
                                        { // todo - check: this is the C2R Doerffer patch
                                            sourceValue = source.scale(
                                                    sourceData.getElemDoubleAt(sourceY * width + sourceX));
                                            targetValue += kernelData[ky * kw + kx] * sourceValue;
                                            numSourceValues++;
                                        }
                                    }
                                }
                            }
                        }
                        if (numSourceValues > 0) {
                            targetValue *= factor;
                        } else {
                            targetValue = getGeophysicalNoDataValue();
                        }
                        rasterData.setElemDoubleAt(y * width + x, targetValue);
                    }
                    subPm.worked(1);
                }
            } finally {
                subPm.done();
            }
        } finally {
            pm.done();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeRasterData(int offsetX, int offsetY, int width, int height, ProductData rasterData,
                                ProgressMonitor pm) throws IOException {
        throw new IllegalStateException("write not supported for filtered band");
    }
}
