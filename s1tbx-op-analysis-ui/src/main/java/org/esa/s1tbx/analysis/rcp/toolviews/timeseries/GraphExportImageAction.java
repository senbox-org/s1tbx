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

/**
 * Action for exporting graph as image.
 *//*
public class GraphExportImageAction extends AbstractExportImageAction {

    private final TimeSeriesDiagram diagram;

    public GraphExportImageAction(final TimeSeriesDiagram diagram) {
        this.diagram = diagram;
    }

    public void exportImage() {
        exportImage(VisatApp.getApp(), getSceneImageFileFilters(), this);
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
}*/
