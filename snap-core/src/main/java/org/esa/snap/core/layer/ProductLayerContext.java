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

package org.esa.snap.core.layer;

import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;

/**
 * A {@link LayerContext} for layers requiring access to a certain {@link Product} or a
 * certain {@link ProductNode}.
 * <p>
 * For {@link LayerType}s requiring this context, {@link LayerType#isValidFor(LayerContext)}
 * must return {@code true} if, and only if, the argument is an instance of this class.
 *
 * @author Ralf Quast
 * @version $Revision $ $Date $
 * @since BEAM 4.7
 */
public interface ProductLayerContext extends LayerContext {

    /**
     * Returns the product provided by this context.
     *
     * @return the product provided by this context.
     */
    Product getProduct();

    ProductNode getProductNode();
}
