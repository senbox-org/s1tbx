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
