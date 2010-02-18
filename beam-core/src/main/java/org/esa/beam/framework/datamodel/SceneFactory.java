package org.esa.beam.framework.datamodel;

import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.logging.BeamLogManager;

/**
 * This class is not public API yet.
 */
public final class SceneFactory {

    /**
     * Creates a scene wrapper around the given product node.
     * @param node the product node
     * @return a scene instance or null if it could not be created
     */
    public static Scene createScene(final ProductNode node) {
        if (node instanceof Product) {
            final Product product = (Product) node;
            return new ProductScene(product);
        } else if (node instanceof RasterDataNode) {
            final RasterDataNode raster = (RasterDataNode) node;
            return new RasterDataNodeScene(raster);
        }
        return null;
    }

    private static boolean transferGeoCoding(final Scene sourceScene,
                                             final Scene targetScene,
                                             final ProductSubsetDef subsetDef) {
        final GeoCoding sourceGeoCoding = sourceScene.getGeoCoding();
        if (sourceGeoCoding instanceof AbstractGeoCoding) {
            AbstractGeoCoding abstractGeoCoding = (AbstractGeoCoding) sourceGeoCoding;
            return abstractGeoCoding.transferGeoCoding(sourceScene, targetScene, subsetDef);
        }
        return false;
    }

    private static boolean transferGeoCodingBandwise(final Scene sourceScene,
                                                     final Scene targetScene,
                                                     final ProductSubsetDef subsetDef) {
        final String[] rasterNames = StringUtils.addArrays(sourceScene.getProduct().getBandNames(),
                                                           sourceScene.getProduct().getTiePointGridNames());
        int numTransferred = 0;
        for (String rasterName : rasterNames) {
            final RasterDataNode sourceRaster = sourceScene.getProduct().getRasterDataNode(rasterName);
            if (sourceRaster != null) {
                final Scene sourceRasterScene = SceneFactory.createScene(sourceRaster);
                final RasterDataNode targetRaster = targetScene.getProduct().getRasterDataNode(rasterName);
                if (targetRaster != null) {
                    final Scene targetRasterScene = SceneFactory.createScene(targetRaster);
                    if (transferGeoCoding(sourceRasterScene, targetRasterScene, subsetDef)) {
                        numTransferred++;
                    } else {
                        BeamLogManager.getSystemLogger().warning(
                                "failed to transfer geo-coding of band '" + sourceRaster.getName() + "'");
                    }
                }
            }
        }
        return numTransferred > 0;
    }

    private static class ProductScene implements Scene {

        private final Product _product;

        public ProductScene(final Product product) {
            Guardian.assertNotNull("product", product);
            _product = product;
        }

        public void setGeoCoding(final GeoCoding geoCoding) {
            _product.setGeoCoding(geoCoding);
        }

        public GeoCoding getGeoCoding() {
            return _product.getGeoCoding();
        }

        public boolean transferGeoCodingTo(final Scene targetScene, final ProductSubsetDef subsetDef) {
            if (_product.isUsingSingleGeoCoding()) {
                return transferGeoCoding(this, targetScene, subsetDef);
            } else {
                return transferGeoCodingBandwise(this, targetScene, subsetDef);
            }
        }

        public int getRasterWidth() {
            return _product.getSceneRasterWidth();
        }

        public int getRasterHeight() {
            return _product.getSceneRasterHeight();
        }

        public Product getProduct() {
            return _product;
        }
    }

    private static class RasterDataNodeScene implements Scene {

        RasterDataNode _raster;

        public RasterDataNodeScene(final RasterDataNode raster) {
            Guardian.assertNotNull("raster", raster);
            _raster = raster;
        }

        public GeoCoding getGeoCoding() {
            return _raster.getGeoCoding();
        }

        public void setGeoCoding(final GeoCoding geoCoding) {
            _raster.setGeoCoding(geoCoding);
        }

        public boolean transferGeoCodingTo(final Scene destScene, final ProductSubsetDef subsetDef) {
            return transferGeoCoding(this, destScene, subsetDef);
        }

        public int getRasterWidth() {
            return _raster.getSceneRasterWidth();
        }

        public int getRasterHeight() {
            return _raster.getSceneRasterHeight();
        }

        public Product getProduct() {
            return _raster.getProduct();
        }
    }
}

