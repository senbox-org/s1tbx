/*
 * Copyright (C) 2013 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.binning.operator;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.ProductUtils;
import org.geotools.geometry.jts.JTS;

import java.awt.geom.GeneralPath;

// todo - nf-20131031 - have a look at BinningOp.getRegionFromProductsExtent() where boundary geometry is already generated and try to reduce code duplication

/**
 * @author Norman Fomferra
 */
class RegionProductFilter extends BinningProductFilter {

    private final Geometry region;
    private final GeometryFactory factory;

    public RegionProductFilter(BinningProductFilter parent, Geometry region) {
        setParent(parent);
        this.region = region;
        this.factory = new GeometryFactory();
    }

    @Override
    protected boolean acceptForBinning(Product product) {
        GeneralPath[] geoBoundaryPaths = ProductUtils.createGeoBoundaryPaths(product);
        for (GeneralPath geoBoundaryPath : geoBoundaryPaths) {
            Geometry boundary = getPolygon(geoBoundaryPath);
            if (boundary.intersects(region)) {
                return true;
            }
        }
        setReason("Does not intersect the region.");
        return false;
    }

    private Geometry getPolygon(GeneralPath geoBoundaryPath) {
        Geometry boundary = JTS.shapeToGeometry(geoBoundaryPath, factory);
        if (boundary instanceof LinearRing) {
            boundary = factory.createPolygon((LinearRing) boundary, null);
        }
        return boundary;
    }
}
