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

package org.esa.beam.collocation.visat;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.ui.SingleTargetProductDialog;
import org.esa.beam.framework.ui.AppContext;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Ralf Quast
 */
class CollocationDialog extends SingleTargetProductDialog {

    private CollocationFormModel formModel;
    private CollocationForm form;

    public CollocationDialog(AppContext appContext) {
        super(appContext, "Collocation", "collocation");
        formModel = new CollocationFormModel(getTargetProductSelector().getModel());
        form = new CollocationForm(formModel, getTargetProductSelector(), appContext);
    }

    @Override
    protected Product createTargetProduct() throws Exception {
        final Map<String, Product> productMap = new HashMap<String, Product>(5);
        productMap.put("master", formModel.getMasterProduct());
        productMap.put("slave", formModel.getSlaveProduct());

        final Map<String, Object> parameterMap = new HashMap<String, Object>(5);
        // collocation parameters
        parameterMap.put("targetProductName", formModel.getTargetProductName());
        parameterMap.put("renameMasterComponents", formModel.isRenameMasterComponentsSelected());
        parameterMap.put("renameSlaveComponents", formModel.isRenameSlaveComponentsSelected());
        parameterMap.put("masterComponentPattern", formModel.getMasterComponentPattern());
        parameterMap.put("slaveComponentPattern", formModel.getSlaveComponentPattern());
        parameterMap.put("resamplingType", formModel.getResamplingType());

        return GPF.createProduct("Collocate", parameterMap, productMap);
    }

    @Override
    public int show() {
        form.prepareShow();
        setContent(form);
        return super.show();
    }

    @Override
    public void hide() {
        form.prepareHide();
        super.hide();
    }
}
