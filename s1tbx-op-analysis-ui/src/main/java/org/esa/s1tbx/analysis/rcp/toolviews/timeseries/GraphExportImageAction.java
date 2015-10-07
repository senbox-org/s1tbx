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
package org.esa.s1tbx.analysis.rcp.toolviews.timeseries;

import org.esa.snap.framework.ui.SnapFileChooser;
import org.esa.snap.framework.ui.product.ProductSceneView;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.actions.file.export.AbstractExportImageAction;
import org.esa.snap.util.io.SnapFileFilter;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;

/**
 * Action for exporting graph as image.
 */
public class GraphExportImageAction extends AbstractExportImageAction {

    private final static String[][] SCENE_IMAGE_FORMAT_DESCRIPTIONS = {
            BMP_FORMAT_DESCRIPTION,
            PNG_FORMAT_DESCRIPTION,
            JPEG_FORMAT_DESCRIPTION,
            TIFF_FORMAT_DESCRIPTION,
            GEOTIFF_FORMAT_DESCRIPTION,
    };
    private static final String HELP_ID = "exportImageFile";

    private final TimeSeriesDiagram diagram;
    private SnapFileFilter[] sceneImageFileFilters;

    public GraphExportImageAction(final TimeSeriesDiagram diagram) {
        super("Export Time Series Image", HELP_ID);
        this.diagram = diagram;

        sceneImageFileFilters = new SnapFileFilter[SCENE_IMAGE_FORMAT_DESCRIPTIONS.length];
        for (int i = 0; i < SCENE_IMAGE_FORMAT_DESCRIPTIONS.length; i++) {
            sceneImageFileFilters[i] = createFileFilter(SCENE_IMAGE_FORMAT_DESCRIPTIONS[i]);
        }
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        exportImage(sceneImageFileFilters);
    }

    public void exportImage() {
        exportImage(sceneImageFileFilters);
    }

    @Override
    public Action createContextAwareInstance(Lookup lookup) {
        return new GraphExportImageAction(diagram);
    }

    @Override
    public void resultChanged(LookupEvent lookupEvent) {
        setEnabled(SnapApp.getDefault().getSelectedProductSceneView() != null);
    }

    @Override
    protected void configureFileChooser(final SnapFileChooser fileChooser, final ProductSceneView view,
                                        String imageBaseName) {
        fileChooser.setDialogTitle("Export Image");
        fileChooser.setCurrentFilename(imageBaseName + "_" + view.getRaster().getName());
    }

    @Override
    protected RenderedImage createImage(String imageFormat, ProductSceneView view) {
        diagram.invalidate();
        final Rectangle r = diagram.getGraphArea();
        final BufferedImage img = new BufferedImage(r.x+r.width, r.y+r.height, BufferedImage.TYPE_3BYTE_BGR);

        final Graphics2D g = (Graphics2D)img.getGraphics();
        g.setColor(Color.white);
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        diagram.render(g, r.x, r.y, r.width, r.height);

        return img;
    }

    @Override
    protected boolean isEntireImageSelected() {
        return true;
    }
}
