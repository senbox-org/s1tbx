package org.esa.beam.framework.ui;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Pin;
import org.esa.beam.framework.datamodel.PinSymbol;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNodeGroup;

import javax.swing.Icon;
import java.awt.Point;

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

    String getCursorIconResourcePath();

    ProductNodeGroup<Pin> getPlacemarkGroup(Product product);

    PinSymbol createDefaultSymbol();

    PixelPos updatePixelPos(GeoCoding geoCoding, GeoPos geoPos, PixelPos pixelPos);

    GeoPos updateGeoPos(GeoCoding geoCoding, PixelPos pixelPos, GeoPos geoPos);

    Point getIconHotSpot(Icon icon);
}
