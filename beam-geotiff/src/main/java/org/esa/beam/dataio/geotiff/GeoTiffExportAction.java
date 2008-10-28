package org.esa.beam.dataio.geotiff;

import org.esa.beam.framework.datamodel.MapGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.actions.ProductExportAction;

import javax.swing.JOptionPane;
import java.io.File;

/**
 * todo - add API doc
 *
 * @author Marco Peters
 * @version $Revision: $ $Date: $
 * @since BEAM 4.5
 */
public class GeoTiffExportAction extends ProductExportAction {

    @Override
    protected File promptForFile(Product product) {
        if(!(product.getGeoCoding() instanceof MapGeoCoding)) {
            final String message = String.format("The product %s is not map projected.\n" +
                                                 "Un-projected raster data is not well supported by other GIS software.\n" +
                                                 "\n" +
                                                 "Do you want to export the product without a map projection?",
                                                 product.getName());
            final int answer = JOptionPane.showConfirmDialog(VisatApp.getApp().getMainFrame(), message, getText(),
                                                             JOptionPane.YES_NO_OPTION,
                                                             JOptionPane.WARNING_MESSAGE);
            if(answer != JOptionPane.YES_OPTION) {
                return null;
            }

        }
        return super.promptForFile(product);
    }
}
