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

import java.awt.GridBagConstraints;
import java.awt.Window;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.esa.beam.framework.dataop.maptransf.MapTransformUI;
import org.esa.beam.util.Guardian;

public class ProjectionParamsDialog extends ModalDialog {

    private final MapTransformUI _transformUI;

    public ProjectionParamsDialog(Window parent, MapTransformUI transformUI) {
        super(parent, "Projection Parameters", ID_OK_CANCEL | ID_RESET /*| ID_HELP*/, "mapProjection"); /* I18N */
        Guardian.assertNotNull("transformUI", transformUI);
        _transformUI = transformUI;
        createUI();
    }

    public MapTransformUI getTransformUI() {
        return _transformUI;
    }

    @Override
    protected boolean verifyUserInput() {
        return _transformUI.verifyUserInput();
    }

    @Override
    protected void onReset() {
        _transformUI.resetToDefaults();
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Creates the user interface from the UI component passed in tby the transform
     */
    private void createUI() {
        JPanel dialogPane = GridBagUtils.createPanel();
        dialogPane.setBorder(new EmptyBorder(2, 2, 2, 2));
        final GridBagConstraints gbc = GridBagUtils.createDefaultConstraints();
        GridBagUtils.addToPanel(dialogPane,
                                getTransformUI().getUIComponent(),
                                gbc, "gridwidth=1,insets.top=0");
        setContent(dialogPane);
    }
}
