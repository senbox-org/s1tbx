package org.esa.beam.framework.ui.product;

import com.bc.ceres.core.ProgressMonitor;

import java.awt.image.RenderedImage;
import java.io.IOException;

import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.ImageInfo;

/**
 * TODO - Apidoc
 *
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
public class ProductSceneImage45 extends ProductSceneImage {
    public ProductSceneImage45(RasterDataNode raster, ProductSceneView45 view) throws IOException {
        super(raster.getDisplayName(), new RasterDataNode[]{raster}, view.getImageInfo());
    }

    public ProductSceneImage45(RasterDataNode raster, ProgressMonitor pm) throws IOException {
        this(raster.getDisplayName(), new RasterDataNode[]{raster}, raster.getImageInfo(), pm);
    }

    public ProductSceneImage45(RasterDataNode[] rasterDataNodes, ProgressMonitor pm) throws IOException {
        this("RGB", rasterDataNodes, null, pm);
    }

    private ProductSceneImage45(String name, RasterDataNode[] rasterDataNodes, ImageInfo imageInfo, ProgressMonitor pm) throws IOException {
        super(name, rasterDataNodes, imageInfo);
        setBaseImage(createBaseImage(pm));
    }

    protected RenderedImage createBaseImage(ProgressMonitor pm) throws IOException {
        return null;  // todo - implement me!
    }
}
