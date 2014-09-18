package org.esa.beam.framework.help;

import javax.swing.JOptionPane;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author Tonio Fincke
 */
public class ContextHelp {

    public static void showContextHelp(String rasterDataNodeName, String productType) {
        if(Desktop.isDesktopSupported()) {
            final Desktop desktop = Desktop.getDesktop();
            if(desktop.isSupported(Desktop.Action.BROWSE)) {
                //todo make search engine choosable
                try {
                    desktop.browse(new URI("http://www.google.com/search?q=site:esa.int+" + rasterDataNodeName));
//                    desktop.browse(new URI("http://www.google.com/search?q=site:esa.int+" + rasterDataNodeName + "+" + productType));
                    return;
                } catch (IOException | URISyntaxException e) {
                    e.printStackTrace();
                }
            }
        }
        //todo remove null and insert correct component
        JOptionPane.showMessageDialog(null, "Cannot open context help: Access to Browser is not supported");
    }

}
