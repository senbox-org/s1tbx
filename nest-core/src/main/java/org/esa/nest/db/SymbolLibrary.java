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
package org.esa.nest.db;

import org.esa.beam.visat.VisatApp;
import org.esa.nest.util.ResourceUtils;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Caches symbols
 */
public class SymbolLibrary {

    private final static File symbolFolder = new File(ResourceUtils.getResFolder(), "symbols");
    private final static String symbolFolderStr = symbolFolder.getAbsolutePath();
    private final static String symbolFolderPlaceHolder = "{$SYMBOL_FOLDER}";
    private final static File defaultSymbolFile = new File(symbolFolder, "flag_blue.png");

    private static SymbolLibrary _instance = null;
    private static final Map<File, SymbolNode> fileMap = new HashMap<File, SymbolNode>(20);
    private final SymbolNode rootNode;

    public static SymbolLibrary instance() {
        if(_instance == null) {
            _instance = new SymbolLibrary();
        }
        return _instance;
    }

    private SymbolLibrary() {
        rootNode = new SymbolNode(symbolFolder);
        rootNode.scanSymbolFolder(symbolFolder, fileMap);
    }

    public SymbolNode getRootNode() {
        return rootNode;
    }

    public static SymbolNode getSymbol(final File file) {
        return fileMap.get(file);
    }

    public static ImageIcon getIcon(final File file) {
        return fileMap.get(file).getIcon();
    }

    public static BufferedImage getImage(final File file) {
        return fileMap.get(file).getImage();
    }

    public static SymbolNode getDefaultSymbol() {
        SymbolNode defaultSymbol = fileMap.get(getLastSymbolUsed());
        if(defaultSymbol == null) {
            defaultSymbol = fileMap.get(fileMap.keySet().iterator().next());
        }
        return defaultSymbol;
    }

    private static File getLastSymbolUsed() {
        if(VisatApp.getApp() != null) {
            final String path = VisatApp.getApp().getPreferences().getPropertyString("last_symbol_used", "");
            if(path != null && !path.isEmpty()) {
                return new File(path);
            }
        }
        return defaultSymbolFile;
    }

    public static void saveLastSymbolUsed(final File file) {
        if(VisatApp.getApp() != null && file != null) {
            VisatApp.getApp().getPreferences().setPropertyString("last_symbol_used", file.getAbsolutePath());
        }
    }

    public static String translatePath(final File file) {
        if(file == null) return "";
        String filePath = file.getAbsolutePath();
        if(filePath.startsWith(symbolFolderStr)) {
            filePath = filePath.replace(symbolFolderStr, symbolFolderPlaceHolder);
        }
        return filePath;
    }

    public static File translatePath(String path) {
        if(path == null || path.isEmpty()) return null;
        if(path.startsWith(symbolFolderPlaceHolder)) {
            path = path.replace(symbolFolderPlaceHolder, symbolFolderStr);
        }
        return new File(path);
    }

    public static String clipPath(final File file) {
        String path = file.getAbsolutePath();
        if(path.equals(symbolFolderStr))
            return File.separator;
        else if(path.startsWith(symbolFolderStr)) {
            path = path.replace(symbolFolderStr, "");
        }
        return path;
    }
}
