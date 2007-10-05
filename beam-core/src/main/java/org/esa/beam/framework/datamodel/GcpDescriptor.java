package org.esa.beam.framework.datamodel;

import javax.swing.Icon;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Cursor;

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

    public Cursor getCursor() {
        // there also is a custom cursor icon in "cursors/GcpTool.gif"
        return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    }

    public Point getIconHotSpot(Icon icon) {
        Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
        Dimension bestCursorSize = defaultToolkit.getBestCursorSize(icon.getIconWidth(), icon.getIconHeight());
        return new Point((int) Math.floor(bestCursorSize.width / 2), (int) Math.floor(bestCursorSize.getHeight() / 2));
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
