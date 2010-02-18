/*
 * $Id: BitmaskOverlayToolView.java,v 1.1 2007/04/19 10:41:38 norman Exp $
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