package org.esa.beam.visat.actions;

import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductMetadataTable;
import org.esa.beam.framework.ui.product.ProductMetadataView;
import org.esa.beam.framework.ui.product.ProductNodeView;
import org.esa.beam.visat.VisatApp;

public class CollapseMetadataTableAction extends ExecCommand {

    /**
     * Invoked when a command action is performed.
     *
     * @param event the command event
     */
    @Override
    public void actionPerformed(CommandEvent event) {
        collapseMetadataTable();
    }

    /**
     * Called when a command should update its state.
     * <p/>
     * <p> This method can contain some code which analyzes the underlying element and makes a decision whether
     * this item or group should be made visible/invisible or enabled/disabled etc.
     *
     * @param event the command event
     */
    @Override
    public void updateState(CommandEvent event) {
        ProductNodeView view = VisatApp.getApp().getSelectedProductNodeView();
        boolean expandAllowed = false;
        if (view instanceof ProductMetadataView) {
            final ProductMetadataTable metadataTable = ((ProductMetadataView) view).getMetadataTable();
            expandAllowed = metadataTable.isExpandAllAllowed();
        }
        setEnabled(expandAllowed);
    }

    /////////////////////////////////////////////////////////////////////////
    // Private implementations for the "Export Metadata" command
    /////////////////////////////////////////////////////////////////////////

    private static void collapseMetadataTable() {

        ProductNodeView view = VisatApp.getApp().getSelectedProductNodeView();
        if (!(view instanceof ProductMetadataView)) {
            return;
        }

        final ProductMetadataView productMetadataView = (ProductMetadataView) view;
        final ProductMetadataTable metadataTable = productMetadataView.getMetadataTable();
        metadataTable.collapseAll();
    }

}