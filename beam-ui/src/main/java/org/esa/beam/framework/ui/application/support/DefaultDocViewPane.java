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

package org.esa.beam.framework.ui.application.support;

import org.esa.beam.framework.ui.application.PageComponent;

import javax.swing.*;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import java.awt.*;
import java.beans.PropertyChangeEvent;

/**
 * Uses a {@link JInternalFrame} as control.
 */
public class DefaultDocViewPane extends AbstractPageComponentPane {
    private JInternalFrame internalFrame;

    public DefaultDocViewPane(PageComponent pageComponent) {
        super(pageComponent);
    }

    @Override
    protected JComponent createControl() {
        JComponent pageComponentControl = getPageComponent().getControl();
        if (pageComponentControl.getName() == null) {
            nameComponent(pageComponentControl, "Control");
        }
        internalFrame = new JInternalFrame();
        configureControl();
        internalFrame.getContentPane().add(pageComponentControl, BorderLayout.CENTER);
        internalFrame.addInternalFrameListener(new InternalFrameHandler());
        nameComponent(internalFrame, "Pane");
        return internalFrame;
    }

    @Override
    protected void pageComponentChanged(PropertyChangeEvent evt) {
        configureControl();
    }

    private void configureControl() {
        internalFrame.setTitle(getPageComponent().getTitle());
        internalFrame.setFrameIcon(getPageComponent().getIcon());
    }

    private static class InternalFrameHandler implements InternalFrameListener {
        public void internalFrameOpened(InternalFrameEvent e) {
            // todo
        }

        public void internalFrameClosing(InternalFrameEvent e) {
            // todo
        }

        public void internalFrameClosed(InternalFrameEvent e) {
            // todo
        }

        public void internalFrameIconified(InternalFrameEvent e) {
            // todo
        }

        public void internalFrameDeiconified(InternalFrameEvent e) {
            // todo
        }

        public void internalFrameActivated(InternalFrameEvent e) {
            // todo
        }

        public void internalFrameDeactivated(InternalFrameEvent e) {
            // todo
        }
    }
}
