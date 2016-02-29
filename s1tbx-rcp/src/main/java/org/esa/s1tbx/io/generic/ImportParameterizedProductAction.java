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

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
    /*    Preferences preferences = SnapApp.getDefault().getPreferences();
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
        }*/
        return true;
    }

//    private static Boolean openProductFileDoNotCheckOpened(File file, String formatName) {
//        SnapApp.getDefault().setStatusBarMessage(MessageFormat.format("Reading product ''{0}''...", file.getName()));
//
//        ReadProductOperation operation = new ReadProductOperation(file, formatName);
//        operation.run();
//
//        SnapApp.getDefault().setStatusBarMessage("");
//
//        return operation.getStatus();
//    }
}
