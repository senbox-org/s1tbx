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

import javax.swing.Icon;

/**
 * The tool view containing a profile plot
 *
 * @author Marco Zuehlke
 */
public class ProfilePlotToolView extends AbstractStatisticsToolView {

    public static final String ID = ProfilePlotToolView.class.getName();

    @Override
    protected PagePanel createPagePanel() {
        final String helpId = getDescriptor().getHelpId();
        final String chartTitle = ProfilePlotPanel.CHART_TITLE;
        final Icon largeIcon = getDescriptor().getLargeIcon();
        final ProfilePlotPanel profilePlotPanel = new ProfilePlotPanel(this, helpId);
        final TableViewPagePanel tableViewPagePanel = new TableViewPagePanel(this, helpId, chartTitle, largeIcon);
        profilePlotPanel.setAlternativeView(tableViewPagePanel);
        tableViewPagePanel.setAlternativeView(profilePlotPanel);
        return profilePlotPanel;
    }
}
