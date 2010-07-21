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

package org.esa.beam.visat.actions.session;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.product.ProductNodeView;

/**
 * A restored session comprising products and views.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public class RestoredSession {

    private final Product[] products;
    private final ProductNodeView[] views;
    private final Exception[] problems;

    public RestoredSession(Product[] products, ProductNodeView[] views, Exception[] problems) {
        this.products = products;
        this.views = views;
        this.problems = problems;
    }

    public Product[] getProducts() {
        return products.clone();
    }

    public ProductNodeView[] getViews() {
        return views.clone();
    }

    public Exception[] getProblems() {
        return problems.clone();
    }
}
