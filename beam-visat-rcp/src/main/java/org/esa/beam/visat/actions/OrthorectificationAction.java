package org.esa.beam.visat.actions;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.dialogs.MapProjectionDialog;

import javax.swing.JOptionPane;
import java.util.ArrayList;

public class OrthorectificationAction extends ExecCommand {

    @Override
    public void actionPerformed(CommandEvent event) {
        openProjectionDialog(VisatApp.getApp(), getHelpId());
    }

    @Override
    public void updateState(CommandEvent event) {
        final Product product = VisatApp.getApp().getSelectedProduct();
        setEnabled(canBeOrthorectified(product));
    }

    private static boolean canBeOrthorectified(Product product) {
        if (product != null) {
            return product.canBeOrthorectified();
        } else {
            return false;
        }
    }

    private static void openProjectionDialog(final VisatApp visatApp, String helpId) {

        final Product sourceProduct = visatApp.getSelectedProduct();
        if (!sourceProduct.canBeOrthorectified()) {
            visatApp.showErrorDialog("The selected product cannot be orthorectified at all."); /*I18N*/
            return;
        }

        ArrayList<Band> problemBandList = getOrthorectionProblemBandList(sourceProduct);
        if (!problemBandList.isEmpty()) {
            StringBuffer sb = new StringBuffer();
            sb.append("The following bands cannot be orthorectified and\n" +
                      "will simply be map-projected:\n");
            for (int i = 0; i < problemBandList.size(); i++) {
                Band band = problemBandList.get(i);
                sb.append("    ");
                sb.append(band.getName());
                sb.append("\n");
            }
            sb.append("Do you want to continue?");
            String message = sb.toString();
            final int answer = JOptionPane.showConfirmDialog(visatApp.getMainFrame(),
                                                             message,
                                                             "Orthorectification Problems",
                                                             JOptionPane.YES_NO_OPTION,
                                                             JOptionPane.WARNING_MESSAGE);
            if (answer != JOptionPane.YES_OPTION) {
                return;
            }
        }

        final MapProjectionDialog dialog = new MapProjectionDialog(visatApp.getMainFrame(), sourceProduct, true);
        if (helpId != null && helpId.length() > 0) {
            HelpSys.enableHelp(dialog.getJDialog(), helpId);
            HelpSys.enableHelpKey(dialog.getJDialog(), helpId);
        }

        if (dialog.show() == ModalDialog.ID_OK) {
            final Product outputProduct = dialog.getOutputProduct();
            if (outputProduct != null) {
                visatApp.addProduct(outputProduct);
            } else if (dialog.getException() != null) {
                visatApp.showErrorDialog("Orthorectified product could not be created:\n" +
                                         dialog.getException().getMessage());                       /*I18N*/
            }
        }
    }

    private static ArrayList<Band> getOrthorectionProblemBandList(Product sourceProduct) {
        ArrayList<Band> problemBandList = new ArrayList<Band>();
        for (int i = 0; i < sourceProduct.getNumBands(); i++) {
            final Band sourceBand = sourceProduct.getBandAt(i);
            if (!sourceBand.canBeOrthorectified()) {
                problemBandList.add(sourceBand);
            }
        }
        return problemBandList;
    }
}
