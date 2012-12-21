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

import org.esa.nest.util.ResourceUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public  class SymbolNode {
    private final File file;
    private final boolean isFolder;
    private ImageIcon imgIcon = null;
    private BufferedImage bi = null;
    private final List<SymbolNode> subNodes = new ArrayList<SymbolNode>();

    public SymbolNode(final File file) {
        this.file = file;
        this.isFolder = file.isDirectory();
    }

    public void scanSymbolFolder(final File folder, final Map<File, SymbolNode> fileMap) {
        final File[] files = folder.listFiles(new ImageFileFilter());
        for(File file : files) {
            final SymbolNode newNode = new SymbolNode(file);
            if(file.isDirectory()) {
                newNode.scanSymbolFolder(file, fileMap);
            }
            subNodes.add(newNode);
            fileMap.put(file, newNode);
        }
    }

    public File getFile() {
        return file;
    }

    public boolean isFolder() {
        return isFolder;
    }

    public List<SymbolNode> getSubNodes() {
        return subNodes;
    }

    public ImageIcon getIcon() {
        if(imgIcon == null) {
            imgIcon = new ImageIcon(ResourceUtils.loadImage(file));
        }
        return imgIcon;
    }

    public BufferedImage getImage() {
        if(bi == null) {
            bi = new BufferedImage(getIcon().getIconWidth(), getIcon().getIconHeight(), BufferedImage.TYPE_4BYTE_ABGR);
            final Graphics2D g = bi.createGraphics();
            g.drawImage(getIcon().getImage(), null, null);
        }
        return bi;
    }

    private static class ImageFileFilter implements FileFilter {

        public boolean accept(File file) {
            if(file.isDirectory()) return true;
            final String n = file.getName().toLowerCase();
            return n.endsWith("png") || n.endsWith("jpg") || n.endsWith("gif") || n.endsWith("bmp");
        }
    }
}
