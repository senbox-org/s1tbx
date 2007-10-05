package org.esa.beam.framework.datamodel;

import javax.swing.Icon;
import java.awt.Point;
import java.awt.Cursor;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision:$ $Date:$
 */
public interface PlacemarkDescriptor {

    String getShowLayerCommandId();

    String getRoleName();

    String getRoleLabel();

    Cursor getCursor();

    ProductNodeGroup<Pin> getPlacemarkGroup(Product product);

    PinSymbol createDefaultSymbol();

    PixelPos updatePixelPos(GeoCoding geoCoding, GeoPos geoPos, PixelPos pixelPos);

    GeoPos updateGeoPos(GeoCoding geoCoding, PixelPos pixelPos, GeoPos geoPos);

    Point getIconHotSpot(Icon icon);
}
