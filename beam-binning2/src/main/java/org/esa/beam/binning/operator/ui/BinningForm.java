/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.binning.operator.ui;

import org.esa.beam.framework.gpf.ui.TargetProductSelector;
import org.esa.beam.framework.ui.AppContext;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

/**
 * The form for the {@link BinningDialog}.
 *
 * @author Olaf Danne
 * @author Thomas Storm
 */
class BinningForm extends JTabbedPane {

    BinningForm(AppContext appContext, BinningFormModel binningFormModel, TargetProductSelector targetProductSelector) {
        final JPanel ioPanel = new BinningIOPanel(appContext, binningFormModel, targetProductSelector);
        final JPanel regionPanel = new BinningFilterPanel(binningFormModel);
        final JPanel binningParametersPanel = new BinningVariablesPanel(appContext, binningFormModel);
        addTab("I/O Parameters", ioPanel);
        addTab("Filter", regionPanel);
        addTab("Configuration", binningParametersPanel);
    }
}
