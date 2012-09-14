package org.esa.nest.dat.views.polarview;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

class ReadoutCanvas {

    private final Point readoutOrigin = new Point(20, 40);
    private String[] readoutList = null;
    private String[] metadataList = null;

    public ReadoutCanvas() {

    }

    public void setReadout(String[] readout) {
        this.readoutList = readout;
    }

    public void setMetadata(String[] metadata) {
        this.metadataList = metadata;
    }

    public void paint(Graphics g) {
            draw(g);
    }

    private synchronized void draw(Graphics g) {
        g.translate(readoutOrigin.x, readoutOrigin.y);
        if(readoutList != null) {

            int x = 0, y = 0;
            for(String str : readoutList) {
                g.drawString(str, x, y);
                y += 20;
            }
        }
        if(metadataList != null) {
            g.translate((int)g.getClipBounds().getWidth() - 230, 0);

            int x = 0, y = 0;
            for(String str : metadataList) {
                g.drawString(str, x, y);
                y += 20;
            }
        }
    }

    public void exportReadout(final File file) throws IOException {
        if(metadataList != null) {
            final FileOutputStream out = new FileOutputStream(file.getAbsolutePath(), false);
            // Connect print stream to the output stream
            final PrintStream p = new PrintStream(out);

            if(readoutList != null) {
                for(String str : readoutList) {
                    p.println(str +",");
                }
            }

            for(String str : metadataList) {
                p.println(str +",");
            }
        }
    }

}