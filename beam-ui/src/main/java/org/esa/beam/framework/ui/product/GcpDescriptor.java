package org.esa.beam.framework.ui.product;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Pin;
import org.esa.beam.framework.datamodel.PinSymbol;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.ui.PlacemarkDescriptor;

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

    public String getCursorIconResourcePath() {
        return "cursors/GcpTool.gif";
    }

    public Point getIconHotSpot(Icon icon) {
        Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
        Dimension bestCursorSize = defaultToolkit.getBestCursorSize(icon.getIconWidth(), icon.getIconHeight());
        return new Point((int)Math.floor(bestCursorSize.width / 2), (int)Math.floor(bestCursorSize.getHeight() /2));
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
