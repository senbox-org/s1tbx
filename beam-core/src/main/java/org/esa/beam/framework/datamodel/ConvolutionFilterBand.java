package org.esa.beam.framework.datamodel;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import org.esa.beam.jai.ImageManager;

import javax.media.jai.BorderExtender;
import javax.media.jai.BorderExtenderCopy;
import javax.media.jai.JAI;
import javax.media.jai.KernelJAI;
import javax.media.jai.operator.ConvolveDescriptor;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
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

    private Kernel kernel;
    private KernelJAI jaiKernel;

    public ConvolutionFilterBand(String name, RasterDataNode source, Kernel kernel) {
        super(name,
              source.getGeophysicalDataType(),
              source.getSceneRasterWidth(),
              source.getSceneRasterHeight(),
              source);
        this.kernel = kernel;
        final double[] data = this.kernel.getKernelData(null);
        final float[] scaledData = new float[data.length];
        final double factor = this.kernel.getFactor();
        for (int i = 0; i < data.length; i++) {
            scaledData[i] = (float) (data[i] * factor);
        }
        jaiKernel = new KernelJAI(this.kernel.getWidth(), this.kernel.getHeight(),
                                  this.kernel.getXOrigin(), this.kernel.getYOrigin(),
                                  scaledData);
        setOwner(source.getProduct());
    }

    @Override
    protected RenderedImage createSourceImage() {
        final MultiLevelModel model = ImageManager.getInstance().getMultiLevelModel(this);
        final AbstractMultiLevelSource multiLevelSource = new AbstractMultiLevelSource(model) {
            @Override
            protected RenderedImage createImage(int level) {
                RenderingHints rh = new RenderingHints(JAI.KEY_BORDER_EXTENDER, BorderExtender.createInstance(
                        BorderExtenderCopy.BORDER_COPY));
                final ImageManager imageManager = ImageManager.getInstance();
                final RenderedImage geophysicalSourceImage = imageManager.getGeophysicalImage(getSource(), level);
                return ConvolveDescriptor.create(geophysicalSourceImage, jaiKernel, rh);
            }
        };
        return new DefaultMultiLevelImage(multiLevelSource);
    }

    public Kernel getKernel() {
        return kernel;
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
