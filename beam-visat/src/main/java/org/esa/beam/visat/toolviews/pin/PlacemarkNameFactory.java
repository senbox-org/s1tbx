package org.esa.beam.visat.toolviews.pin;

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.datamodel.Pin;
import org.esa.beam.framework.datamodel.PlacemarkDescriptor;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNodeGroup;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision:$ $Date:$
 */
public class PlacemarkNameFactory {

    private PlacemarkNameFactory() {
    }

    public static String[] createUniqueNameAndLabel(PlacemarkDescriptor placemarkDescriptor, Product product) {
        ProductNodeGroup<Pin> placemarkGroup = placemarkDescriptor.getPlacemarkGroup(product);
        int pinNumber = placemarkGroup.getNodeCount() + 1;
        String name = createName(placemarkDescriptor, pinNumber);
        while (placemarkGroup.get(name) != null) {
            name = createName(placemarkDescriptor, ++pinNumber);
        }
        final String label = createLabel(placemarkDescriptor, pinNumber, true);
        return new String[]{name, label};
    }

    public static String createLabel(PlacemarkDescriptor placemarkDescriptor, int pinNumber, boolean firstLetterIsUpperCase) {
        Assert.argument(placemarkDescriptor.getRoleLabel().length() > 0, "placemarkDescriptor.getRoleLabel()");
        String name = placemarkDescriptor.getRoleLabel();
        if (firstLetterIsUpperCase) {
            name = name.substring(0, 1).toUpperCase() + name.substring(1);
        }
        return name + " " + pinNumber;
    }

    public static String createName(PlacemarkDescriptor placemarkDescriptor, int pinNumber) {
        return placemarkDescriptor.getRoleName() + "_" + pinNumber;
    }

}
