/*
 * $Id: VisatContext.java,v 1.1.1.1 2006/09/11 08:16:54 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
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
package org.esa.beam.visat;

import org.esa.beam.framework.datamodel.Product;

/**
 * The <code>VisatContext</code> class represents the current context of a running VISAT application instance.
 */
public interface VisatContext {

    /**
     * Gets the one and only active product, if any.
     *
     * @return the currently active product, if there is no active product, <code>null</code> is returned
     */
    Product getActiveProduct();

    /**
     * Gets the currently selected products.
     *
     * @return the array of currently selected product, if there are no selected products, <code>null</code> is
     *         returned
     */
    Product[] getSelectedProducts();

    /**
     * Gets currently opened products of the given type. If the is <code>null</code> all currently opened products are
     * returned.
     *
     * @param productTypeId a product type identifier, can be <code>null</code>
     *
     * @return the array of currently open product, if there are no products opened at the time, <code>null</code> is
     *         returned
     */
    Product[] getOpenProducts(String productTypeId);


    /* @todo 3 nf/nf - provide more context information here: active, selected and open images etc. */
}

