/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package com.bc.swing.desktop;

import java.awt.Dimension;
import java.beans.PropertyVetoException;

import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;

/**
 * @author Norman Fomferra (norman.fomferra@brockmann-consult.de)
 * @version $Revision$ $Date$
 */
public class DefaultInternalFrameLayoutManager implements InternalFrameLayoutManager {
    public void moveFrameToVisible(final JDesktopPane desktopPane, final JInternalFrame internalFrame) {
        final int delta = 128;
        int x = internalFrame.getX();
        if (x > desktopPane.getWidth() - delta) {
            x = desktopPane.getWidth() - delta;
        }
        if (x < 0) {
            x = 0;
        }
        int y = internalFrame.getY();
        if (y > desktopPane.getHeight() - delta) {
            y = desktopPane.getHeight() - delta;
        }
        if (y < 0) {
            y = 0;
        }
        internalFrame.setLocation(x, y);
    }

    public void cascadeFrames(final JDesktopPane desktopPane, final JInternalFrame[] frames) {
        if (frames.length == 0) {
            return;
        }
        final int delta = 24;
        int x = 0, y = 0;
        int nCols = 0, nRows = 0;
        for (int i = 0; i < frames.length; i++) {
            frames[i].setLocation(x, y);
            x += delta;
            y += delta;
            if (x > desktopPane.getWidth() - delta) {
                nCols++;
                x = nCols * delta;
                y = nRows * delta;
            }
            if (y > desktopPane.getHeight() - delta) {
                nRows++;
                x = nCols * delta;
                y = nRows * delta;
            }
        }
    }

    public void tileFramesEvenly(final JDesktopPane desktopPane, final JInternalFrame[] frames) {
        if (frames.length == 0) {
            return;
        }
        final Dimension d = getTileSubdivision(desktopPane, frames.length);
        final int widthTot = desktopPane.getWidth();
        final int heightTot = desktopPane.getHeight();
        int y = 0;
        for (int j = 0; j < d.height; j++) {
            int h = heightTot / d.height;
            if (j == d.height - 1) {
                h = heightTot - h * (d.height - 1);
            }
            int x = 0;
            for (int i = 0; i < d.width; i++) {
                int w = widthTot / d.width;
                if (i == d.width - 1) {
                    w = widthTot - w * (d.width - 1);
                }
                int tileIndex = j * d.width + i;
                if (tileIndex < frames.length) {
                    final JInternalFrame internalFrame = frames[tileIndex];
                    try {
                        internalFrame.setMaximum(false);
                    } catch (PropertyVetoException e) {
                        //ignore
                    }
                    internalFrame.setBounds(x, y, w, h);
                }
                x += w;
            }
            y += h;
        }
    }

    public void tileFramesHorizontally(final JDesktopPane desktopPane, final JInternalFrame[] frames) {
        if (frames.length == 0) {
            return;
        }
        final int widthTot = desktopPane.getWidth();
        final int heightTot = desktopPane.getHeight();
        int x = 0;
        final int n = frames.length;
        for (int i = 0; i < n; i++) {
            int w = widthTot / n;
            if (i == n - 1) {
                w = widthTot - w * (n - 1);
            }
            try {
                frames[i].setMaximum(false);
            } catch (PropertyVetoException e) {
                //ignore
            }
            frames[i].setBounds(x, 0, w, heightTot);
            x += w;
        }
    }

    public void tileFramesVertically(final JDesktopPane desktopPane, final JInternalFrame[] frames) {
        if (frames.length == 0) {
            return;
        }
        final int widthTot = desktopPane.getWidth();
        final int heightTot = desktopPane.getHeight();
        int y = 0;
        final int n = frames.length;
        for (int i = 0; i < n; i++) {
            int h = heightTot / n;
            if (i == n - 1) {
                h = heightTot - h * (n - 1);
            }
            try {
                frames[i].setMaximum(false);
            } catch (PropertyVetoException e) {
                //ignore
            }
            frames[i].setBounds(0, y, widthTot, h);
            y += h;
        }
    }

    private Dimension getTileSubdivision(final JDesktopPane desktopPane, final int numTiles) {
        int numHorTiles = (int) Math.round(Math.sqrt(numTiles));
        if (numHorTiles == 0) {
            numHorTiles = 1;
        }
        int numVerTiles = numTiles / numHorTiles;
        while (numHorTiles * numVerTiles < numTiles) {
            numVerTiles++;
        }
        double horTileSize1 =  (double) desktopPane.getWidth() / numHorTiles;
        double verTileSize1 =  (double) desktopPane.getHeight() / numVerTiles;
        double ratio1 = verTileSize1 != 0 ? horTileSize1 / verTileSize1 : 1.0;
        double horTileSize2 =  (double) desktopPane.getWidth() / numVerTiles;
        double verTileSize2 =  (double) desktopPane.getHeight() / numHorTiles;
        double ratio2 = verTileSize2 != 0 ? horTileSize2 / verTileSize2 : 1.0;
        if (Math.abs(1 - ratio1) < Math.abs(1 - ratio2)) {
            return new Dimension(numHorTiles, numVerTiles);
        } else {
            return new Dimension(numVerTiles, numHorTiles);
        }
    }
}
