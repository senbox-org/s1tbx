/*
 * $Id: $
 *
 * Copyright (C) 2007 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.gpf.operators.meris;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.util.ProductUtils;


/**
 * Created by marcoz.
 *
 * @author marcoz
 * @version $Revision$ $Date$
 */
public abstract class MerisBasisOp extends Operator {

    @Parameter(description="If set to 'false' only the lat and lon TiePoins will be copied to the target product", defaultValue="false")
    private boolean copyAllTiePoints = false;

    /**
     * creates a new product with the same size
     *
     * @param sourceProduct
     * @param name
     * @param type
     * @return targetProduct
     */
    public Product createCompatibleProduct(Product sourceProduct, String name, String type) {
        final int sceneWidth = sourceProduct.getSceneRasterWidth();
        final int sceneHeight = sourceProduct.getSceneRasterHeight();

        Product targetProduct = new Product(name, type, sceneWidth, sceneHeight);
        copyProductTrunk(sourceProduct, targetProduct);
        return targetProduct;
    }

    /**
     * Copies basic information for a MERIS product to the target product
     *
     * @param sourceProduct
     * @param targetProduct
     */
    public void copyProductTrunk(Product sourceProduct,
                                 Product targetProduct) {
        copyTiePoints(sourceProduct, targetProduct);
        copyBaseGeoInfo(sourceProduct, targetProduct);
    }

    /**
     * Copies the tie point data.
     *
     * @param sourceProduct
     * @param targetProduct
     */
    private void copyTiePoints(Product sourceProduct,
                               Product targetProduct) {
        if (copyAllTiePoints) {
            // copy all tie point grids to output product
            ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);
        } else {
            for (int i = 0; i < sourceProduct.getNumTiePointGrids(); i++) {
                TiePointGrid srcTPG = sourceProduct.getTiePointGridAt(i);
                if (srcTPG.getName().equals("latitude") || srcTPG.getName().equals("longitude")) {
                    targetProduct.addTiePointGrid(srcTPG.cloneTiePointGrid());
                }
            }
        }
    }

    /**
     * Copies geocoding and the start and stop time.
     *
     * @param sourceProduct
     * @param targetProduct
     */
    private void copyBaseGeoInfo(Product sourceProduct,
                                 Product targetProduct) {
        // copy geo-coding to the output product
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());
    }

}
