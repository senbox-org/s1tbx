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
package org.esa.beam.visat.toolviews.mask;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;

class MaskViewerForm extends MaskForm {

    public MaskViewerForm(ListSelectionListener selectionListener) {
        super(false, selectionListener);
    }

    @Override
    public JPanel createContentPanel() {
        JPanel tablePanel = new JPanel(new BorderLayout(4, 4));
        tablePanel.add(new JScrollPane(getMaskTable()), BorderLayout.CENTER);
        tablePanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        return tablePanel;
    }
}