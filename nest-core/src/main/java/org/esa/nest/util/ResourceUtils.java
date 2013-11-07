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
package org.esa.nest.util;

import com.bc.ceres.core.runtime.internal.RuntimeActivator;
import org.esa.beam.framework.dataio.ProductCache;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.BasicApp;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.visat.VisatApp;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileSystemView;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Properties;

/**
 * Look up paths to resources
 */
public final class ResourceUtils {

    public static ImageIcon nestIcon = LoadIcon("org/esa/nest/icons/dat.png");
    public static ImageIcon rstbIcon = LoadIcon("array/rstb/icons/csa.png");
    public static ImageIcon arrayIcon = LoadIcon("array/rstb/icons/array_logo.png");
    public static ImageIcon esaIcon = LoadIcon("org/esa/nest/icons/esa.png");
    public static ImageIcon esaPlanetIcon = LoadIcon("org/esa/nest/icons/esa-planet.png");
    public static ImageIcon geoAusIcon = LoadIcon("org/esa/nest/icons/geo_aus.png");

    public static ImageIcon LoadIcon(final String path) {
        final java.net.URL imageURL = ResourceUtils.class.getClassLoader().getResource(path);
        if (imageURL == null) return null;
        return new ImageIcon(imageURL);
    }

    public static void deleteFile(final File file) {
        if (file.isDirectory()) {
            for (String aChild : file.list()) {
                deleteFile(new File(file, aChild));
            }
        }
        final Product prod = ProductCache.instance().getProduct(file);
        if(prod != null) {
            ProductCache.instance().removeProduct(file);
            prod.dispose();
        }
        if(!file.delete())
            System.out.println("Could not delete "+file.getName());
    }

    public static boolean validateFolder(final File file) {
        if(!file.exists()) {
            if(!file.mkdirs()) {
                if(VisatApp.getApp() != null)
                    VisatApp.getApp().showErrorDialog("Unable to create folder\n"+file.getAbsolutePath());
                else
                    System.out.println("Unable to create folder\n"+file.getAbsolutePath());
                return false;
            }
        }
        return true;
    }

    public static File GetFilePath(final String title, final String formatName, final String extension,
                                   final String fileName, final String description, final boolean isSave) {
        return GetFilePath(title, formatName, extension, fileName, description, isSave,
                           BasicApp.PROPERTY_KEY_APP_LAST_OPEN_DIR,
                           FileSystemView.getFileSystemView().getRoots()[0].getAbsolutePath());
    }

    public static File GetSaveFilePath(final String title, final String formatName, final String extension,
                                   final String fileName, final String description) {
        return GetFilePath(title, formatName, extension, fileName, description, true,
                           BasicApp.PROPERTY_KEY_APP_LAST_SAVE_DIR,
                           FileSystemView.getFileSystemView().getRoots()[0].getAbsolutePath());
    }

    public static File GetFilePath(final String title, final String formatName, final String extension,
                                   final String fileName, final String description, final boolean isSave,
                                   final String lastDirPropertyKey, final String defaultPath) {
        BeamFileFilter fileFilter = null;
        if(!extension.isEmpty()) {
            fileFilter = new BeamFileFilter(formatName, extension, description);
        }
        File file;
        if (isSave) {
            file = VisatApp.getApp().showFileSaveDialog(title, false, fileFilter, '.' + extension, fileName,
                                                        lastDirPropertyKey);
        } else {
            String lastDir = VisatApp.getApp().getPreferences().getPropertyString(lastDirPropertyKey, defaultPath);
            if(fileName == null)
                file = showFileOpenDialog(title, false, fileFilter, lastDir, lastDirPropertyKey);
            else
                file = showFileOpenDialog(title, false, fileFilter, fileName, lastDirPropertyKey);
        }
        
        return file == null ? null : FileUtils.ensureExtension(file, extension);
    }

