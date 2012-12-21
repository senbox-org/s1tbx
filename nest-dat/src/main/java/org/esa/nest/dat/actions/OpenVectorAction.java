/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dat.actions;

import org.esa.beam.BeamUiActivator;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.layer.LayerSourceAssistantPane;
import org.esa.beam.framework.ui.layer.LayerSourceDescriptor;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.actions.AbstractVisatAction;
import org.esa.beam.visat.toolviews.layermanager.layersrc.SelectLayerSourceAssistantPage;

/**
 * This action opens a vector dataset
 *
 * @author lveci
 * @version $Revision: 1.7 $ $Date: 2011-04-08 18:23:59 $
 */
public class OpenVectorAction extends AbstractVisatAction {

    @Override
    public void actionPerformed(final CommandEvent event) {

        final LayerSourceAssistantPane pane = new LayerSourceAssistantPane(VisatApp.getApp().getApplicationWindow(),
                "Add Layer",
                getAppContext());
        final LayerSourceDescriptor[] layerSourceDescriptors = BeamUiActivator.getInstance().getLayerSources();
        pane.show(new SelectLayerSourceAssistantPage(layerSourceDescriptors));
    }

     @Override
    public void updateState(final CommandEvent event) {
        event.getCommand().setEnabled(VisatApp.getApp().getSelectedProductSceneView() != null);
    }
}