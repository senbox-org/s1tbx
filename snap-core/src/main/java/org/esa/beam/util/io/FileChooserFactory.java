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

package org.esa.beam.util.io;

import javax.swing.*;
import java.io.File;
import java.lang.reflect.Constructor;

/**
 * A factory which is used to create instances of {@link javax.swing.JFileChooser}.
 */
public class FileChooserFactory {
    private Class<? extends JFileChooser> fileChooserClass;
    private Class<? extends JFileChooser> dirChooserClass;

    public Class<? extends JFileChooser> getFileChooserClass() {
        return fileChooserClass;
    }

    public void setFileChooserClass(Class<? extends JFileChooser> fileChooserClass) {
        this.fileChooserClass = fileChooserClass;
    }

    public Class<? extends JFileChooser> getDirChooserClass() {
        return dirChooserClass;
    }

    public void setDirChooserClass(Class<? extends JFileChooser> dirChooserClass) {
        this.dirChooserClass = dirChooserClass;
    }

    public static FileChooserFactory getInstance() {
        return Holder.instance;
    }

    public JFileChooser createFileChooser(File currentDirectory) {
        JFileChooser fileChooser = createChooser(fileChooserClass, currentDirectory);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        return fileChooser;
    }

    public JFileChooser createDirChooser(File currentDirectory) {
        JFileChooser dirChooser = createChooser(dirChooserClass, currentDirectory);
        dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        return dirChooser;
    }

    private JFileChooser createChooser(Class<?> chooserClass, File currentDirectory) {
        JFileChooser fileChooser;
        try {
            Constructor<?> constructor = chooserClass.getConstructor(File.class);
            fileChooser = (JFileChooser) constructor.newInstance(currentDirectory);
        } catch (Throwable e) {
            fileChooser = new JFileChooser(currentDirectory);
        }
        return fileChooser;
    }

    private FileChooserFactory() {
        fileChooserClass = BeamFileChooser.class;
        dirChooserClass = BeamFileChooser.class;
    }
    
    // Initialization on demand holder idiom
    private static class Holder {
        private static final FileChooserFactory instance = new FileChooserFactory();
    }
}
