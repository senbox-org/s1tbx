/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.dat.actions.reports;

import org.esa.s1tbx.dat.dialogs.ReportDialog;
import org.esa.s1tbx.dat.reports.MetadataReport;
import org.esa.snap.framework.ui.ModalDialog;
import org.esa.snap.framework.ui.command.CommandEvent;
import org.esa.snap.framework.ui.command.ExecCommand;
import org.esa.snap.rcp.SnapApp;

/**
 * This Action shows the report dialog
 */
public class ShowMetadataReport extends ExecCommand {

    @Override
    public void updateState(final CommandEvent event) {
        setEnabled(SnapApp.getDefault().getSelectedProduct() != null);
    }

    @Override
    public void actionPerformed(final CommandEvent event) {
        final ModalDialog dlg = new ReportDialog(new MetadataReport(SnapApp.getDefault().getSelectedProduct()));
        dlg.show();
    }
}
