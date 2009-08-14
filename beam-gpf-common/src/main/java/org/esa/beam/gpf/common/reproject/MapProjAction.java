/*
 * $Id: $
 *
 * Copyright (C) 2009 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.gpf.common.reproject;

import org.esa.beam.framework.gpf.ui.DefaultSingleTargetProductDialog;
import org.esa.beam.framework.ui.ModelessDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.visat.actions.AbstractVisatAction;


public class MapProjAction extends AbstractVisatAction {

    private ModelessDialog dialog;

    @Override
    public void actionPerformed(final CommandEvent event) {
        if (dialog == null) {
            DefaultSingleTargetProductDialog dstpDialog =
                    new DefaultSingleTargetProductDialog("Reprojection",
                                                         getAppContext(),
                                                         "map projection operator (SPIKE)", 
                                                         "helpId");
            dstpDialog.setTargetProductNameSuffix("_projected");
            dialog = dstpDialog;
        }
        dialog.show();
    }
}
