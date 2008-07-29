package org.esa.beam.framework.ui.product;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.util.Guardian;

import java.awt.image.RenderedImage;
import java.io.IOException;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.Assert;

/**
 * TODO - Apidoc
 *
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
public abstract class ProductSceneImage {

    private String name;
    private ImageInfo imageInfo;
    private RasterDataNode[] rasters;
    private RenderedImage baseImage;

    public static boolean isInTiledImagingMode() {
        return Boolean.getBoolean("beam.imageTiling.enabled");
    }

    /**
     * Creates a color indexed product scene for the given product raster.
     *
     * @param raster the product raster, must not be null
     * @param pm     a monitor to inform the user about progress
     * @param inTiledImagingMode todo - remove me
     * @return a color indexed product scene image
     * @throws java.io.IOException if the image creation failed due to an I/O problem
     */
    public static ProductSceneImage create(RasterDataNode raster, ProgressMonitor pm, boolean inTiledImagingMode) throws IOException {
        Guardian.assertNotNull("raster", raster);
        if (inTiledImagingMode) {
            return new ProductSceneImage45(raster, pm);
        }  else {
            return new ProductSceneImage42(raster, pm);
        }

    }

    /**
     * Creates a new scene image for an existing view.
     *
     * @param raster The product raster.
     * @param view   An existing view.
     * @return A new scene image.
     * @throws java.io.IOException if the image creation failed due to an I/O problem.
     */
    public static ProductSceneImage create(RasterDataNode raster, ProductSceneView view) throws IOException {
        Assert.notNull(raster, "raster");
        Assert.notNull(view, "view");
        if (view instanceof ProductSceneView45) {
            return new ProductSceneImage45(raster, (ProductSceneView45) view);
        } else {
            return new ProductSceneImage42(raster, (ProductSceneView42) view);
        }
    }

    /**
     * Creates an RGB product scene for the given raster datasets.
     *
     * @param redRaster   the product raster used for the red color component, must not be null
     * @param greenRaster the product raster used for the green color component, must not be null
     * @param blueRaster  the product raster used for the blue color component, must not be null
     * @param pm          a monitor to inform the user about progress
     * @return an RGB product scene image
     * @throws java.io.IOException if the image creation failed due to an I/O problem
     */
    public static ProductSceneImage create(RasterDataNode redRaster,
                                           RasterDataNode greenRaster,
                                           RasterDataNode blueRaster,
                                           ProgressMonitor pm) throws IOException {
        return create(redRaster, greenRaster, blueRaster, pm, isInTiledImagingMode());
    }

    /**
     * Creates an RGB product scene for the given raster datasets.
     *
     * @param redRaster   the product raster used for the red color component, must not be null
     * @param greenRaster the product raster used for the green color component, must not be null
     * @param blueRaster  the product raster used for the blue color component, must not be null
     * @param pm          a monitor to inform the user about progress
     * @param inTiledImagingMode  todo - remove me
     * @return an RGB product scene image
     * @throws java.io.IOException if the image creation failed due to an I/O problem
     */
    public static ProductSceneImage create(RasterDataNode redRaster,
                                           RasterDataNode greenRaster,
                                           RasterDataNode blueRaster,
                                           ProgressMonitor pm, final boolean inTiledImagingMode) throws IOException {
        Assert.notNull(redRaster, "redRaster");
        Assert.notNull(greenRaster, "greenRaster");
        Assert.notNull(blueRaster, "blueRaster");
        Assert.notNull(pm, "pm");
        if (inTiledImagingMode) {
            return new ProductSceneImage45(new RasterDataNode[]{redRaster, greenRaster, blueRaster}, pm);
        }  else {
            return new ProductSceneImage42(new RasterDataNode[]{redRaster, greenRaster, blueRaster}, pm);
        }
    }

    protected ProductSceneImage(String name, RasterDataNode[] rasters, ImageInfo imageInfo) {
        this.name = name;
        this.rasters = rasters;
        this.imageInfo = imageInfo;
    }

    /**
     * @return the associated product.
     */
    protected Product getProduct() {
        return getRaster().getProduct();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ImageInfo getImageInfo() {
        return imageInfo;
    }

    public void setImageInfo(ImageInfo imageInfo) {
        this.imageInfo = imageInfo;
    }

    public RasterDataNode[] getRasters() {
        return rasters;
    }

    public void setRasters(RasterDataNode[] rasters) {
        this.rasters = rasters;
    }

    /**
     * Gets the product raster of a single banded view.
     *
     * @return the product raster, or <code>null</code> if this is a 3-banded RGB view
     */
    protected RasterDataNode getRaster() {
        return rasters[0];
    }

    RenderedImage getBaseImage() {
        return baseImage;
    }

    void setBaseImage(RenderedImage baseImage) {
        this.baseImage = baseImage;
    }

    protected abstract RenderedImage createBaseImage(ProgressMonitor pm) throws IOException;

}
