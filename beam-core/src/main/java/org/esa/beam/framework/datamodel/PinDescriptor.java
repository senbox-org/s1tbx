package org.esa.beam.framework.datamodel;

import javax.swing.Icon;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;

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

    public String getCursorIconResourcePath() {
        return "cursors/PinTool.gif";
    }

    public Point getIconHotSpot(Icon icon) {
        Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
        Dimension bestCursorSize = defaultToolkit.getBestCursorSize(icon.getIconWidth(), icon.getIconHeight());
        return new Point((7 * bestCursorSize.width) / icon.getIconWidth(),
                         (7 * bestCursorSize.height) / icon.getIconHeight());
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
