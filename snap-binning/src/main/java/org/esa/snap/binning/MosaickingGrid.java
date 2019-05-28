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

    static Rectangle alignToTileGrid(Rectangle rectangle, Dimension tileSize) {
        int minX = (rectangle.x / tileSize.width) * tileSize.width;
        int maxX = (rectangle.x + rectangle.width + tileSize.width - 1) / tileSize.width * tileSize.width;
        int minY = (rectangle.y / tileSize.height) * tileSize.height;
        int maxY = (rectangle.y + rectangle.height + tileSize.height - 1) / tileSize.height * tileSize.height;
        return new Rectangle(minX, minY, maxX - minX, maxY - minY);
    }

}
