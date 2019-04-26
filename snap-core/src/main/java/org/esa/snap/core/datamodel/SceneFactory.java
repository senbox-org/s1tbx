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

import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.SystemUtils;

/**
 * This class is not public API yet.
 */
public final class SceneFactory {

    /**
     * Creates a scene wrapper around the given product node.
     *
     * @param node the product node
     *
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
        if (sourceGeoCoding == null) {
            targetScene.setGeoCoding(null);
            return true;
        }
        if (sourceGeoCoding instanceof AbstractGeoCoding) {
            AbstractGeoCoding abstractGeoCoding = (AbstractGeoCoding) sourceGeoCoding;
            return abstractGeoCoding.transferGeoCoding(sourceScene, targetScene, subsetDef);
        }
        return false;
    }
    
    private static boolean transferGeoCodingBandwise(final Scene sourceScene,
                                                     final Scene targetScene,
                                                     final ProductSubsetDef subsetDef) {
        boolean sceneTransferred = transferGeoCoding(sourceScene, targetScene, subsetDef);

        Product sourceProduct = sourceScene.getProduct();
        Product targetProduct = targetScene.getProduct();
        final String[] rasterNames = StringUtils.addArrays(sourceProduct.getBandNames(),
                                                           sourceProduct.getTiePointGridNames());
        int numTransferred = 0;
        for (String rasterName : rasterNames) {
            final RasterDataNode sourceRaster = sourceProduct.getRasterDataNode(rasterName);
            if (sourceRaster != null) {
                ProductSubsetDef auxiliarSubsetDef = null;
                if(subsetDef == null || subsetDef.getRegionMap() == null) {
                    auxiliarSubsetDef = subsetDef;
                } else {
                    auxiliarSubsetDef = new ProductSubsetDef();
                    auxiliarSubsetDef.setRegion(subsetDef.getRegionMap().get(sourceRaster.getName()));
                    auxiliarSubsetDef.setSubSampling(subsetDef.getSubSamplingX(),subsetDef.getSubSamplingY());
                }

                final Scene sourceRasterScene = SceneFactory.createScene(sourceRaster);
                final RasterDataNode targetRaster = targetProduct.getRasterDataNode(rasterName);
                if (targetRaster != null) {
                    final Scene targetRasterScene = SceneFactory.createScene(targetRaster);
                    if (transferGeoCoding(sourceRasterScene, targetRasterScene, auxiliarSubsetDef)) {
                        numTransferred++;
                    } else {
                        SystemUtils.LOG.warning(
                                "failed to transfer geo-coding of band '" + sourceRaster.getName() + "'");
                    }
                }
            }
        }

        return numTransferred > 0 || sceneTransferred;
    }

    private static class ProductScene implements Scene {

        private final Product product;

        public ProductScene(final Product product) {
            Guardian.assertNotNull("product", product);
            this.product = product;
        }

        public void setGeoCoding(final GeoCoding geoCoding) {
            product.setSceneGeoCoding(geoCoding);
        }

        public GeoCoding getGeoCoding() {
            return product.getSceneGeoCoding();
        }

        public boolean transferGeoCodingTo(final Scene targetScene, final ProductSubsetDef subsetDef) {
            if (product.isUsingSingleGeoCoding()) {
                return transferGeoCoding(this, targetScene, subsetDef);
            } else {
                return transferGeoCodingBandwise(this, targetScene, subsetDef);
            }
        }

        public int getRasterWidth() {
            return product.getSceneRasterWidth();
        }

        public int getRasterHeight() {
            return product.getSceneRasterHeight();
        }

        public Product getProduct() {
            return product;
        }
    }

    private static class RasterDataNodeScene implements Scene {

        RasterDataNode raster;

        public RasterDataNodeScene(final RasterDataNode raster) {
            Guardian.assertNotNull("raster", raster);
            this.raster = raster;
        }

        public GeoCoding getGeoCoding() {
            return raster.getGeoCoding();
        }

        public void setGeoCoding(final GeoCoding geoCoding) {
            raster.setGeoCoding(geoCoding);
        }

        public boolean transferGeoCodingTo(final Scene destScene, final ProductSubsetDef subsetDef) {
            return transferGeoCoding(this, destScene, subsetDef);
        }

        public int getRasterWidth() {
            return raster.getRasterWidth();
        }

        public int getRasterHeight() {
            return raster.getRasterHeight();
        }

        public Product getProduct() {
            Product product = raster.getProduct();
            if (product == null) {
                // product can be null if e.g. raster is a mask and is contained in a group
                ProductNode owner = raster.getOwner(); // the group which contains the mask
                if (owner != null) {
                    product = owner.getProduct();
                }
            }

            return product;
        }
    }
}

