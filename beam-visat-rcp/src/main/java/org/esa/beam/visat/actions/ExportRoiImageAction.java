/*
 * $Id: ExportRoiImageAction.java,v 1.2 2007/02/09 11:05:57 marcop Exp $
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
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.io.BeamFileChooser;

import java.awt.image.RenderedImage;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
public class ExportRoiImageAction extends AbstractExportImageAction {

    @Override
    public void actionPerformed(CommandEvent event) {
        exportImage(getVisatApp(), getSceneImageFileFilters(), event.getSelectableCommand());

    }

    @Override
    public void updateState(final CommandEvent event) {
        ProductSceneView view = getVisatApp().getSelectedProductSceneView();
        boolean enabled = view != null && view.getROIImage() != null;
        event.getSelectableCommand().setEnabled(enabled);

    }

    @Override
    protected RenderedImage createImage(String imageFormat, ProductSceneView view) {
        return view.getROIImage();
    }

    @Override
    protected void configureFileChooser(BeamFileChooser fileChooser, ProductSceneView view, String imageBaseName) {
        fileChooser.setDialogTitle(getVisatApp().getAppName() + " - Export ROI Overlay Image"); /*I18N*/
        fileChooser.setCurrentFilename(imageBaseName + "_roi");
    }

    @Override
    protected boolean isEntireImageSelected() {
        return true;
    }

}
