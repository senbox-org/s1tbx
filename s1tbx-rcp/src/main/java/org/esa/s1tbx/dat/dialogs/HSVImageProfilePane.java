/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.dat.dialogs;

import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.ui.RGBImageProfilePane;
import org.esa.snap.util.PropertyMap;

/**

 */
public class HSVImageProfilePane extends RGBImageProfilePane {

    public final static String[] HSV_COMP_NAMES = new String[]{
            "Hue", /*I18N*/
            "Saturation", /*I18N*/
            "Value" /*I18N*/
    };

    public HSVImageProfilePane(final PropertyMap preferences, final Product product,
                               final Product[] openedProducts, final int[] defaultBandIndices) {
        super(preferences, product, openedProducts, defaultBandIndices);

        storeInProductCheck.setText("Store HSV channels as virtual bands in current product");
    }

    @Override
    protected String getComponentName(final int index) {
        return HSV_COMP_NAMES[index];
    }
}
