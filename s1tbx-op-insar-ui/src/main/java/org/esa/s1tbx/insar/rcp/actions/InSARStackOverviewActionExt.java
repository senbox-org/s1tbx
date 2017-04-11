/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.s1tbx.insar.rcp.actions;

import org.esa.s1tbx.insar.rcp.dialogs.InSARStackOverviewDialog;
import org.esa.snap.engine_utilities.db.ProductEntry;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.productlibrary.rcp.toolviews.ProductLibraryActions;
import org.esa.snap.productlibrary.rcp.toolviews.extensions.ProductLibraryActionExt;
import org.esa.snap.ui.UIUtils;

import javax.swing.*;

/**
 * send ProductLibrary selection to InSARStackOverview
 */
public class InSARStackOverviewActionExt implements ProductLibraryActionExt {

    private static final ImageIcon stackIcon = UIUtils.loadImageIcon("/org/esa/s1tbx/insar/icons/stack24.png", InSARStackOverviewActionExt.class);
    private JButton button = null;
    private ProductLibraryActions actionHandler;

    public void setActionHandler(final ProductLibraryActions actionHandler) {
        this.actionHandler = actionHandler;
    }

    public JButton getButton(final JPanel panel) {
        if(button == null) {
            button = DialogUtils.createButton("stackButton", "Stack overview", stackIcon, panel, DialogUtils.ButtonStyle.Icon);
        }
        return button;
    }

    public void selectionChanged(final ProductEntry[] selections) {
        button.setEnabled(selections.length > 1);
    }

    public void performAction(final com.bc.ceres.core.ProgressMonitor pm) {
        final InSARStackOverviewDialog dialog = new InSARStackOverviewDialog();
        dialog.setInputProductList(actionHandler.getSelectedProductEntries());
        dialog.show();
    }
}
