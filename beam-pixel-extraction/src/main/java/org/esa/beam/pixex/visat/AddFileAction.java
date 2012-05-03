/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.pixex.visat;

import com.bc.ceres.binding.ValidationException;
import org.esa.beam.dataio.dimap.DimapProductConstants;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.visat.VisatApp;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * @author Thomas Storm
 */
class AddFileAction extends AbstractAction {

    private final AppContext appContext;
    private final InputListModel listModel;

    AddFileAction(AppContext appContext, InputListModel listModel) {
        super("Add product file(s)...");
        this.appContext = appContext;
        this.listModel = listModel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final PropertyMap preferences = appContext.getPreferences();
        String lastDir = preferences.getPropertyString(PixelExtractionIOForm.LAST_OPEN_INPUT_DIR,
                                                       SystemUtils.getUserHomeDir().getPath());
        String lastFormat = preferences.getPropertyString(PixelExtractionIOForm.LAST_OPEN_FORMAT,
                                                          DimapProductConstants.DIMAP_FORMAT_NAME);

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File(lastDir));
        fileChooser.setDialogTitle("Select product(s)");
        fileChooser.setMultiSelectionEnabled(true);

        FileFilter actualFileFilter = fileChooser.getAcceptAllFileFilter();
        Iterator<ProductReaderPlugIn> allReaderPlugIns = ProductIOPlugInManager.getInstance().getAllReaderPlugIns();
        List<BeamFileFilter> sortedFileFilters = BeamFileFilter.getSortedFileFilters(allReaderPlugIns);
        for (BeamFileFilter productFileFilter : sortedFileFilters) {
            fileChooser.addChoosableFileFilter(productFileFilter);
            if (!VisatApp.ALL_FILES_IDENTIFIER.equals(lastFormat) &&
                productFileFilter.getFormatName().equals(lastFormat)) {
                actualFileFilter = productFileFilter;
            }
        }
        fileChooser.setFileFilter(actualFileFilter);

        int result = fileChooser.showDialog(appContext.getApplicationWindow(), "Select product(s)");    /*I18N*/
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        preferences.setPropertyString(PixelExtractionIOForm.LAST_OPEN_INPUT_DIR,
                                      fileChooser.getCurrentDirectory().getAbsolutePath());

        final Object[] selectedProducts = fileChooser.getSelectedFiles();
        try {
            listModel.addElements(selectedProducts);
        } catch (ValidationException ve) {
            // not expected to ever come here
            appContext.handleError("Invalid input path", ve);
        }

        setLastOpenedFormat(preferences, selectedProducts);
    }

    private static void setLastOpenedFormat(PropertyMap preferences, Object[] selectedProducts) {
        String lastOpenedFormat = DimapProductConstants.DIMAP_FORMAT_NAME;
        if (selectedProducts.length > 0) {
            Object lastSelectedProduct = selectedProducts[selectedProducts.length - 1];
            ProductReader productReader = null;
            if (lastSelectedProduct instanceof File) {
                productReader = ProductIO.getProductReaderForFile((File) lastSelectedProduct);
            } else if (lastSelectedProduct instanceof Product) {
                productReader = ((Product) lastSelectedProduct).getProductReader();
            }
            if (productReader != null) {
                String[] formatNames = productReader.getReaderPlugIn().getFormatNames();
                if (formatNames.length > 0) {
                    lastOpenedFormat = formatNames[formatNames.length - 1];
                }
            }

        }
        preferences.setPropertyString(PixelExtractionIOForm.LAST_OPEN_FORMAT, lastOpenedFormat);
    }
}
