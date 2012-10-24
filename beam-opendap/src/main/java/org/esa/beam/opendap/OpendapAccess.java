package org.esa.beam.opendap;

import com.jidesoft.utils.Lm;
import org.esa.beam.framework.gpf.ui.DefaultAppContext;
import org.esa.beam.opendap.ui.OpendapAccessPanel;

import javax.swing.JFrame;
import javax.swing.UIManager;

/**
 * @author Tonio Fincke
 */
public class OpendapAccess {

    public static void main(String[] args) {
        Lm.verifyLicense("Brockmann Consult", "BEAM", "lCzfhklpZ9ryjomwWxfdupxIcuIoCxg2");

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        final OpendapAccessPanel opendapAccessPanel = new OpendapAccessPanel(new DefaultAppContext(""), "");
        final JFrame mainFrame = new JFrame("OPeNDAP Access");
        mainFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        mainFrame.setContentPane(opendapAccessPanel);
        mainFrame.pack();
        mainFrame.setVisible(true);
    }

}
