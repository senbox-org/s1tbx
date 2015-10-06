/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.core.gpf.pointop;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNodeFilter;

/**
 * A {@code ProductConfigurer} is used to configure a target product with respect to a current source product
 * which serves as a template.
 * Various {@code copy...()} methods may be used to copy parts of the source product into the target product.
 * <p>
 * This interface is not intended to be implemented by clients.
 *
 * @author Olaf Danne
 * @author Norman Fomferra
 * @author Marco Peters
 * @author Ralf Quast
 * @author Marco Zuehlke
 * @author Thomas Storm
 * @since BEAM 4.9
 */
public interface ProductConfigurer {

    Product getSourceProduct();

    void setSourceProduct(Product sourceProduct);

    Product getTargetProduct();

    void copyMetadata();

    void copyTimeCoding();

    void copyGeoCoding();

    void copyMasks();

    void copyTiePointGrids(String... gridName);

    void copyBands(String... bandName);

    void copyBands(ProductNodeFilter<Band> filter);

    void copyVectorData();

    Band addBand(String name, int dataType);

    Band addBand(String name, int dataType, double noDataValue);

    Band addBand(String name, String expression);

    Band addBand(String name, String expression, double noDataValue);
}
