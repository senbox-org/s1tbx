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
