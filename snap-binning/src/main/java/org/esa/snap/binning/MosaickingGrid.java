package org.esa.snap.binning;

import com.vividsolutions.jts.geom.Geometry;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.Product;

import java.awt.Dimension;
import java.awt.Rectangle;

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
