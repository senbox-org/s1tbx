/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.gpf.operators.reproject;

import org.esa.beam.framework.ui.ModelessDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.visat.actions.AbstractVisatAction;

/**
 * Geographic collocation action.
 *
 * @author Ralf Quast
 * @version $Revision: 2535 $ $Date: 2008-07-09 14:10:01 +0200 (Mi, 09 Jul 2008) $
 */
public class OrthorectificationAction extends AbstractVisatAction {

    private ModelessDialog dialog;

    @Override
    public void actionPerformed(CommandEvent event) {
        if (dialog == null) {
            dialog = new ReprojectionDialog(true, "Orthorectification", event.getCommand().getHelpId(), getAppContext());
        }
        dialog.show();
    }

}