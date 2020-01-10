package org.esa.snap.core.gpf.internal;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.SystemUtils;

import javax.media.jai.JAI;
import javax.media.jai.OpImage;
import javax.media.jai.TileCache;
import java.awt.image.RenderedImage;

/**
 * Experimental: An Operator which uses a dedicated tile cache for the source product.
 * The property <code>snap.gpf.disableTileCache</code> can be set to <code>true</code> in order to disable the cache for GPF by default.
 * Within a graph, this operator can be used to cache the results of specific operators.
 *
 * @author Marco Peters
 * @since SNAP 8.0.0
 */
@OperatorMetadata(alias = "TileCache",
        category = "Tools",
        authors = "Marco Peters",
        copyright = "Brockmann Consult GmbH",
        version = "0.1",
        internal = true,
        description = "Operator which provides a dedicated cache for its source product.")
public class TileCacheOp extends Operator {

    private static final long MEGABYTES = 1024 * 1024;
    @SourceProduct
    Product source;

    @TargetProduct
    Product target;

    @Parameter(defaultValue = "1000", unit = "MB", description = "The cache size in MB. Set it to 0 to use default tile cache.", label = "Cache size")
    int cacheSize;

    private TileCache localCache;

    @Override
    public void initialize() throws OperatorException {
        SystemUtils.LOG.warning("You are using TileCache operator. Be aware that it is an experimental implementation.");
        if (cacheSize > 0) {
            localCache = JAI.createTileCache(cacheSize * MEGABYTES);
        } else {
            localCache = JAI.getDefaultInstance().getTileCache();
        }
        Product sourceProduct = getSourceProduct();
        Band[] bands = sourceProduct.getBands();
        for (Band band : bands) {
            RenderedImage image = band.getSourceImage().getImage(0);
            if (image instanceof OpImage) {
                OpImage opImage = (OpImage) image;
                opImage.setTileCache(localCache);
                SystemUtils.LOG.info(String.format("Local tile cache %d assigned to %s", cacheSize, image));
            }
        }

        target = source;
    }

    @Override
    public void dispose() {
        if (cacheSize > 0) {
            localCache.flush();
        }
        localCache = null;
        super.dispose();
    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(TileCacheOp.class);
        }
    }
}
