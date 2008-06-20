/*
 * $Id: ContrastStretchToolView.java,v 1.2 2007/04/20 08:49:14 marcop Exp $
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
package org.esa.beam.visat.toolviews.imageinfo;

import org.esa.beam.framework.ui.application.support.AbstractToolView;

import javax.swing.JComponent;


/**
 * The contrast stretch window.
 */
public class ColorManipulationToolView extends AbstractToolView {

    public static final String ID = ColorManipulationToolView.class.getName();

    public ColorManipulationToolView() {
    }

    @Override
    protected JComponent createControl() {
        ColorManipulationForm colorManipulationForm = new ColorManipulationForm(this);
        return colorManipulationForm.getContentPanel();
    }
}