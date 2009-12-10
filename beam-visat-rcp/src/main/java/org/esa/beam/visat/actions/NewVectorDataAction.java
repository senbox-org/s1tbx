/*
 * $Id: CreateFilteredBandAction.java,v 1.1 2007/04/19 10:16:12 marcop Exp $
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
import org.esa.beam.framework.datamodel.VectorData;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.ValueEditorsPane;
import org.esa.beam.framework.ui.product.SimpleFeatureFigureFactory;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.visat.VisatApp;
import org.opengis.feature.simple.SimpleFeatureType;

import javax.swing.JPanel;
import java.awt.Dimension;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertySet;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Creates a new vector data node.
 * @author Norman Fomferra
 * @since BEAM 4.7
 */
public class NewVectorDataAction extends ExecCommand {

    // todo - add validators (nf)
    static class DialogData {
        String name;
        String description;
    }


    @Override
    public void actionPerformed(CommandEvent event) {
        DialogData data = new DialogData();
        PropertySet propertySet = PropertyContainer.createObjectBacked(data);
        ValueEditorsPane valueEditorsPane = new ValueEditorsPane(propertySet);
        JPanel panel = valueEditorsPane.createPanel();
        panel.setPreferredSize(new Dimension(200, -1));
        ModalDialog dialog = new ModalDialog(VisatApp.getApp().getMainFrame(),
                                             "New Vector Data",
                                             ModalDialog.ID_OK | ModalDialog.ID_CANCEL,
                                             "");
        dialog.setContent(panel);
        int i = dialog.show();
        if (i == ModalDialog.ID_OK) {
            Product product = VisatApp.getApp().getSelectedProduct();
            // todo - always use same schema name! (nf)
            SimpleFeatureType type = SimpleFeatureFigureFactory.createSimpleFeatureType("X", Geometry.class);
            product.getVectorDataGroup().add(new VectorData(data.name, type));
        }
    }

    /**
     * Causes this command to fire the 'check status' event to all of its listeners.
     */
    @Override
    public void updateState() {
        Product product = VisatApp.getApp().getSelectedProduct();
        setEnabled(product != null);
    }

}