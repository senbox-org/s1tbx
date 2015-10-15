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
package org.esa.snap.core.param.editors;

import org.esa.snap.core.param.AbstractParamXEditor;
import org.esa.snap.core.param.Parameter;

import javax.swing.BorderFactory;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;

/**
 * An editor which uses a coloured {@link javax.swing.JComponent} if, when clicked, opens a {@link javax.swing.JColorChooser}.
 */
public class ColorEditor extends AbstractParamXEditor {

    private ColorDisplay _colorDisplay;

    public ColorEditor(Parameter parameter) {
        super(parameter, false);
    }

    public ColorDisplay getColorDisplay() {
        return _colorDisplay;
    }

    @Override
    public JComponent getEditorComponentChild() {
        return getColorDisplay();
    }

    @Override
    protected void initUIChild() {
        _colorDisplay = new ColorDisplay();
        nameComponent(_colorDisplay, "ColorDisplay");
    }

    @Override
    public void updateUI() {
        super.updateUI();
        if (getColorDisplay().isEnabled() != isEnabled()) {
            getColorDisplay().setEnabled(isEnabled());
        }
        getColorDisplay().repaint();
    }

    @Override
    protected void invokeXEditor() {
        Color color = JColorChooser.showDialog(getEditorComponent(),
                                               "Select Colour", /*I18N*/
                                               getParameterColorValue());
        if (color != null) {
            setParameterColorValue(color);
        }
    }

    private Color getParameterColorValue() {
        Color color = (Color) getParameter().getValue();
        return color == null ? Color.black : color;
    }

    private void setParameterColorValue(Color color) {
        getParameter().setValue(color, null);
    }

    class ColorDisplay extends JComponent {

        public ColorDisplay() {
            setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(0, 0, 1, 1),
                                                         BorderFactory.createLineBorder(Color.darkGray)));
        }

        @Override
        public Dimension getMinimumSize() {
            return getXEditorButton().getMinimumSize();
        }

        @Override
        public Dimension getPreferredSize() {
            Runtime.getRuntime().maxMemory();
            Dimension size1 = getXEditorButton().getPreferredSize();
            Dimension size2 = new JTextField(1).getPreferredSize();
            int height = Math.max(size1.height, size2.height);
            return new Dimension(3 * height, height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Rectangle b = getBounds();
            Insets i = getInsets();
            g.setColor(getParameterColorValue());
            g.fillRect(b.x + i.left,
                       b.y + i.top,
                       b.width - (i.left + i.right),
                       b.height - (i.top + i.bottom));
            super.paintComponent(g);
        }
    }
}
