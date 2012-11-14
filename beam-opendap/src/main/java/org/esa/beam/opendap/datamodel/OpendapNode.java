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

package org.esa.beam.opendap.datamodel;

import thredds.catalog.InvDataset;

/**
 * @author Tonio Fincke
 * @author Thomas Storm
 */
public abstract class OpendapNode {

    private final String name;
    private final InvDataset dataset;

    public OpendapNode(String name, InvDataset dataset) {
        this.name = name;
        this.dataset = dataset;
    }

    public String getName() {
        return name;
    }

    public InvDataset getDataset() {
        return dataset;
    }

    @Override
    public String toString() {
        return name;
    }
}
