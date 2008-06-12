/*
 * $Id: BeamFileFilter.java,v 1.1 2006/10/11 10:36:41 marcop Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.util.io;

import org.esa.beam.util.StringUtils;

import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.util.ArrayList;

/**
 * A <code>FileFilter</code> with file extensions support.
 *
 * @author Norman Fomferra
 * @version $Revision$  $Date$
 */
public class BeamFileFilter extends FileFilter {

    private String _formatName;
    private String[] _extensions;
    private String _description;

    public BeamFileFilter() {
    }

    public BeamFileFilter(String formatName, String extension, String description) {
        this(formatName, StringUtils.toStringArray(extension, ","), description);
    }

    public BeamFileFilter(String formatName, String[] extensions, String description) {
        setFormatName(formatName);
        setExtensions(extensions);
        setDescription(description);
    }

    public String getFormatName() {
        return _formatName;
    }

    public void setFormatName(String formatName) {
        _formatName = formatName;
    }

    /**
     * Returns whether or not this file filter has extensions.
     *
     * @return <code>true</code> if so
     */
    public boolean hasExtensions() {
        return _extensions != null && _extensions.length > 0;
    }

    /**
     * Returns the default extension. The default extension is the first entry in the array returned by the
     * <code>getExtensions</code> method.
     *
     * @return the default extension or <code>null</code> if no extensions have bees specified.
     *
     * @see #getExtensions
     */
    public String getDefaultExtension() {
        return hasExtensions() ? getExtensions()[0] : null;
    }

    /**
     * Returns the accepted extensions of this filter. For example: <code>{".jpg", ".gif", ".png"}</code>.
     *
     * @see #setExtensions
     */
    public String[] getExtensions() {
        return _extensions;
    }

    /**
     * Sets the accepted extensions of this filter. For example: <code>{".jpg", ".gif", ".png"}</code>.
     *
     * @see #getExtensions
     */
    public void setExtensions(String[] extensions) {
        if (extensions != null) {
            ArrayList<String> extensionList = new ArrayList<String>();
            for (int i = 0; i < extensions.length; i++) {
                final String extension = extensions[i];
                if (extension.startsWith(".")) {
                    extensionList.add(extension);
                } else if (extension.trim().length() > 0) {
                    extensionList.add("." + extension);
                }
            }
            _extensions = extensionList.toArray(new String[0]);
        } else {
            _extensions = null;
        }
    }

    /**
     * Returns the description of this filter. For example: <code>"JPEG Images (*.jpg,*.jpeg)"</code>.
     *
     * @see javax.swing.filechooser.FileView#getTypeDescription(java.io.File)
     */
    public String getDescription() {
        return _description;
    }

    /**
     * Returns the description of this filter. For example: <code>"JPEG Images (*.jpg,*.jpeg)"</code>. If the extension
     * list is missing in the description text, it is automatically appended.
     *
     * @see #getDescription
     */
    public void setDescription(String description) {
        if (hasExtensions() && !description.endsWith(")")) {
            StringBuffer sb = new StringBuffer(description);
            sb.append(" (");
            for (int i = 0; i < _extensions.length; i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append("*");
                if (_extensions[i] != null) {
                    sb.append(_extensions[i]);
                }
            }
            sb.append(")");
            _description = sb.toString();
        } else {
            _description = description;
        }
    }

    /**
     * Utility method which checks the extension the given file.
     *
     * @param file the file
     *
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
     *
     * @return <code>true</code> if the given file name ends with one of the registered extensions, <code>false</code>
     *         otherwise.
     */
    public boolean checkExtension(String filename) {
        if (filename != null) {
            filename = filename.toLowerCase();
            for (int i = 0; i < _extensions.length; i++) {
                final String extension = _extensions[i].toLowerCase();
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
     *
     * @return <code>true</code> if given file is accepted by this filter
     */
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

}
