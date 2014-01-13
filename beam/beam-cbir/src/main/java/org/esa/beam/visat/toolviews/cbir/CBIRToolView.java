/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.beam.visat.toolviews.cbir;


import org.esa.beam.framework.ui.application.support.AbstractToolView;

import javax.swing.*;


public class CBIRToolView extends AbstractToolView {

 /*   private static final ImageIcon updateIcon = UIUtils.loadImageIcon("icons/Update24.gif");
    private static final ImageIcon updateRolloverIcon = ToolButtonFactory.createRolloverIcon(updateIcon);
    private static final ImageIcon stopIcon = UIUtils.loadImageIcon("icons/Stop24.gif");
    private static final ImageIcon stopRolloverIcon = ToolButtonFactory.createRolloverIcon(stopIcon);

 */

    public CBIRToolView() {
    }

    public JComponent createControl() {

        return new TaskSequencePanel(this, new QueryPanel());

    }

}