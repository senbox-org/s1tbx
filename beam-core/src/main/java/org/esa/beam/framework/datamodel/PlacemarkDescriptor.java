package org.esa.beam.framework.datamodel;

import java.awt.Image;
import java.awt.Point;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
public interface PlacemarkDescriptor {

    String getShowLayerCommandId();

    String getRoleName();

    String getRoleLabel();

    Image getCursorImage();

    PlacemarkGroup getPlacemarkGroup(Product product);

    PlacemarkSymbol createDefaultSymbol();

    PixelPos updatePixelPos(GeoCoding geoCoding, GeoPos geoPos, PixelPos pixelPos);

    GeoPos updateGeoPos(GeoCoding geoCoding, PixelPos pixelPos, GeoPos geoPos);

    Point getCursorHotSpot();
}
