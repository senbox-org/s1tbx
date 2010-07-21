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

package org.esa.beam.gpf.operators.mosaic;

import org.esa.beam.framework.gpf.ui.TargetProductSelector;
import org.esa.beam.framework.ui.AppContext;

import javax.swing.JTabbedPane;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
public class MosaicForm extends JTabbedPane {

    private final AppContext appContext;
    private MosaicFormModel mosaicModel;
    private MosaicIOPanel ioPanel;
    private MosaicMapProjectionPanel mapProjectionPanel;
    private MosaicExpressionsPanel expressionsPanel;

    public MosaicForm(TargetProductSelector targetProductSelector, AppContext appContext) {
        this.appContext = appContext;
        mosaicModel = new MosaicFormModel();
        createUI(targetProductSelector);
    }

    private void createUI(TargetProductSelector selector) {
        ioPanel = new MosaicIOPanel(appContext, mosaicModel, selector);
        mapProjectionPanel = new MosaicMapProjectionPanel(appContext, mosaicModel);
        expressionsPanel = new MosaicExpressionsPanel(appContext, mosaicModel);

        addTab("I/O Parameters", ioPanel); /*I18N*/
        addTab("Map Projection Definition", mapProjectionPanel); /*I18N*/
        addTab("Variables & Conditions", expressionsPanel);  /*I18N*/
    }


    MosaicFormModel getFormModel() {
        return mosaicModel;
    }

    void prepareShow() {
        ioPanel.prepareShow();
        mapProjectionPanel.prepareShow();
    }

    void prepareHide() {
        mapProjectionPanel.prepareHide();
        ioPanel.prepareHide();
    }

}
