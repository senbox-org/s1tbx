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