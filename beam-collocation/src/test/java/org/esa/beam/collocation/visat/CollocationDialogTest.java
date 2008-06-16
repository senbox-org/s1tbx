package org.esa.beam.collocation.visat;

import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.ui.DefaultAppContext;
import org.esa.beam.framework.dataop.maptransf.MapInfo;
import org.esa.beam.framework.dataop.maptransf.MapProjectionRegistry;
import org.esa.beam.framework.dataop.maptransf.Datum;

import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.UIManager;
import javax.swing.JDialog;

public class CollocationDialogTest {
    public static void main(String[] args) throws IllegalAccessException, UnsupportedLookAndFeelException, InstantiationException, ClassNotFoundException {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        float[] wl = new float[]{
                412.6395569f,
                442.5160217f,
                489.8732910f,
                509.8299866f,
                559.7575684f,
                619.7247925f,
                664.7286987f,
                680.9848022f,
                708.4989624f,
                753.5312500f,
                761.7092285f,
                778.5520020f,
                864.8800049f,
                884.8975830f,
                899.9100342f
        };
        final Product inputProduct1 = new Product("MER_RR_1P", "MER_RR_1P", 16, 16);
        for (int i = 0; i < wl.length; i++) {
            Band band = new VirtualBand("radiance_" + (i + 1), ProductData.TYPE_FLOAT32, 16, 16, "X+Y");
            band.setSpectralWavelength(wl[i]);
            band.setSpectralBandIndex(i);
        }
        inputProduct1.addBand("l1_flags", ProductData.TYPE_UINT32);
        inputProduct1.addTiePointGrid(createTPG("latitude"));
        inputProduct1.addTiePointGrid(createTPG("longitude"));
        MapInfo mapInfo1 = new MapInfo(MapProjectionRegistry.getProjection("Geographic Lat/Lon"), 0.0f, 0.0f, 0.0f, 0.0f, 0.1f, 0.1f, Datum.WGS_84);
        mapInfo1.setSceneWidth(16);
        mapInfo1.setSceneHeight(16);
        inputProduct1.setGeoCoding(new MapGeoCoding(mapInfo1));

        final Product inputProduct2 = new Product("MER_RR_2P", "MER_RR_2P", 16, 16);
        for (int i = 0; i < wl.length; i++) {
            Band band = new VirtualBand("reflec_" + (i + 1), ProductData.TYPE_FLOAT32, 16, 16, "X*Y");
            band.setSpectralWavelength(wl[i]);
            band.setSpectralBandIndex(i);
        }
        inputProduct2.addBand("l2_flags", ProductData.TYPE_UINT32);
        inputProduct2.addTiePointGrid(createTPG("latitude"));
        inputProduct2.addTiePointGrid(createTPG("longitude"));
        MapInfo mapInfo2 = new MapInfo(MapProjectionRegistry.getProjection("Geographic Lat/Lon"), 0.0f, 0.0f, 0.2f, 0.2f, 0.1f, 0.1f, Datum.WGS_84);
        mapInfo2.setSceneWidth(16);
        mapInfo2.setSceneHeight(16);
        inputProduct2.setGeoCoding(new MapGeoCoding(mapInfo2));

        DefaultAppContext context = new DefaultAppContext("dev0");
        context.getProductManager().addProduct(inputProduct1);
        context.getProductManager().addProduct(inputProduct2);
        context.setSelectedProduct(inputProduct1);
        CollocationDialog dialog = new CollocationDialog(context);
        dialog.getJDialog().setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.show();
    }

    private static TiePointGrid createTPG(String name) {
        return new TiePointGrid(name, 5, 5, 0.5f, 0.5f, 4f, 4f, new float[5*5]);
    }
}
