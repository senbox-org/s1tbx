/*
 * $Id$
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
package org.esa.beam.framework.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;

import javax.media.jai.PlanarImage;
import java.awt.Rectangle;
import java.util.logging.Logger;

/**
 * The context in which operators are executed.
 *
 * @author Norman Fomferra
 * @author Marco Zühlke
 * @version $Revision$ $Date$
 * @since 4.1
 */
public interface OperatorContext {

    /**
     * Gets the {@link Operator} this context belongs to.
     *
     * @return the operator
     */
    Operator getOperator();
    
    /**
     * Gets the SPI (Service Provider Interface) of the {@link Operator operator}.
     *
     * @return the SPI
     */
    OperatorSpi getOperatorSpi();

    /**
     * Gets the target product for the operator.
     *
     * @return the target product
     */
    Product getTargetProduct();

    /**
     * Gets the source products in the order they have been declared.
     *
     * @return the array source products
     */
    Product[] getSourceProducts();

    /**
     * Gets the source product using the specified id.
     *
     * @param id the identifier
     * @return the source product, or {@code null} if not found
     */
    Product getSourceProduct(String id);

    /**
     * Gets the identifier for the given source product.
     *
     * @param product the product
     * @return the identifier, or {@code null} if not found
     */
    String getSourceProductId(Product product);

    /**
     * Gets an array of JAI {@link javax.media.jai.PlanarImage PlanarImage}s associated with each of the bands in the target product.
     *
     * @return The array of target images.
     * @see #getTargetProduct()
     */
    PlanarImage[] getTargetImages();

    /**
     * Gets a source {@link Tile tile} for a given raster data node and tile rectangle.
     * Target tiles are passed directly to the operator's {@link org.esa.beam.framework.gpf.Operator#computeTile(org.esa.beam.framework.datamodel.Band,Tile,com.bc.ceres.core.ProgressMonitor) computeBand}
     * and/or {@link org.esa.beam.framework.gpf.Operator#computeTileStack(java.util.Map,java.awt.Rectangle,com.bc.ceres.core.ProgressMonitor) computeAllBands}
     * methods.
     *
     * @param rasterDataNode the raster data node of a data product, e.g. a {@link org.esa.beam.framework.datamodel.Band} or {@link org.esa.beam.framework.datamodel.TiePointGrid}.
     * @param tileRectangle  the tile rectangle in pixel coordinates
     * @return a tile
     * @throws OperatorException if the operation fails
     */
    Tile getSourceTile(RasterDataNode rasterDataNode, Rectangle tileRectangle) throws
            OperatorException;

    /**
     * Gets a logger for the operator.
     *
     * @return a logger
     */
    Logger getLogger();
}