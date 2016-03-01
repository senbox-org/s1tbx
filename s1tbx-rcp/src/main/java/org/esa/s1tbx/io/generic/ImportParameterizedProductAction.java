/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.io.generic;

import com.bc.ceres.core.Assert;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.actions.file.ProductFileChooser;
import org.esa.snap.rcp.actions.file.ProductOpener;
import org.esa.snap.rcp.util.Dialogs;
import org.openide.util.Cancellable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.prefs.Preferences;

/**
 * Generic configurable action for importing data products with parameters.
 *
 * @author Luis Veci
 */
public class ImportParameterizedProductAction extends AbstractAction {

    protected static final Set<String> KNOWN_KEYS = new HashSet<>(Arrays.asList("displayName", "formatName",
                                                                                "useAllFileFilter", "helpId"));

    public static ImportParameterizedProductAction create(Map<String, Object> properties) {
        ImportParameterizedProductAction action = new ImportParameterizedProductAction();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            if (KNOWN_KEYS.contains(entry.getKey())) {
                action.putValue(entry.getKey(), entry.getValue());
            }
        }
        return action;
    }

    public String getPropertyString(final String key) {
        Object value = getValue(key);
        if (value instanceof String) {
            return (String) value;
        }
        return null;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        openProduct();
    }

    private boolean openProduct() {
        Preferences preferences = SnapApp.getDefault().getPreferences();
        String userHomePath = SystemUtils.getUserHomeDir().getAbsolutePath();
        ProductFileChooser fc = new ProductFileChooser(new File(preferences.get(ProductOpener.PREFERENCES_KEY_LAST_PRODUCT_DIR, userHomePath)));
        fc.setSubsetEnabled(true);
        fc.setDialogTitle("Import");
        fc.setAcceptAllFileFilterUsed(true);

        fc.setMultiSelectionEnabled(true);
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);

        int returnVal = fc.showOpenDialog(SnapApp.getDefault().getMainFrame());
        if (returnVal != JFileChooser.APPROVE_OPTION) {
            // cancelled
            return false;
        }

        File[] files = fc.getSelectedFiles();

        if (files == null || files.length == 0) {
            // cancelled
            return false;
        }

        File currentDirectory = fc.getCurrentDirectory();
        if (currentDirectory != null) {
            preferences.put(ProductOpener.PREFERENCES_KEY_LAST_PRODUCT_DIR, currentDirectory.toString());
        }

        if (fc.getSubsetProduct() != null) {
            SnapApp.getDefault().getProductManager().addProduct(fc.getSubsetProduct());
            return true;
        }

        String formatName = getPropertyString("formatName");

        for (File file : files) {
            openProductFileDoNotCheckOpened(file, formatName);
        }
        return true;
    }

    private static Boolean openProductFileDoNotCheckOpened(File file, String formatName) {
        SnapApp.getDefault().setStatusBarMessage(MessageFormat.format("Reading product ''{0}''...", file.getName()));

        ReadProductOperation operation = new ReadProductOperation(file, formatName);
        operation.run();

        SnapApp.getDefault().setStatusBarMessage("");

        return operation.getStatus();
    }

    private static class ReadProductOperation implements Runnable, Cancellable {

        private final File file;
        private final String formatName;
        private Boolean status;

        public ReadProductOperation(File file, String formatName) {
            Assert.notNull(file, "file");
            Assert.notNull(formatName, "formatName");
            this.file = file;
            this.formatName = formatName;
        }

        public Boolean getStatus() {
            return status;
        }

        @Override
        public void run() {
            try {
                ProductReader reader = ProductIO.getProductReader(formatName);
                Product product = reader.readProductNodes(file, null);

                if (!Thread.interrupted()) {
                    if (product == null) {
                        status = false;
                        SwingUtilities.invokeLater(() -> Dialogs.showError(String.format("%nFile '%s' can not be opened.", file)));
                    } else {
                        status = true;
                        SwingUtilities.invokeLater(() -> SnapApp.getDefault().getProductManager().addProduct(product));
                    }
                } else {
                    status = null;
                }
            } catch (IOException problem) {
                status = false;
                SwingUtilities.invokeLater(() -> Dialogs.showError("Import", problem.getMessage()));
            }
        }

        @Override
        public boolean cancel() {
            Dialogs.Answer answer = Dialogs.requestDecision("Import",
                                                            "Do you really want to cancel the read process?",
                                                            false, null);
            boolean cancel = answer == Dialogs.Answer.YES;
            if (cancel) {
                status = null;
            }
            return cancel;
        }
    }
}
