package org.esa.beam.framework.ui.product;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.PropertyMap;

import java.io.IOException;

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
    private PropertyMap configuration;

    /**
     * Creates a color indexed product scene for the given product raster.
     *
     * @param raster        the product raster, must not be null
     * @param configuration a configuration
     * @param pm            a monitor to inform the user about progress @return a color indexed product scene image
     * @throws java.io.IOException if the image creation failed due to an I/O problem
     */
    public static ProductSceneImage create(RasterDataNode raster, PropertyMap configuration, ProgressMonitor pm) throws IOException {
        Guardian.assertNotNull("raster", raster);
        Assert.notNull(configuration, "configuration");
        return new ProductSceneImage45(raster, configuration, pm);
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
        return new ProductSceneImage45(raster, (ProductSceneView45) view);
    }

    /**
     * Creates an RGB product scene for the given raster datasets.
     *
     * @param name          the name of the scene view
     * @param redRaster     the product raster used for the red color component, must not be null
     * @param greenRaster   the product raster used for the green color component, must not be null
     * @param blueRaster    the product raster used for the blue color component, must not be null
     * @param configuration a configuration
     * @param pm            a monitor to inform the user about progress @return an RGB product scene image @throws java.io.IOException if the image creation failed due to an I/O problem
     * @return the scene view created.
     * @throws IOException when an I/O error occurred.
     */
    public static ProductSceneImage create(String name, RasterDataNode redRaster,
                                           RasterDataNode greenRaster,
                                           RasterDataNode blueRaster,
                                           PropertyMap configuration,
                                           ProgressMonitor pm) throws IOException {
        Assert.notNull(name, "name");
        Assert.notNull(redRaster, "redRaster");
        Assert.notNull(greenRaster, "greenRaster");
        Assert.notNull(blueRaster, "blueRaster");
        Assert.notNull(configuration, "configuration");
        Assert.notNull(pm, "pm");

        return new ProductSceneImage45(name, new RasterDataNode[]{redRaster, greenRaster, blueRaster}, configuration, pm);
    }

    protected ProductSceneImage(String name, RasterDataNode[] rasters, ImageInfo imageInfo, PropertyMap configuration) {
        this.name = name;
        this.rasters = rasters;
        this.imageInfo = imageInfo;
        this.configuration = configuration;
    }

    public PropertyMap getConfiguration() {
        return configuration;
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


}
