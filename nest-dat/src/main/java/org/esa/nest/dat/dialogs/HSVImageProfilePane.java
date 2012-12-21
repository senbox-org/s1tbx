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
package org.esa.nest.dat.dialogs;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.RGBImageProfilePane;
import org.esa.beam.util.PropertyMap;

/**

 */
public class HSVImageProfilePane extends RGBImageProfilePane {

    public final static String[] HSV_COMP_NAMES = new String[]{
            "Hue", /*I18N*/
            "Saturation", /*I18N*/
            "Value" /*I18N*/
    };

    public HSVImageProfilePane(final PropertyMap preferences, final Product product, final Product[] openedProducts) {
        super(preferences, product, openedProducts);

        _storeInProductCheck.setText("Store HSV channels as virtual bands in current product");
    }

    @Override
    protected String getComponentName(final int index) {
        return HSV_COMP_NAMES[index];
    }
}
