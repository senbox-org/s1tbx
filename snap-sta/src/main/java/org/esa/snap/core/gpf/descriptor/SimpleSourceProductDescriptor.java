/*
 *
 *  * Copyright (C) 2016 CS ROMANIA
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  *  with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package org.esa.snap.core.gpf.descriptor;

import org.esa.snap.core.datamodel.Product;

/**
 * Created by kraftek on 1/30/2017.
 */
public class SimpleSourceProductDescriptor implements SourceProductDescriptor {

    private String name;
    private String alias;
    private String label;
    private String description;
    private Boolean optional;
    private String productType;
    private String[] bands;

    public SimpleSourceProductDescriptor(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getAlias() {
        return alias;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public boolean isOptional() {
        return optional != null ? optional : false;
    }

    @Override
    public String getProductType() {
        return productType;
    }

    @Override
    public String[] getBands() {
        return bands != null ? bands : new String[0];
    }

    @Override
    public Class<? extends Product> getDataType() {
        return Product.class;
    }
}
