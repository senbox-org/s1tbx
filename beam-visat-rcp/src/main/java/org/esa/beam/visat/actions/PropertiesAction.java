package org.esa.beam.visat.actions;

import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.dialogs.PropertyEditor;

public class PropertiesAction extends ExecCommand {

    public PropertiesAction() {

    }

    @Override
    public void actionPerformed(CommandEvent event) {
        showEditor(VisatApp.getApp());
    }

    @Override
    public void updateState(CommandEvent event) {
        final ProductNode node = VisatApp.getApp().getSelectedProductNode();
        event.getCommand().setEnabled(PropertyEditor.isValidNode(node));
    }

    private static void showEditor(final VisatApp visatApp) {
        final ProductNode selectedProductNode = visatApp.getSelectedProductNode();
        if (selectedProductNode != null) {
            final PropertyEditor propertyEditor = new PropertyEditor(visatApp);
            propertyEditor.show(selectedProductNode);
        }
        visatApp.updateState();
    }
}
