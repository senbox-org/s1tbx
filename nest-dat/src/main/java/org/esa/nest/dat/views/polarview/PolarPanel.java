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
package org.esa.nest.dat.views.polarview;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

/**

 */
class PolarPanel extends JPanel {

    private final PolarCanvas polarCanvas;
    private final ReadoutCanvas readoutCanvas;

    PolarPanel() {
        polarCanvas = new PolarCanvas();
        readoutCanvas = new ReadoutCanvas();
    }

    /**
     * Paints the panel component
     *
     * @param g The Graphics
     */
    @Override
    public void paint(Graphics g) {
        super.paintComponent(g);

        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        polarCanvas.setSize(getWidth(), getHeight());
        polarCanvas.paint(g);
        readoutCanvas.paint(g);
    }

    PolarCanvas getPolarCanvas() {
        return polarCanvas;
    }

    public void setMetadata(String[] metadataList) {
        readoutCanvas.setMetadata(metadataList);
    }

    public void setReadout(String[] readoutList) {
        readoutCanvas.setReadout(readoutList);
    }

    public void exportReadout(final File file) throws IOException {
        readoutCanvas.exportReadout(file);
    }
}