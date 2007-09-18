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
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.gpf.internal.GpfOpImage;

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
    //-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI
    GpfOpImage[] getOpImages();

    ClassInfo getClassInfo();

    public interface ClassInfo {
        boolean isBandMethodImplemented();

        boolean isAllBandsMethodImplemented();
    }
    //-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI

    /**
     * Gets the {@link Operator} this context belongs to.
     *
     * @return the operator
     */
    Operator getOperator();

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
     * @return the identifier
     */
    String getIdForSourceProduct(Product product);

    /**
     * Gets a {@link Raster} for a given band and rectangle.
     *
     * @param rasterDataNode the raster data node of a data product, e.g. a {@link org.esa.beam.framework.datamodel.Band} or {@link org.esa.beam.framework.datamodel.TiePointGrid}.
     * @param tileRectangle  the tile rectangle in pixel coordinates
     * @param pm             a monitor to observe progress
     * @return a tile
     * @see ProgressMonitor
     */
    Raster getRaster(RasterDataNode rasterDataNode, Rectangle tileRectangle, ProgressMonitor pm) throws
            OperatorException;

    /**
     * Gets a {@link Raster} for a given band and rectangle.
     *
     * @param rasterDataNode the raster data node of a data product, e.g. a {@link org.esa.beam.framework.datamodel.Band} or {@link org.esa.beam.framework.datamodel.TiePointGrid}.
     * @param tileRectangle  the tile rectangle in pixel coordinates
     * @param dataBuffer     a data buffer to be reused by the tile, its size must be equal to {@code tileRectangle.width * tileRectangle.height}.
     * @param pm             a monitor to observe progress
     * @return a tile which will reuse the given data buffer
     * @see ProgressMonitor
     */
    @Deprecated
    Raster getRaster(RasterDataNode rasterDataNode, Rectangle tileRectangle, ProductData dataBuffer,
                     ProgressMonitor pm) throws OperatorException;

    /**
     * Gets a logger for the operator.
     */
    Logger getLogger();
}