package org.esa.beam.framework.datamodel;

import java.awt.Image;
import java.awt.Point;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
public class PinDescriptor implements PlacemarkDescriptor {

    public static final PinDescriptor INSTANCE = new PinDescriptor();

    private PinDescriptor() {
    }

    @Override
    public String getShowLayerCommandId() {
        return "showPinOverlay";
    }

    @Override
    public String getRoleName() {
        return "pin";
    }

    @Override
    public String getRoleLabel() {
        return "pin";
    }

    @Override
    public Image getCursorImage() {
        return null;
    }

    @Override
    public Point getCursorHotSpot() {
        return new Point();
    }


    @Override
    public PlacemarkGroup getPlacemarkGroup(Product product) {
        return product.getPinGroup();
    }

    @Override
    public PlacemarkSymbol createDefaultSymbol() {
        return PlacemarkSymbol.createDefaultPinSymbol();
    }

    @Override
    public PixelPos updatePixelPos(GeoCoding geoCoding, GeoPos geoPos, PixelPos pixelPos) {
        if (geoCoding == null || !geoCoding.canGetPixelPos() || geoPos == null) {
            return pixelPos;
        }
        return geoCoding.getPixelPos(geoPos, pixelPos);
    }

    @Override
    public GeoPos updateGeoPos(GeoCoding geoCoding, PixelPos pixelPos, GeoPos geoPos) {
        if (geoCoding == null || !geoCoding.canGetGeoPos()) {
            return geoPos;
        }
        return geoCoding.getGeoPos(pixelPos, geoPos);

    }
}
