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

package com.bc.ceres.swing.figure.support;

import com.bc.ceres.swing.figure.Figure;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

public class FigureTransferable implements Transferable {

    public static final DataFlavor FIGURES_DATA_FLAVOR = new DataFlavor(FigureTransferable.class, "Figures");

    private final Figure[] figures;
    private final boolean snapshot;

    public FigureTransferable(Figure[] figures, boolean snapshot) {
        this.figures = figures.clone();
        this.snapshot = snapshot;
        if (snapshot) {
            for (int i = 0; i < this.figures.length; i++) {
                Figure figure = this.figures[i];
                this.figures[i] = (Figure) figure.clone();
            }
        }
    }

    public boolean isSnapshot() {
        return snapshot;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{FIGURES_DATA_FLAVOR};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavor.equals(FIGURES_DATA_FLAVOR);
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if (flavor.equals(FIGURES_DATA_FLAVOR)) {
            if (snapshot) {
                // E.g. COPY + PASTE
                Figure[] figures1 = figures.clone();
                for (int i = 0; i < figures1.length; i++) {
                    figures1[i] = (Figure) figures1[i].clone();
                }
                return figures1;
            } else {
                // E.g. CUT + PASTE
                return figures;
            }
        }
        return null;
    }

    public void dispose() {
        if (snapshot) {
            for (Figure figure : figures) {
                figure.dispose();
            }
        }
    }
}
