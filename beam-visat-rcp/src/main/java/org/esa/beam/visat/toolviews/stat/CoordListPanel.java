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

package org.esa.beam.visat.toolviews.stat;

import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.application.ToolView;

import javax.swing.JOptionPane;
import java.awt.Cursor;
import java.io.IOException;

/**
 * A pane within the statistics window which displays a co-oordinate list as a text table.
 *
 */
class CoordListPanel extends TextPagePanel {

    private static final String TITLE_PREFIX = "Co-ordinate List";     /*I18N*/
    private static final String DEFAULT_COORDLIST_TEXT = "Co-ordinate list not available.\n" +
            "No geometry (line, polyline or polygon) selected in the image view.";  /*I18N*/

    CoordListPanel(final ToolView parentDialog, String helpID) {
        super(parentDialog, DEFAULT_COORDLIST_TEXT, helpID, TITLE_PREFIX);
    }

    @Override
    protected void ensureValidData() {
        final Cursor oldCursor = UIUtils.setRootFrameWaitCursor(getParentDialogContentPane());
        try {
            StatisticsUtils.TransectProfile.getTransectProfileData(getRaster());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(getParent(),
                                          "Failed to compute profile plot.\n" +
                                          "An I/O error occurred:" + e.getMessage(),
                                          "I/O error",
                                          JOptionPane.ERROR_MESSAGE);       /*I18N*/
        } finally {
            UIUtils.setRootFrameCursor(getParentDialogContentPane(), oldCursor);
        }
    }

    @Override
    protected String createText() {
        final Cursor oldCursor = UIUtils.setRootFrameWaitCursor(getParentDialog().getControl());
        try {
            if (getRaster() == null) {
                return DEFAULT_COORDLIST_TEXT;
            }
            final String transectProfileText = StatisticsUtils.TransectProfile.createTransectProfileText(getRaster());
            if (transectProfileText != null) {
                return transectProfileText;
            } else {
                return DEFAULT_COORDLIST_TEXT;
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(getParent(),
                                          "Failed to compute profile plot.\n" +
                                                  "An I/O error occurred:" + e.getMessage(),
                                          "I/O error",
                                          JOptionPane.ERROR_MESSAGE);   /*I18N*/
            return DEFAULT_COORDLIST_TEXT;
        } finally {
            UIUtils.setRootFrameCursor(getParentDialogContentPane(), oldCursor);
        }
    }

    @Override
    public void handleLayerContentChanged() {
        updateContent();
    }

    @Override
    protected boolean mustUpdateContent() {
        return super.mustUpdateContent() || isVectorDataNodeChanged();
    }

    @Override
    public void setVisible(boolean aFlag) {
        super.setVisible(aFlag);
        updateContent();
    }

}
