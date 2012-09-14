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
