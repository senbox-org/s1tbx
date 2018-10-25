package org.esa.snap.binning;

import com.vividsolutions.jts.geom.Geometry;
import java.awt.Dimension;
import java.awt.Rectangle;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.Product;

/**
 * TODO add API doc
 *
 * @author marcoz
 */
public interface MosaickingGrid extends PlanetaryGrid {
    Product reprojectToGrid(Product var1);

    Rectangle[] getDataSliceRectangles(Geometry var1, Dimension var2);

    GeoCoding getGeoCoding(Rectangle var1);
}
