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
package org.esa.snap.core.param;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

/**
 * A <code>AbstractParamXEditor</code> is base class for editors which use special purpose editors for editing a
 * parameter value. For example, the special purpose editor could be a color chooser or file chooser dialog. The special
 * purpose editor is invoked by pressing the "..." button usually located to the right of the value display.
 *
 * @author Norman Fomferra
 * @version $Revision$  $Date$
 */
public abstract class AbstractParamXEditor extends AbstractParamEditor {

    private XEditorPane _xEditorPane;
    private static Icon _xEditorIcon;
    private AbstractButton _xEditorButton;

    /**
     * Creates the object with a given parameter.
     *
     * @param parameter          the <code>Parameter</code> to be edited.
     * @param useDefaultVerifier <code>true</code> if a default verifier should be used
     */
    protected AbstractParamXEditor(Parameter parameter, boolean useDefaultVerifier) {
        super(parameter, useDefaultVerifier);
    }

    /**
     * Gets the UI component used to edit the parameter's value.
     */
    public JComponent getEditorComponent() {
        return _xEditorPane;
    }

    @Override
    protected void initUI() {
        super.initUI(); // creates the default label components for us
        initUIChild();
        _xEditorPane = new XEditorPane();
        nameComponent(_xEditorPane, "XEditor");
        final JComponent editorComponentChild = getEditorComponentChild();
        _xEditorPane.add(BorderLayout.CENTER, editorComponentChild);
        final JPanel buttonPanel = new JPanel(new BorderLayout());
        nameComponent(buttonPanel, "ButtonPanel");

        final AbstractButton xEditorButton = getXEditorButton();
        if (editorComponentChild instanceof JTextField) {
            buttonPanel.add(BorderLayout.CENTER, xEditorButton);
        } else {
            final Dimension size = xEditorButton.getPreferredSize();
            xEditorButton.setPreferredSize(new Dimension(size.width, size.width));
            buttonPanel.add(BorderLayout.NORTH, xEditorButton);
        }
        _xEditorPane.add(BorderLayout.EAST, buttonPanel);
    }

    /**
     * Tells the UI to update it's state.
     * <p>Note: If you override this method, please call the super class version first.
     */
    @Override
    public void updateUI() {
        super.updateUI();
        if (_xEditorPane.isEnabled() != isEnabled()) {
            _xEditorPane.setEnabled(isEnabled());
        }
    }

    protected abstract void initUIChild();

    public abstract JComponent getEditorComponentChild();

    protected abstract void invokeXEditor();

    protected AbstractButton getXEditorButton() {
        if (_xEditorButton == null) {
            _xEditorButton = createXEditorButton();
        }
        return _xEditorButton;
    }

    protected AbstractButton createXEditorButton() {
//        Icon icon = getXEditorIcon();
//        AbstractButton button = ToolButtonFactory.createButton(icon, false);
        final AbstractButton button = new JButton("...");
        nameComponent(button, "Button");
        final Dimension size = new Dimension(26, 16);
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                invokeXEditor();
            }
        });
        return button;
    }

    /**
     * Gets the "standard" edit icon used for parameter editors.
     */
    protected Icon getXEditorIcon() {
        if (_xEditorIcon == null) {
            URL url = AbstractParamEditor.class.getResource("/org/esa/snap/resources/images/icons/Edit16.gif");
            if (url != null) {
                _xEditorIcon = new ImageIcon(url);
            }
        }
        return _xEditorIcon;
    }

    protected String getXEditorTitle() {
        String title = getParameter().getProperties().getLabel();
        if (title == null) {
            title = getParameter().getName();
        }
        return title;
    }

    class XEditorPane extends JPanel {

        public XEditorPane() {
            super(new BorderLayout(3, 3));
        }

        /**
         * Overrides the JPanel setEnabled method to dispatch call to all components attached
         *
         * @param enabled boolean whether the componets are enabled or not
         */
        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            getEditorComponentChild().setEnabled(enabled);
            getXEditorButton().setEnabled(enabled);
        }
    }
}
