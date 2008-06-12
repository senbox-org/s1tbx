/*
 * $Id: DeleteShapeAction.java,v 1.1 2006/11/15 16:21:48 marcop Exp $
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

import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.visat.VisatApp;

import javax.swing.JOptionPane;

/**
 * This action deletes the current shape.
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
public class DeleteShapeAction extends ExecCommand {

    @Override
    public void actionPerformed(final CommandEvent event) {
        deleteShape();
    }

    @Override
    public void updateState(final CommandEvent event) {
        setEnabled(isDeleteShapeActionPossible());
    }

    private void deleteShape() {
        final ProductSceneView productSceneView = VisatApp.getApp().getSelectedProductSceneView();
        if (productSceneView != null) {
            final int status = VisatApp.getApp().showQuestionDialog("Delete Shape Figure",
                                                                    "Do you really want to delete the shape assigned\n" +
                                                                    "to the current view?\n\n" +
                                                                    "This action cannot be undone.\n", /*I18N*/
                                                                                                       null);
            if (status == JOptionPane.YES_OPTION) {
                productSceneView.setCurrentShapeFigure(null);
                this.updateState();
            }
        }
    }

    private static boolean isDeleteShapeActionPossible() {
        final ProductSceneView productSceneView = VisatApp.getApp().getSelectedProductSceneView();
        boolean enabled = false;
        if (productSceneView != null) {
            if (productSceneView.getCurrentShapeFigure() != null) {
                enabled = true;
            }
        }
        return enabled;
    }

}
