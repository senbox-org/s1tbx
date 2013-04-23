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
 * {@link OpendapLeaf} that serves as catalog leaf. The URI points to a catalog that contains datasets.
 *
 * @author Tonio Fincke
 * @author Thomas Storm
 */
public class CatalogNode extends OpendapNode {

    private String catalogUri;

    public CatalogNode(String name, InvDataset dataset) {
        super(name, dataset);
    }

    public String getCatalogUri() {
        return catalogUri;
    }

    public void setCatalogUri(String catalogUri) {
        this.catalogUri = catalogUri;
    }
}
