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
package org.jlinda.nest.dat;

import org.jlinda.nest.dat.dialogs.InSARMasterDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class InSARMasterAction extends AbstractAction {

    @Override
    public void actionPerformed(final ActionEvent event) {
        final InSARMasterDialog dialog = new InSARMasterDialog();
        dialog.show();
    }

}
