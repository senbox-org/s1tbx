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
package org.esa.beam.visat.actions;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.visat.VisatApp;

/**
 * This action saves the selected product.
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
public class SaveAction extends ExecCommand {

    @Override
    public void actionPerformed(final CommandEvent event) {
        VisatApp.getApp().saveSelectedProduct();
    }

    @Override
    public void updateState(final CommandEvent event) {
        boolean enable = false;
        final ProductNode selectedProductNode = VisatApp.getApp().getSelectedProductNode();
        if (selectedProductNode != null) {
            ProductReader productReader = selectedProductNode.getProductReader();
            if (productReader != null) {
                ProductReaderPlugIn readerPlugIn = productReader.getReaderPlugIn();
                if (readerPlugIn != null) {
                    String[] formatNames = readerPlugIn.getFormatNames();
                    for (int i = 0; i < formatNames.length && !enable; i++) {
                        String formatName = formatNames[i];
                        ProductWriter writer = ProductIO.getProductWriter(formatName);
                        if (writer != null) {
                            enable = true;
                        }
                    }
                } else {
                    // No ReaderPlugIn found so the reader is some kind of AbstractProductBuilder
                    // --> Save should be always anabled
                    enable = true;
                }
            }
        }
        setEnabled(enable);
    }
}
