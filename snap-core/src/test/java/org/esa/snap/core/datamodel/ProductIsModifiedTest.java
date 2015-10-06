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

import junit.framework.TestCase;

public class ProductIsModifiedTest extends TestCase {

    public ProductIsModifiedTest(String testName) {
        super(testName);
    }

    /*
     * @todo 1 se/se - tests must be implemented to ensure the modified flag
     * usability for save only modified product nodes.
     */
    public void testProductConstructors() {
        createProduct();
    }

    private Product createProduct() {
        return new Product("product", "TestProduct", 20, 30);
    }
}
