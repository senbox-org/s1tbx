package org.esa.beam.framework.datamodel;

import java.awt.Point;
import java.awt.Image;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision:$ $Date:$
 */
public class GcpDescriptor implements PlacemarkDescriptor {

    public final static GcpDescriptor INSTANCE = new GcpDescriptor();

    private GcpDescriptor() {
    }

    public String getShowLayerCommandId() {
        return "showGcpOverlay";
    }

    public String getRoleName() {
        return "gcp";
    }

    public String getRoleLabel() {
        return "GCP";
    }

    public Image getCursorImage() {
        return null;
    }

    public Point getCursorHotSpot() {
        return new Point();
    }

    public ProductNodeGroup<Pin> getPlacemarkGroup(Product product) {
        return product.getGcpGroup();
    }

    public PinSymbol createDefaultSymbol() {
        return PinSymbol.createDefaultGcpSymbol();
    }

    public PixelPos updatePixelPos(GeoCoding geoCoding, GeoPos geoPos, PixelPos pixelPos) {
        return pixelPos;
    }

    public GeoPos updateGeoPos(GeoCoding geoCoding, PixelPos pixelPos, GeoPos geoPos) {
        return geoPos;
    }
}
