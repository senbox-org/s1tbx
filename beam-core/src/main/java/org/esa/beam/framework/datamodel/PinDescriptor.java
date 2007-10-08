package org.esa.beam.framework.datamodel;

import java.awt.Image;
import java.awt.Point;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision:$ $Date:$
 */
public class PinDescriptor implements PlacemarkDescriptor {

    public final static PinDescriptor INSTANCE = new PinDescriptor();

    private PinDescriptor() {
    }

    public String getShowLayerCommandId() {
        return "showPinOverlay";
    }

    public String getRoleName() {
        return "pin";
    }

    public String getRoleLabel() {
        return "pin";
    }

    public Image getCursorImage() {
        return null;
    }

    public Point getCursorHotSpot() {
        return new Point();
    }


    public ProductNodeGroup<Pin> getPlacemarkGroup(Product product) {
        return product.getPinGroup();
    }

    public PinSymbol createDefaultSymbol() {
        return PinSymbol.createDefaultPinSymbol();
    }

    public PixelPos updatePixelPos(GeoCoding geoCoding, GeoPos geoPos, PixelPos pixelPos) {
        if (geoCoding == null || !geoCoding.canGetPixelPos()) {
            return pixelPos;
        }
        return geoCoding.getPixelPos(geoPos, pixelPos);
    }

    public GeoPos updateGeoPos(GeoCoding geoCoding, PixelPos pixelPos, GeoPos geoPos) {
        if (geoCoding == null || !geoCoding.canGetGeoPos()) {
            return geoPos;
        }
        return geoCoding.getGeoPos(pixelPos, geoPos);

    }
}
