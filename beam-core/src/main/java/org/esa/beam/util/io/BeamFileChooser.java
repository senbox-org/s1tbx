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

import org.esa.beam.util.Debug;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.HeadlessException;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

/**
 * The general BEAM file chooser.
 *
 * @author Norman Fomferra
 * @version $Revision$  $Date$
 */
public class BeamFileChooser extends JFileChooser {

    private String lastFilename;
    private Rectangle dialogBounds;
    private ResizeHandler resizeHandler;

    public BeamFileChooser() {
        super();
        init();
    }

    public BeamFileChooser(FileSystemView fsv) {
        super(fsv);
        init();
    }

    public BeamFileChooser(File currentDirectory) {
        super(currentDirectory);
        init();
    }

    public BeamFileChooser(File currentDirectory, FileSystemView fsv) {
        super(currentDirectory, fsv);
        init();
    }

    @Override
    public Icon getIcon(File f) {
        final Icon icon = super.getIcon(f);
        if (f.isDirectory() && isCompoundDocument(f)) {
            return new CompoundDocumentIcon(icon);
        }
        return icon;
    }

    @Override
    public boolean isTraversable(File f) {
        return f.isDirectory() && !isCompoundDocument(f);
    }

    /**
     * Overridden in order to recognize dialog size changes.
     *
     * @param parent the parent
     * @return the dialog
     * @throws HeadlessException
     */
    @Override
    protected JDialog createDialog(Component parent) throws HeadlessException {
        final JDialog dialog = super.createDialog(parent);
        dialog.addComponentListener(resizeHandler);
        if (dialogBounds != null) {
            dialog.setBounds(dialogBounds);
        }
        return dialog;
    }

    /**
     * Called by the UI when the user hits the Approve button (labeled "Open" or "Save", by default). This can also be
     * called by the programmer.
     */
    @Override
    public void approveSelection() {
        Debug.trace("BeamFileChooser: approveSelection(): selectedFile = " + getSelectedFile());
        Debug.trace("BeamFileChooser: approveSelection(): currentFilename = " + getCurrentFilename());
        Debug.trace("BeamFileChooser: approveSelection(): currentDirectory = " + getCurrentDirectory());
        if (getDialogType() != JFileChooser.OPEN_DIALOG) {
            ensureSelectedFileHasValidExtension();
        }
        super.approveSelection();
    }

    /**
     * Gets the dialog bounds to be used for the next {@link #showDialog(java.awt.Component, String)} call.
     *
     * @return the dialog bounds
     */
    public Rectangle getDialogBounds() {
        return dialogBounds;
    }

    /**
     * Sets the dialog bounds to be used for the next {@link #showDialog(java.awt.Component, String)} call.
     *
     * @param dialogBounds the dialog bounds
     */
    public void setDialogBounds(Rectangle dialogBounds) {
        this.dialogBounds = dialogBounds;
    }

    /**
     * @return The current filename, or <code>null</code>.
     */
    public String getCurrentFilename() {
        File selectedFile = getSelectedFile();
        if (selectedFile != null) {
            return selectedFile.getName();
        }
        return null;
    }

    /**
     * Sets the current filename.
     *
     * @param currentFilename The current filename, or <code>null</code>.
     */
    public void setCurrentFilename(String currentFilename) {
        Debug.trace("BeamFileChooser: setCurrentFilename(\"" + currentFilename + "\")");
        String defaultExtension = getDefaultExtension();

        if (getDialogType() != JFileChooser.OPEN_DIALOG) {
            if (currentFilename != null && defaultExtension != null) {
                FileFilter fileFilter = getFileFilter();
                if (fileFilter instanceof BeamFileFilter) {
                    BeamFileFilter filter = (BeamFileFilter) fileFilter;
                    if (!filter.checkExtension(currentFilename)) {
                        currentFilename = FileUtils.exchangeExtension(currentFilename, defaultExtension);
                    }
                } else if (fileFilter instanceof FileNameExtensionFilter) {
                    FileNameExtensionFilter filter = (FileNameExtensionFilter) fileFilter;
                    if (!BeamFileFilter.checkExtensions(currentFilename, filter.getExtensions())) {
                        currentFilename = FileUtils.exchangeExtension(currentFilename, defaultExtension);
                    }
                }
            }
        }

        if (currentFilename != null && currentFilename.length() > 0) {
            setSelectedFile(new File(currentFilename));
        }
    }

