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

package com.bc.swing.dock;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.Icon;
import javax.swing.JDialog;

/**
 * @author Norman Fomferra (norman.fomferra@brockmann-consult.de)
 * @version $Revision$ $Date$
 */
public class FloatingDialog extends JDialog implements FloatingComponent {

    private static final long serialVersionUID = 4032105256845729611L;

    private static final FloatingComponentFactory _factory = new Factory();

    private Component _content;
    private DockableComponent _originator;

    public FloatingDialog(Frame owner) {
        super(owner);
        installCloseHandler();
    }

    public FloatingDialog(Dialog owner) {
        super(owner);
        installCloseHandler();
    }

    public static FloatingComponentFactory getFactory() {
        return _factory;
    }

    /////////////////////////////////////////////////////////////////////////
    // FloatingComponent interface implementation

    public Icon getIcon() {
        return null;
    }

    public void setIcon(Icon icon) {
    }

    public DockableComponent getOriginator() {
        return _originator;
    }

    public void setOriginator(DockableComponent floatableComponent) {
        _originator = floatableComponent;
    }

    public Component getContent() {
        return _content;
    }

    public void setContent(Component content) {
        removeContent();
        _content = content;
        addContent();
    }

    public void close() {
        setVisible(false);
        removeContent();
        dispose();
    }

    /////////////////////////////////////////////////////////////////////////
    // private

    private void addContent() {
        if (_content != null) {
            getContentPane().add(_content, BorderLayout.CENTER);
        }
    }

    private void removeContent() {
        if (_content != null) {
            getContentPane().remove(_content);
        }
    }

    private void installCloseHandler() {
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowIconified(WindowEvent e) {
                getOriginator().setDocked(true);
            }

            @Override
            public void windowClosing(WindowEvent e) {
                getOriginator().setDocked(true);
            }
        });
    }

    private static class Factory implements FloatingComponentFactory {

        private Factory() {
        }

        public FloatingComponent createFloatingComponent(Window owner) {
            if (owner instanceof Frame) {
                return new FloatingDialog((Frame) owner);
            } else if (owner instanceof Dialog) {
                return new FloatingDialog((Dialog) owner);
            } else {
                return new FloatingDialog((Frame) null);
            }
        }
    }
}
