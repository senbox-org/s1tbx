package org.esa.snap.binning;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.ProductUtils;

import java.awt.*;
import java.awt.Dimension;
import java.awt.Polygon;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.util.ArrayList;

/**
 * A {@link PlanetaryGrid} that supports the {@link CompositingType} MOSAICKING.
 *
 * @author marcoz
 */
public interface MosaickingGrid extends PlanetaryGrid {

    Product reprojectToGrid(Product product);

    Rectangle[] getDataSliceRectangles(Geometry sourceProductGeometry, Dimension tileSize);

    GeoCoding getGeoCoding(Rectangle outputRegion);

}