    /**
     * Returns the currently selected BEAM file filter.
     *
     * @return the current BEAM file filter, or <code>null</code>
     */
    public BeamFileFilter getBeamFileFilter() {
        FileFilter ff = getFileFilter();
        if (ff instanceof BeamFileFilter) {
            return (BeamFileFilter) ff;
        }
        return null;
    }

    /**
     * @return The current extension or <code>null</code> if it is unknown.
     */
    public String getDefaultExtension() {
        if (getBeamFileFilter() != null) {
            return getBeamFileFilter().getDefaultExtension();
        }
        return null;
    }


    /**
     * Checks whether or not the given filename with one of the known file extensions. The known file extension of this
     * file chooser are those, which are registered using a {@link <code>BeamFileFilter</code>}.
     *
     * @param filename the filename to be checked
     * @return <code>true</code>, if the given file has a "known" extension
     * @see BeamFileFilter
     */
    public boolean checkExtension(String filename) {
        if (filename != null) {
            FileFilter[] fileFilters = getChoosableFileFilters();
            if (fileFilters != null) {
                for (FileFilter filter : fileFilters) {
                    if (filter instanceof BeamFileFilter) {
                        final BeamFileFilter beamFileFilter = (BeamFileFilter) filter;
                        if (beamFileFilter.checkExtension(filename)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Utility method which returns this file chooser's parent window.
     *
     * @return the parent window or <code>null</code>
     */
    protected Window getWindow() {
        Container w = this;
        while (!(w instanceof Window)) {
            w = w.getParent();
        }
        return (Window) w;
    }

    ///////////////////////////////////////////////////////////////////////////
    // private stuff
    ///////////////////////////////////////////////////////////////////////////

    private void init() {

        resizeHandler = new ResizeHandler();
        setAcceptAllFileFilterUsed(false);

        addPropertyChangeListener(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                Object newValue = evt.getNewValue();
                if (newValue instanceof File) {
                    lastFilename = ((File) newValue).getName();
                }
            }
        });
        addPropertyChangeListener(JFileChooser.FILE_FILTER_CHANGED_PROPERTY, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                final BeamFileFilter beamFileFilter = getBeamFileFilter();
                if (beamFileFilter != null) {
                    setFileSelectionMode(beamFileFilter.getFileSelectionMode().getValue());
                } else {
                    setFileSelectionMode(FILES_ONLY);
                }

                if (getSelectedFile() != null) {
                    return;
                }
                if (lastFilename == null || lastFilename.length() == 0) {
                    return;
                }
                setCurrentFilename(lastFilename);
            }
        });
    }

    private boolean isCompoundDocument(File file) {
        final FileFilter[] filters = getChoosableFileFilters();
        for (FileFilter fileFilter : filters) {
            if (fileFilter instanceof BeamFileFilter) {
                BeamFileFilter beamFileFilter = (BeamFileFilter) fileFilter;
                if (beamFileFilter.isCompoundDocument(file)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void ensureSelectedFileHasValidExtension() {
        File selectedFile = getSelectedFile();
        if (selectedFile != null) {
            BeamFileFilter mff = getBeamFileFilter();
            if (mff != null
                    && mff.getDefaultExtension() != null
                    && !mff.checkExtension(selectedFile)) {
                selectedFile = FileUtils.exchangeExtension(selectedFile, mff.getDefaultExtension());
                Debug.trace("mod. selected file: " + selectedFile.getPath());
                setSelectedFile(selectedFile);
            }
        }
    }

    private class ResizeHandler extends ComponentAdapter {

        @Override
        public void componentMoved(ComponentEvent e) {
            setDialogBounds(e.getComponent().getBounds());
        }

        @Override
        public void componentResized(ComponentEvent e) {
            setDialogBounds(e.getComponent().getBounds());
        }
    }

    private static class CompoundDocumentIcon implements Icon {

        private final Icon baseIcon;
        private static final Icon compoundDocumentIcon = new ImageIcon(
                CompoundDocumentIcon.class.getResource("CompoundDocument12.png"));

        public CompoundDocumentIcon(Icon baseIcon) {
            this.baseIcon = baseIcon;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            baseIcon.paintIcon(c, g, x, y);
            compoundDocumentIcon.paintIcon(c, g,
                                           x + baseIcon.getIconWidth() - compoundDocumentIcon.getIconWidth(),
                                           y + baseIcon.getIconHeight() - compoundDocumentIcon.getIconHeight());
        }

        @Override
        public int getIconWidth() {
            return baseIcon.getIconWidth();
        }

        @Override
        public int getIconHeight() {
            return baseIcon.getIconHeight();
        }
    }
}