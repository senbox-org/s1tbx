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
package org.esa.snap.core.util.io;

import org.esa.snap.core.dataio.ProductIOPlugIn;
import org.esa.snap.core.util.StringUtils;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * A <code>FileFilter</code> with file extensions support.
 *
 * @author Norman Fomferra
 */
public class SnapFileFilter extends FileFilter {

    private String formatName;
    private String[] extensions;
    private String description;

    public SnapFileFilter() {
    }

    public SnapFileFilter(String formatName, String extension, String description) {
        this(formatName, StringUtils.toStringArray(extension, ","), description);
    }

    public SnapFileFilter(String formatName, String[] extensions, String description) {
        setFormatName(formatName);
        setExtensions(extensions);
        setDescription(description);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SnapFileFilter that = (SnapFileFilter) o;

        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (!Arrays.equals(extensions, that.extensions)) return false;
        if (formatName != null ? !formatName.equals(that.formatName) : that.formatName != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = formatName != null ? formatName.hashCode() : 0;
        result = 31 * result + (extensions != null ? Arrays.hashCode(extensions) : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        return result;
    }

    public String getFormatName() {
        return formatName;
    }

    public void setFormatName(String formatName) {
        this.formatName = formatName;
    }

    /**
     * Returns whether or not this file filter has extensions.
     *
     * @return <code>true</code> if so
     */
    public boolean hasExtensions() {
        return extensions != null && extensions.length > 0;
    }

    /**
     * Returns the default extension. The default extension is the first entry in the array returned by the
     * <code>getExtensions</code> method.
     *
     * @return the default extension or <code>null</code> if no extensions have bees specified.
     * @see #getExtensions
     */
    public String getDefaultExtension() {
        return hasExtensions() ? getExtensions()[0] : null;
    }

    /**
     * Returns the accepted extensions of this filter. For example: <code>{".jpg", ".gif", ".png"}</code>.
     *
     * @return The array of extensions.
     * @see #setExtensions
     */
    public String[] getExtensions() {
        return extensions;
    }

    /**
     * Sets the accepted extensions of this filter. For example: <code>{".jpg", ".gif", ".png"}</code>.
     *
     * @param extensions The array of extensions.
     * @see #getExtensions
     */
    public void setExtensions(String[] extensions) {
        if (extensions != null) {
            ArrayList<String> extensionList = new ArrayList<>();
            for (final String extension : extensions) {
                if (extension.startsWith(".")) {
                    extensionList.add(extension);
                } else if (extension.trim().length() > 0) {
                    extensionList.add("." + extension);
                }
            }
            this.extensions = extensionList.toArray(new String[extensionList.size()]);
        } else {
            this.extensions = null;
        }
    }

    /**
     * Returns the description of this filter. For example: <code>"JPEG Images (*.jpg,*.jpeg)"</code>.
     *
     * @see javax.swing.filechooser.FileView#getTypeDescription(java.io.File)
     */
    @Override
    public String getDescription() {
        return description;
    }

    /**
     * Returns the description of this filter. For example: <code>"JPEG Images (*.jpg,*.jpeg)"</code>. If the extension
     * list is missing in the description text, it is automatically appended.
     *
     * @param description The description, must not be null.
     * @see #getDescription
     */
    public void setDescription(String description) {
        if (hasExtensions() && !description.endsWith(")")) {
            StringBuilder sb = new StringBuilder(description);
            sb.append(" (");
            for (int i = 0; i < extensions.length; i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append("*");
                if (extensions[i] != null) {
                    sb.append(extensions[i]);
                }
            }
            sb.append(")");
            this.description = sb.toString();
        } else {
            this.description = description;
        }
    }

    /**
     * Utility method which checks the extension the given file.
     *
     * @param file the file
     * @return <code>true</code> if the given file path ends with one of the registered extensions, <code>false</code>
     *         otherwise.
     */
    public boolean checkExtension(File file) {
        return file != null && checkExtension(file.getName());
    }

    /**
     * Utility method which checks the extension the given filename.
     *
     * @param filename the file name
     * @return <code>true</code> if the given file name ends with one of the registered extensions, <code>false</code>
     *         otherwise.
     */
    public boolean checkExtension(String filename) {
        return checkExtensions(filename, extensions);
    }

    /**
     * Utility method which checks the extension the given filename.
     *
     * @param filename   the file name
     * @param extensions the extension
     * @return <code>true</code> if the given file name ends with one of the registered extensions, <code>false</code>
     *         otherwise.
     */
    public static boolean checkExtensions(String filename, String[] extensions) {
        if (filename != null) {
            filename = filename.toLowerCase();
            for (String extension : extensions) {
                extension = extension.toLowerCase();
                if (filename.endsWith(extension)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Tests whether or not the given file is accepted by this filter. The default implementation returns
     * <code>true</code> if the given file is a directory or the path string ends with one of the registered extensions.
     * if no extension are defined, the method always returns <code>true</code>
     *
     * @param file the file to be or not be accepted.
     * @return <code>true</code> if given file is accepted by this filter
     */
    @Override
    public boolean accept(File file) {
        if (!hasExtensions()) {
            return true;
        }

        // directories are accepted right away
        if (file.isDirectory()) {
            return true;
        }

        // otherwise name must end with one of the extensions
        return checkExtension(file);
    }

    /**
     * Checks if the given directory represents a compound document.
     * If so, we don't want the user to descend into it when using the
     * {@link JFileChooser}.
     * The default implementation returns {@code false}.
     * Clients may override.
     *
     * @param dir The directory to check.
     * @return {@code true} If the given directory represents a compound document.
     * @since BEAM 4.6.1
     */
    public boolean isCompoundDocument(File dir) {
        return false;
    }

    /**
     * Gets the file selection mode for the {@link JFileChooser} if this filter is used.
     * The default implementation returns {@link FileSelectionMode#FILES_ONLY}.
     * Clients may override.
     *
     * @return {@code true} if the user can also select directories using this filter.
     * @since BEAM 4.6.1
     */
    public FileSelectionMode getFileSelectionMode() {
        return FileSelectionMode.FILES_ONLY;
    }

    /**
     * File selection modes.
     */
    public enum FileSelectionMode {
        /**
         * Instruction to display only files.
         */
        FILES_ONLY(JFileChooser.FILES_ONLY),

        /**
         * Instruction to display only directories.
         */
        DIRECTORIES_ONLY(JFileChooser.DIRECTORIES_ONLY),

        /**
         * Instruction to display both files and directories.
         */
        FILES_AND_DIRECTORIES(JFileChooser.FILES_AND_DIRECTORIES);

        private final int value;

        FileSelectionMode(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    /**
     * Return a alphabetically sorted list of file filters originating from a productIO plugin iterator.
     *
     * @param pluginIterator a productIO plugin iterator
     * @return a sorted list of file filters
     * @since BEAM 4.10
     */
    public static <T extends ProductIOPlugIn> List<SnapFileFilter> getSortedFileFilters(Iterator<T> pluginIterator) {
        HashSet<SnapFileFilter> fileFilterSet = new HashSet<>();
        while (pluginIterator.hasNext()) {
            final SnapFileFilter productFileFilter = pluginIterator.next().getProductFileFilter();
            if (productFileFilter != null) {
                fileFilterSet.add(productFileFilter);
            }
        }
        List<SnapFileFilter> fileFilterList = new ArrayList<>(fileFilterSet);
        Collections.sort(fileFilterList, (bff1, bff2) -> bff1.getDescription().compareTo(bff2.getDescription()));
        return fileFilterList;
    }
}
