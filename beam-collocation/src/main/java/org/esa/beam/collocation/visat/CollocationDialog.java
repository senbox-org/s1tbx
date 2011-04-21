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

import com.bc.ceres.binding.PropertyContainer;
import org.esa.beam.collocation.CollocateOp;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.ParameterDescriptorFactory;
import org.esa.beam.framework.gpf.ui.OperatorMenuSupport;
import org.esa.beam.framework.gpf.ui.SingleTargetProductDialog;
import org.esa.beam.framework.ui.AppContext;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Ralf Quast
 */
class CollocationDialog extends SingleTargetProductDialog {

    public static final String HELP_ID = "collocation";

    private Map<String, Object> parameterMap;
    private CollocationForm form;

    public CollocationDialog(AppContext appContext) {
        super(appContext, "Collocation", HELP_ID);

        parameterMap = new HashMap<String, Object>(17);
        final PropertyContainer propertyContainer = PropertyContainer.createMapBacked(parameterMap,
                                                                   CollocateOp.class,
                                                                   new ParameterDescriptorFactory());
        propertyContainer.setDefaultValues();

        OperatorMenuSupport menuSupport = new OperatorMenuSupport(this.getJDialog(),
                                                                  CollocateOp.class,
                                                                  propertyContainer,
                                                                  HELP_ID);
        getJDialog().setJMenuBar(menuSupport.createDefaultMenue());

        form = new CollocationForm(propertyContainer, getTargetProductSelector(), appContext);

    }

    @Override
    protected Product createTargetProduct() throws Exception {
        final Map<String, Product> productMap = new HashMap<String, Product>(5);
        productMap.put("master", form.getMasterProduct());
        productMap.put("slave", form.getSlaveProduct());

        return GPF.createProduct(OperatorSpi.getOperatorAlias(CollocateOp.class), parameterMap, productMap);
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