    /**
     * allows the choice of picking directories only
     * @param title
     * @param dirsOnly
     * @param fileFilter
     * @param currentDir
     * @param lastDirPropertyKey
     * @return
     */
    private static File showFileOpenDialog(String title,
                                         boolean dirsOnly,
                                         FileFilter fileFilter,
                                         String currentDir,
                                         String lastDirPropertyKey) {
        final BeamFileChooser fileChooser = new BeamFileChooser();
        fileChooser.setCurrentDirectory(new File(currentDir));
        if (fileFilter != null) {
            fileChooser.setFileFilter(fileFilter);
        }
        fileChooser.setDialogTitle(VisatApp.getApp().getAppName() + " - " + title);
        fileChooser.setFileSelectionMode(dirsOnly ? JFileChooser.DIRECTORIES_ONLY : JFileChooser.FILES_ONLY);
        int result = fileChooser.showOpenDialog(VisatApp.getApp().getMainFrame());
        if (fileChooser.getCurrentDirectory() != null) {
            final String lastDirPath = fileChooser.getCurrentDirectory().getAbsolutePath();
            if (lastDirPath != null) {
                VisatApp.getApp().getPreferences().setPropertyString(lastDirPropertyKey, lastDirPath);
            }
        }
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (file == null || file.getName().isEmpty()) {
                return null;
            }
            return file.getAbsoluteFile();
        }
        return null;
    }

    public static Properties loadProperties(final String filename) throws IOException {
        final InputStream dbPropInputStream = getResourceAsStream(filename);
        final Properties dbProperties = new Properties();
        try {
            dbProperties.load(dbPropInputStream);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return dbProperties;
    }

    public static InputStream getResourceAsStream(final String filename) throws IOException {
        return getResourceAsStream(filename, ResourceUtils.class);
    }

    public static InputStream getResourceAsStream(final String filename, final Class theClass) throws IOException {
        // Try to load resource from jar
        final InputStream stream = ClassLoader.getSystemResourceAsStream(filename);
        if (stream != null) return stream;

        // If not found in jar, then load from disk
        final java.net.URL resURL = theClass.getClassLoader().getResource(filename);
        if (resURL != null) {
            try {
                return new FileInputStream(resURL.toURI().getPath());
            } catch(URISyntaxException e) {
                //
            }
        }

        return new FileInputStream(filename);
    }

    public static File getResourceAsFile(final String filename, final Class theClass) throws IOException {

        try {
            // load from disk
            final java.net.URL resURL = theClass.getClassLoader().getResource(filename);
            if (resURL != null) {
                return new File(resURL.getFile());
            }
        } catch(Exception e) {
            throw new IOException("Unable to open "+ filename);
        }
        throw new IOException("resURL==null Unable to open "+ filename);
    }

    /**
     * Optionally creates and returns the current user's application data directory.
     *
     * @param forceCreate if true, the directory will be created if it didn't exist before
     * @return the current user's application data directory
     */
    public static File getApplicationUserDir(boolean forceCreate) {
        final File dir = new File(SystemUtils.getUserHomeDir(), '.' + getContextID());
        if (forceCreate && !dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static String getContextID() {
        if (RuntimeActivator.getInstance() != null
                && RuntimeActivator.getInstance().getModuleContext() != null) {
            return RuntimeActivator.getInstance().getModuleContext().getRuntimeConfig().getContextId();
        }
        return System.getProperty("ceres.context", "nest");
    }

    public static String getHomeUrl() {
        return System.getProperty(getContextID()+".home", ".");
    }

    /**
     * get the temporary data folder in the user's application data directory
     * @return the temp folder
     */
    public static File getApplicationUserTempDataDir() {
        final File tmpDir = new File(ResourceUtils.getApplicationUserDir(true), "temp");
        if(!tmpDir.exists())
            tmpDir.mkdirs();
        return tmpDir;
    }

    public static File getGraphFolder(final String subFolder) {
        return new File(getHomeUrl(), File.separator + "graphs" + File.separator + subFolder);
    }

    public static File getResFolder() {
        return new File(ResourceUtils.getHomeUrl(), "res");
    }

    public static File findUserAppFile(String filename)
    {
        // check userhome/.nest first
        final File appHomePath = ResourceUtils.getApplicationUserDir(false);
        final String filePath = appHomePath.getAbsolutePath()
                + File.separator + filename;
        
        final File outFile = new File(filePath);
        if(outFile.exists())
            return outFile;

        // next check config folder
        return findConfigFile(filename);
    }

    public static File findConfigFile(String filename) {
        final String homeDir = System.getProperty(getContextID()+".home");
        if (homeDir != null && homeDir.length() > 0) {
            final File homeDirFile = new File(homeDir);

            final String homeDirStr = homeDirFile.getAbsolutePath();
            String settingsfilePath = homeDirStr + File.separator + "config" + File.separator + filename;

            final File outFile2 = new File(settingsfilePath);
            if(outFile2.exists())
                return outFile2;

            final int idx = homeDirStr.lastIndexOf(File.separator);
            settingsfilePath = homeDirStr.substring(0, idx) + File.separator + "config" + File.separator + filename;

            final File outFile3 = new File(settingsfilePath);
            if(outFile3.exists())
                return outFile3;
        }

        final File homeFolder = ResourceUtils.findHomeFolder();
        return new File(homeFolder, "config" + File.separator + filename);
    }

    public static File findHomeFolder()
    {
        final String nestHome = System.getProperty(getContextID()+".home");
        File homePath;
        if(nestHome == null)
            homePath = SystemUtils.getApplicationHomeDir();
        else
            homePath = new File(nestHome);
        String homePathStr = homePath.getAbsolutePath();
        if(homePathStr.endsWith(".") && homePathStr.length() > 1)
            homePathStr = homePathStr.substring(0, homePathStr.lastIndexOf(File.separator));
        return new File(homePathStr);
    }

    public static File findInHomeFolder(String filename)
    {
        final File homePath = findHomeFolder();
        String homePathStr = homePath.getAbsolutePath();
        filename = filename.replaceAll("/", File.separator);
        if(!File.separator.equals("\\")) {
            filename = filename.replaceAll("\\\\", File.separator);
        }
        if(homePathStr.endsWith(".") && homePathStr.length() > 1)
            homePathStr = homePathStr.substring(0, homePathStr.lastIndexOf(File.separator));
        String filePath = homePathStr + filename;

        final File outFile = new File(filePath);
        if(outFile.exists())
            return outFile;

        filePath = homePathStr.substring(0, homePathStr.lastIndexOf(File.separator)) + filename;

        final File outFile2 = new File(filePath);
        if(outFile2.exists())
            return outFile2;

        filePath = homePathStr.substring(0, homePathStr.lastIndexOf(File.separator)) + File.separator + "beam" + filename;

        final File outFile3 = new File(filePath);
        if(outFile3.exists())
            return outFile3;

        System.out.println("findInHomeFolder "+filename+ " not found in " + homePath);

        return null;
    }

    public static void sortFileList(final File[] filelist) {
        Comparator<File> byDirThenAlpha = new DirAlphaComparator();

        Arrays.sort(filelist, byDirThenAlpha);
    }

    private static class DirAlphaComparator implements Comparator<File> {

        // Comparator interface requires defining compare method.
        public int compare(File filea, File fileb) {
            //... Sort directories before files, otherwise alphabetical ignoring case.
            if (filea.isDirectory() && !fileb.isDirectory()) {
                return -1;
            } else if (!filea.isDirectory() && fileb.isDirectory()) {
                return 1;
            } else {
                return filea.getName().compareToIgnoreCase(fileb.getName());
            }
        }
    }

    /**
     * can be overriden to load the image to use
     * @param imgFile the file to load
     * @return BufferedImage
     */
    public static BufferedImage loadImage(final File imgFile) {
        if(imgFile != null && imgFile.exists()) {
            try {
                return ImageIO.read(imgFile);
            } catch(Exception e) {
                //
            }
        }
        return null;
    }
}
