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
package org.esa.beam.framework.ui;

import java.awt.BorderLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * A utility class providing helper methods for <code>JPanel</code>s with a <code>BorderLayout</code> layout manager.
 *
 * @author Sabine Embacher
 * @version $Revision$  $Date$
 */
public class BorderLayoutUtils {

    public static BorderLayout createDefaultBorderLayout() {
        return new BorderLayout(7, 7);
    }

    public static JPanel createPanel() {
        return new JPanel(new BorderLayout());
    }

    public static JPanel createPanel(JComponent centerComponent, JComponent placedComponent,
                                     String borderLayoutConstraint) {
        JPanel panel = createPanel();
        return addToPanel(panel, centerComponent, placedComponent, borderLayoutConstraint);
    }

    public static JPanel createDefaultEmptyBorderPanel() {
        JPanel centerPanel = new JPanel(createDefaultBorderLayout());
        centerPanel.setBorder(UIDefaults.getDialogBorder());
        return centerPanel;
    }

    public static JPanel addToPanel(JPanel panel,
                                    JComponent centerComponent,
                                    JComponent arrangedComponent,
                                    String borderLayoutConstraint) {
        panel.add(centerComponent, BorderLayout.CENTER);
        panel.add(arrangedComponent, borderLayoutConstraint);
        return panel;
    }


}
