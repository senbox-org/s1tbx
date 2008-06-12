/*
 * $Id: ShowImageViewRGBAction.java,v 1.1 2006/11/15 16:21:49 marcop Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
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
package org.esa.beam.visat.actions;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.visat.VisatApp;

/**
 * This action open a RGB image view on the currently selected Product.
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
public class ShowImageViewRGBAction extends ExecCommand {


    @Override
    public void actionPerformed(final CommandEvent event) {
        final Product product = VisatApp.getApp().getSelectedProduct();
        if (product != null) {
            VisatApp.getApp().openProductSceneViewRGB(product, getHelpId());
        }
    }

    @Override
    public void updateState(final CommandEvent event) {
        setEnabled(VisatApp.getApp().getSelectedProduct() != null);
    }


}
